package dev.ricodevvv.riven.module;

import dev.ricodevvv.riven.Riven;

/**
 * A piece of Riven you plug in on top of the core: the game engine, the data layer, the network layer or
 * anything of your own. Modules are enabled in the order you install them and disabled in reverse.
 */
public interface RivenModule {

    /**
     * Called once when the module gets installed.
     *
     * @param riven running Riven instance
     */
    void enable(Riven riven);

    /**
     * Called when Riven shuts down. Release everything here: tasks, listeners, connections.
     */
    void disable();

    /**
     * @return name used in logs
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

}
