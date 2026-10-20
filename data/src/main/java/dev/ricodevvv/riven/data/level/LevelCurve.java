package dev.ricodevvv.riven.data.level;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * How much xp each level needs. Either a formula (base + increment per level, capped) or a fixed list.
 *
 * <pre>
 * levels:
 *   base: 500        # xp from level 1 to 2
 *   increment: 250   # extra xp for each next level
 *   cap: 15000       # max xp per level
 *   max-level: 0     # 0 means no max
 *   # or: xp: [500, 1000, 2000, 3500]
 * </pre>
 */
public class LevelCurve {

    private final long base;
    private final long increment;
    private final long cap;
    private final int maxLevel;
    private final List<Long> fixed;

    public LevelCurve(long base, long increment, long cap, int maxLevel, List<Long> fixed) {
        this.base = Math.max(1, base);
        this.increment = Math.max(0, increment);
        this.cap = cap <= 0 ? Long.MAX_VALUE : cap;
        this.maxLevel = maxLevel;
        this.fixed = fixed == null ? new ArrayList<>() : new ArrayList<>(fixed);
    }

    /**
     * @param section levels section, null gives the default curve (500 + 250 per level up to 15000)
     * @return the curve
     */
    public static LevelCurve from(ConfigurationSection section) {
        if (section == null) {
            return new LevelCurve(500, 250, 15000, 0, null);
        }

        final List<Long> fixed = new ArrayList<>();

        for (Object value : section.getList("xp", new ArrayList<>())) {
            fixed.add(Long.parseLong(String.valueOf(value)));
        }

        return new LevelCurve(section.getLong("base", 500), section.getLong("increment", 250), section.getLong("cap", 15000), section.getInt("max-level", 0), fixed);
    }

    /**
     * @param level current level (1 based)
     * @return xp needed to go from this level to the next
     */
    public long xpForLevel(int level) {
        if (!this.fixed.isEmpty()) {
            return this.fixed.get(Math.min(Math.max(0, level - 1), this.fixed.size() - 1));
        }
        return Math.min(this.cap, this.base + this.increment * Math.max(0, level - 1));
    }

    /**
     * @param totalXp xp earned so far
     * @return the level for that xp (1 based)
     */
    public int getLevel(long totalXp) {
        int level = 1;
        long remaining = totalXp;

        while (remaining >= this.xpForLevel(level) && (this.maxLevel <= 0 || level < this.maxLevel)) {
            remaining -= this.xpForLevel(level);
            level++;
        }
        return level;
    }

    /**
     * @param totalXp xp earned so far
     * @return xp inside the current level
     */
    public long getProgress(long totalXp) {
        int level = 1;
        long remaining = totalXp;

        while (remaining >= this.xpForLevel(level) && (this.maxLevel <= 0 || level < this.maxLevel)) {
            remaining -= this.xpForLevel(level);
            level++;
        }
        return remaining;
    }

    /**
     * @param totalXp xp earned so far
     * @return xp the current level needs in total
     */
    public long getRequired(long totalXp) {
        return this.xpForLevel(this.getLevel(totalXp));
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

}
