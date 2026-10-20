package dev.ricodevvv.riven.data.economy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Balances of a player, one per currency id (coins, souls, xp...). Lives inside the profile so it's saved
 * with everything else.
 */
public class Wallet {

    private Map<String, Long> balances = new LinkedHashMap<>();

    public long get(String currency) {
        final Long value = this.balances.get(currency.toLowerCase());

        return value == null ? 0 : value;
    }

    public void set(String currency, long amount) {
        this.balances.put(currency.toLowerCase(), Math.max(0, amount));
    }

    /**
     * @param currency currency
     * @param amount amount to add, negative removes (never below zero)
     * @return the new balance
     */
    public long add(String currency, long amount) {
        final long result = Math.max(0, this.get(currency) + amount);

        this.balances.put(currency.toLowerCase(), result);
        return result;
    }

    public boolean has(String currency, long amount) {
        return this.get(currency) >= amount;
    }

    /**
     * Takes money only if there's enough.
     *
     * @param currency currency
     * @param amount amount
     * @return true if it was taken
     */
    public boolean withdraw(String currency, long amount) {
        if (amount < 0 || !this.has(currency, amount)) {
            return false;
        }

        this.add(currency, -amount);
        return true;
    }

    public Map<String, Long> getBalances() {
        return Collections.unmodifiableMap(this.balances);
    }

}
