package dev.ricodevvv.riven.npc;

import org.bukkit.entity.Player;

/**
 * What happens when someone clicks an NPC.
 */
@FunctionalInterface
public interface NPCInteractCallback {

    /**
     * @param rightClick true if it was a right click
     * @param player player who clicked
     */
    void call(boolean rightClick, Player player);

}
