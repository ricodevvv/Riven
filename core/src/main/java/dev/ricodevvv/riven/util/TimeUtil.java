package dev.ricodevvv.riven.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Time formatting for scoreboards and messages.
 */
public final class TimeUtil {

    private static final String DATE_PATTERN = "dd/MM/yyyy";

    private TimeUtil() {}

    /**
     * Turns seconds into m:ss (or h:mm:ss past one hour).
     *
     * @param seconds time in seconds
     * @return formatted time
     */
    public static String formatTime(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }

        final long hours = seconds / 3600;
        final long minutes = (seconds % 3600) / 60;
        final long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }

    /**
     * @return today's date in the format scoreboards use
     */
    public static String getCurrentFormattedDate() {
        return new SimpleDateFormat(DATE_PATTERN).format(new Date());
    }

    /**
     * Turns milliseconds into something like "1h 20m 5s".
     *
     * @param millis duration in milliseconds
     * @return readable duration
     */
    public static String formatDuration(long millis) {
        long seconds = millis / 1000;

        final long days = seconds / 86400;
        seconds %= 86400;
        final long hours = seconds / 3600;
        seconds %= 3600;
        final long minutes = seconds / 60;
        seconds %= 60;

        final StringBuilder builder = new StringBuilder();

        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(seconds).append("s");

        return builder.toString();
    }

    /**
     * Parses durations like {@code 90}, {@code 30s}, {@code 5m}, {@code 2h} or {@code 1d} into seconds.
     *
     * @param text duration text
     * @return seconds, or -1 if it can't be parsed
     */
    public static long parseSeconds(String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }

        final char unit = Character.toLowerCase(text.charAt(text.length() - 1));
        final String number = Character.isDigit(unit) ? text : text.substring(0, text.length() - 1);

        try {
            final long value = Long.parseLong(number.trim());

            switch (unit) {
                case 'm':
                    return value * 60;
                case 'h':
                    return value * 3600;
                case 'd':
                    return value * 86400;
                default:
                    return value;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
