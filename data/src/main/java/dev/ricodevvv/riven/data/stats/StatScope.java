package dev.ricodevvv.riven.data.stats;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Builds scope names for {@link Statistics}, so every plugin uses the same ones and placeholders can find them.
 */
public final class StatScope {

    public static final String TOTAL = "total";

    private StatScope() {}

    public static String mode(String mode) {
        return "mode:" + mode.toUpperCase();
    }

    public static String kit(String kit) {
        return "kit:" + kit.toLowerCase();
    }

    public static String season(String season) {
        return "season:" + season;
    }

    public static String season(String season, String mode) {
        return "season:" + season + ":mode:" + mode.toUpperCase();
    }

    public static String year(int year) {
        return "year:" + year;
    }

    public static String currentYear() {
        return year(Calendar.getInstance().get(Calendar.YEAR));
    }

    /**
     * Every scope a game stat usually goes to: total, mode, kit, season, season + mode and the current year.
     *
     * @param mode game mode, can be null
     * @param kit kit id, can be null
     * @param season current season, can be null
     * @return the scopes
     */
    public static String[] of(String mode, String kit, String season) {
        final List<String> scopes = new ArrayList<>();

        scopes.add(TOTAL);
        scopes.add(currentYear());

        if (mode != null) {
            scopes.add(mode(mode));
        }

        if (kit != null) {
            scopes.add(kit(kit));
        }

        if (season != null) {
            scopes.add(season(season));

            if (mode != null) {
                scopes.add(season(season, mode));
            }
        }
        return scopes.toArray(new String[0]);
    }

}
