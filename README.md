# SupremePinata

**SupremePinata** is a free, open-source Pinata Party / server event plugin for modern Paper servers.

It targets Paper 1.21+ and Java 21, uses Adventure/MiniMessage for player-facing text, and is designed around configuration-driven pinata types, optional integrations, async storage, and clean APIs.

## Features

- Unlimited YAML-defined pinata types in `plugins/SupremePinata/pinatas/`
- `/pinata spawn <type>`, `/pinata stop`, `/pinata info`, `/pinata reload`, `/pinata help`
- Named locations with `/pinata location set|delete|list`
- Weighted reward pools for hit, participation, final-hit, and placement rewards
- Console commands, player commands, vanilla items, Vault money, experience, messages, broadcasts, and sounds
- Competitive leaderboard with predictable tie ordering
- Adventure BossBars with `%hits%`, `%remaining_hits%`, `%required_hits%`, `%progress%`, `%top_player%`, `%top_hits%`, `%time_remaining%`
- Built-in TextDisplay holograms for Paper 1.21+
- Vote party trigger with direct public vote plugin support for NuVotifier and VotifierPlus
- PlaceholderAPI placeholders when installed
- SQLite statistics storage behind a storage interface
- Public API and Bukkit events for developers
- Strong interaction protection for event entities
- Small plugin jar: optional APIs are compile-only, Paper provides Adventure/MiniMessage, and SQLite is loaded through Bukkit's `libraries` resolver instead of being shaded into the jar

## Requirements

- Java 21
- Paper 1.21+
- Folia is supported through region/entity scheduler detection and `folia-supported: true`
- No required dependencies

Optional integrations:

- PlaceholderAPI
- Vault-compatible economy
- NuVotifier or VotifierPlus
- SupremeTags via generic reward commands

## Build

```bat
gradlew.bat build
```

The included Windows bootstrap downloads Gradle 9.4.0 into `.gradle/bootstrap/` when the normal wrapper jar is unavailable. The compiled plugin jar is produced in `build/libs/`.

The runtime jar intentionally does **not** shade Paper, Adventure, PlaceholderAPI, Vault, or SQLite. SQLite is declared in `plugin.yml` under `libraries`, so Bukkit downloads it at runtime instead of inflating the plugin jar by ~13MB.

## Commands

| Command | Permission |
| --- | --- |
| `/pinata spawn <type>` | `supremepinata.command.spawn` |
| `/pinata spawn <type> [x y z world]` | `supremepinata.command.spawn` |
| `/pinata stop` | `supremepinata.command.stop` |
| `/pinata info` | `supremepinata.command.info` |
| `/pinata reload` | `supremepinata.command.reload` |
| `/pinata location set <name>` | `supremepinata.command.location` |
| `/pinata location delete <name>` | `supremepinata.command.location` |
| `/pinata location list` | `supremepinata.command.location` |
| `/pinata help` | `supremepinata.command.help` |

`supremepinata.admin` grants all administrative permissions.

## Configuration

Default files are generated on first startup:

```text
plugins/SupremePinata/
├── config.yml
├── messages.yml
├── data/
│   ├── locations.yml
│   └── statistics.db
└── pinatas/
    ├── default.yml
    ├── vote.yml
    └── legendary.yml
```

Create a new pinata by copying an existing file in `pinatas/`, changing `id`, `display-name`, event settings, effects, and reward pools, then running `/pinata reload`.

Pinata movement is configurable per pinata file:

```yaml
movement:
  enabled: true
  speed: 1.15
  radius: 20
```

Entity cosmetics are configurable per pinata file:

```yaml
entity:
  glow: true
  rainbow: true
  rainbow-interval-ticks: 10
```

`glow` toggles the vanilla glowing outline. `rainbow` cycles through supported entity colors, such as llama colors or sheep dye colors. If an entity type does not support colors, SupremePinata logs a warning and safely skips the rainbow effect.

The pinata picks random targets inside the configured radius around its original spawn location and moves with a fast panic-run style. It is pulled/teleported back safely if it somehow leaves the area. The built-in TextDisplay hologram updates its text at a controlled rate but follows the entity every tick with display interpolation so it stays visually attached instead of skipping behind.

Reload behavior is deliberate: active events are stopped by default during reload to avoid duplicate entities, bossbars, displays, listeners, or tasks. This is controlled by `reload.stop-active-event`.

## Reward Pool Example

```yaml
reward-pools:
  hit:
    common:
      weight: 70
      commands:
        - "eco give %player% 250"
    rare:
      weight: 25
      items:
        diamond:
          material: DIAMOND
          amount: 3
      message: "<aqua>You found 3 diamonds!"
    legendary:
      weight: 5
      commands:
        - "crate key give %player% legendary 1"
      broadcast: "<gold>%player% found a LEGENDARY reward!"
```

Commands are generic and do not require economy, crate, or SupremeTags plugins. Missing command providers simply result in the server command failing normally.

## Placeholders

## Vote Party Integration

Install NuVotifier or VotifierPlus and configure that plugin normally. SupremePinata listens for the public Votifier vote event directly and increments the vote-party counter when a vote is received. No built-in vote receiver, vote command bridge, or VotingPlugin command reward setup is required.

## Placeholders

When PlaceholderAPI is installed:

- `%supremepinata_votes%`
- `%supremepinata_votes_required%`
- `%supremepinata_votes_remaining%`
- `%supremepinata_hits%`
- `%supremepinata_parties%`
- `%supremepinata_wins%`
- `%supremepinata_final_hits%`
- `%supremepinata_rewards%`

## Developer API

Access the API after SupremePinata has enabled:

```java
SupremePinataApi api = SupremePinataProvider.api();
api.spawnPinata("legendary", location);
api.activeEvent().ifPresent(event -> event.leaderboard().forEach(System.out::println));
```

Events:

- `PinataSpawnEvent` cancellable
- `PinataHitEvent` cancellable
- `PinataRewardEvent` cancellable
- `PinataCompleteEvent`
- `PinataEndEvent`

The API avoids requiring consumers to depend on implementation details beyond public records and service interfaces.

## Production Notes

- SQLite operations run on a dedicated single-thread executor, not the server thread.
- Runtime pinata ticks use the Folia entity scheduler when Folia is detected, otherwise Bukkit scheduling is used.
- Entity spawning is dispatched through a region scheduler on Folia and through Bukkit's scheduler on standard Paper.
- Broadcasts and command rewards are routed through the Folia global-region scheduler when Folia is detected.
- The event loop updates once per second and does not scan all entities.
- BossBars and TextDisplays are reused and updated instead of recreated.
- MiniMessage is parsed only for current display/message updates and uses simple cached services for message files.
- All event entities, displays, bossbars, and tasks are cleaned on event end, reload, and plugin disable.
