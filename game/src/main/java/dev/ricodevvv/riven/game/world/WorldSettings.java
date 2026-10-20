package dev.ricodevvv.riven.game.world;

import org.bukkit.Difficulty;
import org.bukkit.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How game and setup worlds get prepared after loading: game rules, time, weather and difficulty.
 */
public class WorldSettings {

    private final Map<String, String> gameRules = new LinkedHashMap<>();
    private Difficulty difficulty = Difficulty.NORMAL;
    private long time = 6000L;

    public WorldSettings() {
        this.gameRules.put("doDaylightCycle", "false");
        this.gameRules.put("doMobSpawning", "false");
        this.gameRules.put("doFireTick", "false");
        this.gameRules.put("mobGriefing", "false");
        this.gameRules.put("randomTickSpeed", "0");
    }

    public WorldSettings gameRule(String rule, Object value) {
        this.gameRules.put(rule, String.valueOf(value));
        return this;
    }

    public WorldSettings difficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    public WorldSettings time(long time) {
        this.time = time;
        return this;
    }

    /**
     * Applies everything to a world.
     *
     * @param world world
     * @param autoSave true for setup worlds, false for game copies
     */
    public void apply(World world, boolean autoSave) {
        world.setAutoSave(autoSave);
        world.setKeepSpawnInMemory(false);
        world.setDifficulty(this.difficulty);

        for (Map.Entry<String, String> entry : this.gameRules.entrySet()) {
            world.setGameRuleValue(entry.getKey(), entry.getValue());
        }

        world.setTime(this.time);
        world.setStorm(false);
        world.setThundering(false);
    }

}
