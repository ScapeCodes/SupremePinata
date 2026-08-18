package net.scape.project.supremepinata.trigger;

public interface PinataTrigger {
    String id();
    void reload();
    void shutdown();
}
