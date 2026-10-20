package dev.ricodevvv.riven.data.stats;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every stat of a player, split by scope. A scope is just a string: {@code total}, {@code mode:SOLO},
 * {@code kit:archer}, {@code season:2026}, {@code season:2026:mode:SOLO}, {@code year:2026}... see {@link StatScope}.
 * Values are longs; times are stored in milliseconds or seconds, whatever the plugin decides.
 */
public class Statistics {

    private Map<String, Map<String, Long>> scopes = new LinkedHashMap<>();

    /**
     * @param scope scope
     * @param stat stat name
     * @return the value, 0 if it was never set
     */
    public long get(String scope, String stat) {
        final Map<String, Long> values = this.scopes.get(scope);
        final Long value = values == null ? null : values.get(stat.toUpperCase());

        return value == null ? 0 : value;
    }

    public void set(String scope, String stat, long value) {
        this.scopes.computeIfAbsent(scope, key -> new LinkedHashMap<>()).put(stat.toUpperCase(), value);
    }

    /**
     * Adds to a stat in every given scope.
     *
     * @param stat stat name
     * @param amount amount
     * @param scopes scopes to touch
     */
    public void add(String stat, long amount, String... scopes) {
        for (String scope : scopes) {
            if (scope != null) {
                this.set(scope, stat, this.get(scope, stat) + amount);
            }
        }
    }

    /**
     * Keeps the highest value (records like most kills in a game or longest shot).
     *
     * @param stat stat name
     * @param value new value
     * @param scopes scopes to touch
     */
    public void max(String stat, long value, String... scopes) {
        for (String scope : scopes) {
            if (scope != null && value > this.get(scope, stat)) {
                this.set(scope, stat, value);
            }
        }
    }

    /**
     * Keeps the lowest value, treating 0 as "never set" (records like the fastest win).
     *
     * @param stat stat name
     * @param value new value
     * @param scopes scopes to touch
     */
    public void min(String stat, long value, String... scopes) {
        for (String scope : scopes) {
            if (scope == null || value <= 0) {
                continue;
            }

            final long current = this.get(scope, stat);

            if (current == 0 || value < current) {
                this.set(scope, stat, value);
            }
        }
    }

    /**
     * @param scope scope
     * @param numerator stat on top
     * @param denominator stat below
     * @return the ratio with 2 decimals (kills/deaths, hits/shots...), the numerator if the denominator is 0
     */
    public double ratio(String scope, String numerator, String denominator) {
        final long bottom = this.get(scope, denominator);
        final long top = this.get(scope, numerator);

        return bottom == 0 ? top : Math.round(top * 100.0D / bottom) / 100.0D;
    }

    /**
     * @param scope scope
     * @return every stat of a scope
     */
    public Map<String, Long> getScope(String scope) {
        final Map<String, Long> values = this.scopes.get(scope);

        return values == null ? Collections.emptyMap() : Collections.unmodifiableMap(values);
    }

    public Map<String, Map<String, Long>> getScopes() {
        return Collections.unmodifiableMap(this.scopes);
    }

    /**
     * Wipes a scope (for example to reset a season).
     *
     * @param scope scope
     */
    public void clear(String scope) {
        this.scopes.remove(scope);
    }

}
