package dev.ricodevvv.riven.data.level;

/**
 * A level range with its own look, like Hypixel prestiges: {@code &7[%level%✫]} for stone, {@code &f[%level%✫]}
 * for iron... It can need a permission on top of the level (custom conditions).
 */
public class Prestige {

    private final String id;
    private final int minLevel;
    private final String format;
    private final String permission;

    /**
     * @param id prestige id
     * @param minLevel first level of the prestige
     * @param format level format with %level%
     * @param permission extra permission needed, can be null
     */
    public Prestige(String id, int minLevel, String format, String permission) {
        this.id = id;
        this.minLevel = minLevel;
        this.format = format;
        this.permission = permission;
    }

    public String getId() {
        return this.id;
    }

    public int getMinLevel() {
        return this.minLevel;
    }

    public String getFormat() {
        return this.format;
    }

    public String getPermission() {
        return this.permission;
    }

}
