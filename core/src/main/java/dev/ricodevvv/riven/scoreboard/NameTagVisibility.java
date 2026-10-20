package dev.ricodevvv.riven.scoreboard;

/**
 * When the name above the members' heads is visible.
 */
public enum NameTagVisibility {

    ALWAYS("always"),
    NEVER("never"),
    HIDE_FOR_OTHER_TEAMS("hideForOtherTeams"),
    HIDE_FOR_OWN_TEAM("hideForOwnTeam");

    private final String value;

    NameTagVisibility(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
