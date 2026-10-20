package dev.ricodevvv.riven.util;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Text helpers: colors, multi-line lores, roman numbers, progress bars and centered chat lines.
 */
public final class TextUtil {

    private static final String[] ROMAN = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private static final int CHAT_CENTER_PX = 154;

    private TextUtil() {}

    /**
     * Turns {@code &} codes into Minecraft colors.
     *
     * @param text text
     * @return colored text
     */
    public static String colorize(String text) {
        return text == null ? null : ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Colors every line of a list.
     *
     * @param lines lines
     * @return new list with colored lines
     */
    public static List<String> colorize(List<String> lines) {
        final List<String> result = new ArrayList<>(lines.size());

        for (String line : lines) {
            result.add(colorize(line));
        }
        return result;
    }

    /**
     * Splits a text by line breaks and makes every line gray, like HyriBedWars lores.
     *
     * @param text text with \n
     * @return the lines
     */
    public static List<String> loreToList(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> lines = new ArrayList<>();

        for (String line : text.split("\n")) {
            lines.add(ChatColor.GRAY + line);
        }
        return lines;
    }

    /**
     * @param number number between 0 and 10
     * @return the roman number, or the number itself when it's out of range
     */
    public static String toRoman(int number) {
        return number >= 0 && number < ROMAN.length ? ROMAN[number] : String.valueOf(number);
    }

    /**
     * First letter upper case, rest lower case.
     *
     * @param text text
     * @return capitalized text
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }

    /**
     * Turns {@code SOME_ENUM_NAME} into {@code Some Enum Name}.
     *
     * @param name enum constant name
     * @return pretty name
     */
    public static String prettyEnum(String name) {
        final StringBuilder builder = new StringBuilder();

        for (String word : name.split("_")) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(capitalize(word));
        }
        return builder.toString();
    }

    /**
     * Formats a number with thousand separators: 12345 becomes 12,345.
     *
     * @param number number
     * @return formatted number
     */
    public static String formatNumber(double number) {
        return new DecimalFormat("#,###.##", DecimalFormatSymbols.getInstance(Locale.US)).format(number);
    }

    /**
     * Builds a progress bar like {@code ■■■■■■■■■■} with a filled and an empty color.
     *
     * @param current current value
     * @param max max value
     * @param length how many symbols the bar has
     * @param symbol bar symbol
     * @param filled color of the filled part
     * @param empty color of the empty part
     * @return the bar
     */
    public static String progressBar(double current, double max, int length, String symbol, String filled, String empty) {
        final double ratio = max <= 0 ? 0 : Math.max(0, Math.min(1, current / max));
        final int full = (int) Math.round(ratio * length);
        final StringBuilder builder = new StringBuilder(filled);

        for (int i = 0; i < length; i++) {
            if (i == full) {
                builder.append(empty);
            }
            builder.append(symbol);
        }
        return builder.toString();
    }

    /**
     * Pads a chat line with spaces so it looks centered in the default chat width.
     *
     * @param message colored message
     * @return the centered message
     */
    public static String center(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        int pixels = 0;
        boolean previousCode = false;
        boolean bold = false;

        for (char c : message.toCharArray()) {
            if (c == ChatColor.COLOR_CHAR) {
                previousCode = true;
                continue;
            }

            if (previousCode) {
                previousCode = false;
                bold = c == 'l' || c == 'L' || (bold && "kmnoKMNO".indexOf(c) >= 0);
                continue;
            }

            pixels += charWidth(c) + (bold && c != ' ' ? 1 : 0) + 1;
        }

        final int toCompensate = CHAT_CENTER_PX - pixels / 2;
        final StringBuilder builder = new StringBuilder();

        for (int compensated = 0; compensated < toCompensate; compensated += charWidth(' ') + 1) {
            builder.append(' ');
        }
        return builder + message;
    }

    private static int charWidth(char c) {
        switch (c) {
            case 'i': case '!': case ',': case '.': case ':': case ';': case '|': case '\'':
                return 1;
            case 'l': case '`':
                return 2;
            case 'I': case 't': case '[': case ']': case ' ':
                return 3;
            case 'f': case 'k': case '<': case '>': case '(': case ')': case '{': case '}': case '"': case '*':
                return 4;
            case '@': case '~':
                return 6;
            default:
                return 5;
        }
    }

}
