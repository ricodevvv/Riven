package dev.ricodevvv.riven.data.level;

import dev.ricodevvv.riven.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Picks the prestige icon for a level.
 *
 * <pre>
 * prestiges:
 *   stone: { min-level: 1, format: "&amp;7[%level%✫]" }
 *   iron: { min-level: 100, format: "&amp;f[%level%✫]" }
 *   vip: { min-level: 1, format: "&amp;6[%level%✪]", permission: "skywars.prestige.vip" }
 * </pre>
 */
public class PrestigeRegistry {

    private final List<Prestige> prestiges = new ArrayList<>();

    public PrestigeRegistry load(ConfigurationSection section) {
        this.prestiges.clear();

        if (section != null) {
            for (String id : section.getKeys(false)) {
                final ConfigurationSection prestige = section.getConfigurationSection(id);

                if (prestige != null) {
                    this.prestiges.add(new Prestige(id, prestige.getInt("min-level", 1), prestige.getString("format", "&7[%level%]"), prestige.getString("permission")));
                }
            }
        }

        this.prestiges.sort(Comparator.comparingInt(Prestige::getMinLevel));
        return this;
    }

    public PrestigeRegistry add(Prestige prestige) {
        this.prestiges.add(prestige);
        this.prestiges.sort(Comparator.comparingInt(Prestige::getMinLevel));
        return this;
    }

    /**
     * @param level level
     * @param player player for permission checks, can be null to ignore permission prestiges
     * @return the best prestige they qualify for, or null
     */
    public Prestige get(int level, Player player) {
        Prestige result = null;

        for (Prestige prestige : this.prestiges) {
            if (prestige.getMinLevel() > level) {
                continue;
            }

            if (prestige.getPermission() != null && (player == null || !player.hasPermission(prestige.getPermission()))) {
                continue;
            }

            if (result == null || prestige.getMinLevel() >= result.getMinLevel()) {
                result = prestige;
            }
        }
        return result;
    }

    /**
     * @param level level
     * @param player player, can be null
     * @return the colored level badge
     */
    public String format(int level, Player player) {
        final Prestige prestige = this.get(level, player);

        return TextUtil.colorize((prestige == null ? "&7[%level%]" : prestige.getFormat()).replace("%level%", String.valueOf(level)));
    }

    public List<Prestige> getPrestiges() {
        return Collections.unmodifiableList(this.prestiges);
    }

}
