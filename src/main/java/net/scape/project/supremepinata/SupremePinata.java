package net.scape.project.supremepinata;

import dev.faststats.Metrics;
import dev.faststats.bukkit.BukkitContext;
import net.scape.project.supremepinata.api.SupremePinataProvider;
import net.scape.project.supremepinata.command.MoneyPoolCommand;
import net.scape.project.supremepinata.command.PinataCommand;
import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.config.PinataRegistry;
import net.scape.project.supremepinata.integration.IntegrationManager;
import net.scape.project.supremepinata.integration.VoteListenerHook;
import net.scape.project.supremepinata.listener.MenuListener;
import net.scape.project.supremepinata.listener.PinataProtectionListener;
import net.scape.project.supremepinata.location.LocationService;
import net.scape.project.supremepinata.pinata.PinataManager;
import net.scape.project.supremepinata.reward.RewardService;
import net.scape.project.supremepinata.statistics.DataFile;
import net.scape.project.supremepinata.statistics.JdbcStatisticsStorage;
import net.scape.project.supremepinata.statistics.StatisticsStorage;
import net.scape.project.supremepinata.statistics.StatisticsService;
import net.scape.project.supremepinata.trigger.MoneyPoolService;
import net.scape.project.supremepinata.trigger.VotePartyService;
import net.scape.project.supremepinata.utility.menu.MenuUtil;
import net.scape.project.supremepinata.utility.SchedulerService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SupremePinata extends JavaPlugin {

    private static SupremePinata supremePinata;

    private MessageService messages;
    private DataFile dataFile;
    private PinataRegistry pinataRegistry;
    private LocationService locationService;
    private RewardService rewardService;
    private StatisticsStorage storage;
    private StatisticsService statisticsService;
    private IntegrationManager integrationManager;
    private PinataManager pinataManager;
    private VotePartyService votePartyService;
    private MoneyPoolService moneyPoolService;
    private SchedulerService schedulerService;

    private final BukkitContext context = new BukkitContext.Factory(this, "faa0a53659ec1c9512c139705449a3d9")
            .metrics(Metrics.Factory::create)
            .create();

    private static final ConcurrentHashMap<UUID, MenuUtil> menuUtilMap = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        supremePinata = this;

        context.ready();

        saveDefaultConfig();
        saveResourceIfMissing("data.yml");
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("pinatas/default.yml");
        saveResourceIfMissing("pinatas/vote.yml");
        saveResourceIfMissing("pinatas/money.yml");
        saveResourceIfMissing("pinatas/legendary.yml");

        this.messages = new MessageService(this);
        this.dataFile = new DataFile(this);
        this.dataFile.reload();
        this.schedulerService = new SchedulerService(this);
        this.locationService = new LocationService(this);
        this.integrationManager = new IntegrationManager(this);
        this.rewardService = new RewardService(this, integrationManager, messages);
        this.rewardService.scheduler(schedulerService);
        this.pinataRegistry = new PinataRegistry(this, rewardService);
        this.storage = new JdbcStatisticsStorage(this, dataFile.settings());
        this.statisticsService = new StatisticsService(storage, getLogger());
        this.pinataManager = new PinataManager(this, schedulerService, messages, pinataRegistry, locationService, rewardService, statisticsService);
        this.votePartyService = new VotePartyService(this, messages, pinataManager);
        this.moneyPoolService = new MoneyPoolService(this, messages, pinataManager, integrationManager);

        reloadServices(false);
        storage.start().thenRun(() -> getLogger().info("SQLite statistics storage ready."));
        integrationManager.enable(statisticsService, votePartyService);

        Bukkit.getPluginManager().registerEvents(new PinataProtectionListener(pinataManager), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        new VoteListenerHook(this, votePartyService).register();

        PinataCommand command = new PinataCommand(this, messages, pinataRegistry, pinataManager, locationService, votePartyService, statisticsService);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("pinata"), "pinata command missing from plugin.yml");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        MoneyPoolCommand poolCommand = new MoneyPoolCommand(messages, moneyPoolService, integrationManager);
        PluginCommand pluginPoolCommand = Objects.requireNonNull(getCommand("pinatapool"), "pinatapool command missing from plugin.yml");
        pluginPoolCommand.setExecutor(poolCommand);
        pluginPoolCommand.setTabCompleter(poolCommand);

        SupremePinataProvider.set(this, pinataManager, pinataRegistry, rewardService, statisticsService);
        getLogger().info("SupremePinata enabled with " + pinataRegistry.types().size() + " pinata type(s).");
    }

    @Override
    public void onDisable() {
        if (pinataManager != null) pinataManager.shutdown();
        if (votePartyService != null) votePartyService.shutdown();
        if (integrationManager != null) integrationManager.disable();
        if (statisticsService != null) statisticsService.flushAndShutdown();
        SupremePinataProvider.clear();

        context.shutdown();
    }

    public void reloadServices(boolean stopActiveEvent) {
        reloadConfig();
        dataFile.reload();
        messages.reload();
        locationService.reload();
        rewardService.reload();
        pinataRegistry.reload();
        votePartyService.reload();
        moneyPoolService.reload();
        integrationManager.reload();
        if (stopActiveEvent) {
            pinataManager.stopActiveEvent("reload");
        }
    }

    private void saveResourceIfMissing(String path) {
        if (!getDataFolder().toPath().resolve(path).toFile().exists()) {
            saveResource(path, false);
        }
    }

    public static MenuUtil getMenuUtil(Player player) {
        return menuUtilMap.computeIfAbsent(player.getUniqueId(), uuid -> new MenuUtil(player));
    }

    public static SupremePinata getInstance() {
        return supremePinata;
    }

    public ConcurrentHashMap<UUID, MenuUtil> getMenuUtil() {
        return menuUtilMap;
    }

    public VotePartyService getVotePartyService() {
        return votePartyService;
    }

    public PinataRegistry getPinataRegistry() {
        return pinataRegistry;
    }

    public PinataManager getPinataManager() {
        return pinataManager;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public MoneyPoolService getMoneyPoolService() {
        return moneyPoolService;
    }
}
