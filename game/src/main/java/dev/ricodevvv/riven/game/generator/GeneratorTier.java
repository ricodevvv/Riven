package dev.ricodevvv.riven.game.generator;

import org.bukkit.entity.Player;

import java.util.function.Function;

/**
 * Generator tier: how often it drops, how much it piles up and if it splits pickups. Port of IHyriGeneratorTier.
 */
public interface GeneratorTier {

    /**
     * @return tier name per player (for example "II")
     */
    Function<Player, String> getName();

    /**
     * @return max items piled on the ground, -1 for no limit
     */
    int getSpawnLimit();

    /**
     * @return ticks between drops (20 ticks = 1 second)
     */
    long getTimeBetweenSpawns();

    /**
     * @return true if picking it up gives a copy to nearby players
     */
    boolean isSplitting();

    /**
     * Builder so you don't have to implement the interface by hand.
     */
    class Builder {

        private Function<Player, String> name = player -> "";
        private int spawnLimit = 12;
        private long timeBetweenSpawns = 8 * 20;
        private boolean splitting;

        public Builder withName(Function<Player, String> name) {
            this.name = name;
            return this;
        }

        public Builder withSpawnLimit(int spawnLimit) {
            this.spawnLimit = spawnLimit;
            return this;
        }

        public Builder withTimeBetweenSpawns(long timeBetweenSpawns) {
            this.timeBetweenSpawns = Math.max(1L, timeBetweenSpawns);
            return this;
        }

        public Builder withSplitting(boolean splitting) {
            this.splitting = splitting;
            return this;
        }

        public GeneratorTier build() {
            final Function<Player, String> name = this.name;
            final int spawnLimit = this.spawnLimit;
            final long timeBetweenSpawns = this.timeBetweenSpawns;
            final boolean splitting = this.splitting;

            return new GeneratorTier() {
                @Override
                public Function<Player, String> getName() {
                    return name;
                }

                @Override
                public int getSpawnLimit() {
                    return spawnLimit;
                }

                @Override
                public long getTimeBetweenSpawns() {
                    return timeBetweenSpawns;
                }

                @Override
                public boolean isSplitting() {
                    return splitting;
                }
            };
        }

    }

}
