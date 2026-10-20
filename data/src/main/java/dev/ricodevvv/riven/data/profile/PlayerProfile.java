package dev.ricodevvv.riven.data.profile;

import dev.ricodevvv.riven.data.cosmetic.CosmeticData;
import dev.ricodevvv.riven.data.economy.Wallet;
import dev.ricodevvv.riven.data.stats.Statistics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base player profile: identity, wallet, statistics, cosmetics, kits, xp and settings. Extend it with your
 * own fields; the whole object is stored as one JSON document.
 */
public class PlayerProfile {

    private UUID uniqueId;
    private String name;
    private long firstJoin;
    private long lastSeen;
    private long playTime;
    private long experience;
    private String language;
    private Wallet wallet = new Wallet();
    private Statistics statistics = new Statistics();
    private CosmeticData cosmetics = new CosmeticData();
    private Map<String, Integer> kits = new LinkedHashMap<>();
    private Map<String, String> selectedKits = new LinkedHashMap<>();
    private Map<String, String> settings = new LinkedHashMap<>();

    protected PlayerProfile() {}

    public PlayerProfile(UUID uniqueId, String name) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = this.firstJoin;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFirstJoin() {
        return this.firstJoin;
    }

    public long getLastSeen() {
        return this.lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * @return milliseconds played on the server
     */
    public long getPlayTime() {
        return this.playTime;
    }

    public void addPlayTime(long millis) {
        this.playTime += Math.max(0, millis);
    }

    /**
     * @return total xp ever earned, turn it into a level with a {@code LevelCurve}
     */
    public long getExperience() {
        return this.experience;
    }

    public void setExperience(long experience) {
        this.experience = Math.max(0, experience);
    }

    public void addExperience(long experience) {
        this.setExperience(this.experience + experience);
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Wallet getWallet() {
        return this.wallet;
    }

    public Statistics getStatistics() {
        return this.statistics;
    }

    public CosmeticData getCosmetics() {
        return this.cosmetics;
    }

    /**
     * @param kit kit id
     * @return the owned level of a kit, 0 if not owned
     */
    public int getKitLevel(String kit) {
        final Integer level = this.kits.get(kit.toLowerCase());

        return level == null ? 0 : level;
    }

    public void setKitLevel(String kit, int level) {
        if (level <= 0) {
            this.kits.remove(kit.toLowerCase());
        } else {
            this.kits.put(kit.toLowerCase(), level);
        }
    }

    /**
     * @param category kit category (mode), use "default" if the plugin has none
     * @return the selected kit id, or null
     */
    public String getSelectedKit(String category) {
        return this.selectedKits.get(category.toLowerCase());
    }

    public void setSelectedKit(String category, String kit) {
        if (kit == null) {
            this.selectedKits.remove(category.toLowerCase());
        } else {
            this.selectedKits.put(category.toLowerCase(), kit.toLowerCase());
        }
    }

    public String getSetting(String key, String fallback) {
        final String value = this.settings.get(key);

        return value == null ? fallback : value;
    }

    public boolean getSetting(String key, boolean fallback) {
        final String value = this.settings.get(key);

        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public void setSetting(String key, Object value) {
        if (value == null) {
            this.settings.remove(key);
        } else {
            this.settings.put(key, String.valueOf(value));
        }
    }

}
