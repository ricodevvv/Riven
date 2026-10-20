package dev.ricodevvv.riven.data.economy;

import dev.ricodevvv.riven.util.TextUtil;

/**
 * A currency: its id, how it looks and how it's formatted.
 */
public class Currency {

    private final String id;
    private final String singular;
    private final String plural;
    private final String color;

    /**
     * @param id id used in wallets, like "coins"
     * @param singular name for 1, like "Coin"
     * @param plural name for the rest, like "Coins"
     * @param color color code, like "&amp;6"
     */
    public Currency(String id, String singular, String plural, String color) {
        this.id = id.toLowerCase();
        this.singular = singular;
        this.plural = plural;
        this.color = color;
    }

    /**
     * @param amount amount
     * @return something like "§61,250 Coins"
     */
    public String format(long amount) {
        return TextUtil.colorize(this.color + TextUtil.formatNumber(amount) + " " + (amount == 1 ? this.singular : this.plural));
    }

    public String getId() {
        return this.id;
    }

    public String getSingular() {
        return this.singular;
    }

    public String getPlural() {
        return this.plural;
    }

    public String getColor() {
        return this.color;
    }

}
