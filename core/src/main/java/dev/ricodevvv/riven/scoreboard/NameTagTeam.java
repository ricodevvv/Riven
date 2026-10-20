package dev.ricodevvv.riven.scoreboard;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tab / name tag team (prefix, suffix and name visibility). Port of HyriScoreboardTeam.
 */
public class NameTagTeam {

    private final String name;
    private String prefix;
    private String suffix;
    private NameTagVisibility nameTagVisibility;
    private final Set<String> players = new HashSet<>();

    /**
     * @param name internal name, 16 characters max. The tab sorts teams alphabetically by it.
     * @param prefix prefix (16 characters max)
     * @param suffix suffix (16 characters max)
     * @param nameTagVisibility name tag visibility
     */
    public NameTagTeam(String name, String prefix, String suffix, NameTagVisibility nameTagVisibility) {
        this.name = name.length() > 16 ? name.substring(0, 16) : name;
        this.prefix = this.trim(prefix);
        this.suffix = this.trim(suffix);
        this.nameTagVisibility = nameTagVisibility;
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 16 ? value.substring(0, 16) : value;
    }

    public String getName() {
        return this.name;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = this.trim(prefix);
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = this.trim(suffix);
    }

    public NameTagVisibility getNameTagVisibility() {
        return this.nameTagVisibility;
    }

    public void setNameTagVisibility(NameTagVisibility nameTagVisibility) {
        this.nameTagVisibility = nameTagVisibility;
    }

    void addPlayer(String player) {
        this.players.add(player);
    }

    void removePlayer(String player) {
        this.players.remove(player);
    }

    public boolean contains(String player) {
        return this.players.contains(player);
    }

    public Set<String> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

}
