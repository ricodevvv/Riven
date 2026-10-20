package dev.ricodevvv.riven.game.generator;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.hologram.Hologram;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Classic header: generator name, tier and countdown to the next drop.
 * Uses the {@code generator.header.tier} and {@code generator.header.timer} language keys.
 */
public class DefaultGeneratorHeader implements GeneratorHeader {

    private static final int TIER_LINE = 1;

    private final JavaPlugin plugin;
    private final ItemStack item;
    private final Function<Player, String> generatorName;
    private final Predicate<Player> visibility;

    private Hologram hologram;

    public DefaultGeneratorHeader(JavaPlugin plugin, ItemStack item, Function<Player, String> generatorName, Predicate<Player> visibility) {
        this.plugin = plugin;
        this.item = item;
        this.generatorName = generatorName;
        this.visibility = visibility;
    }

    @Override
    public ItemStack getItem() {
        return this.item;
    }

    @Override
    public void onCreate(Generator generator) {
        this.hologram = new Hologram.Builder(this.plugin, generator.getLocation().clone().add(0.0D, 2.6D, 0.0D))
                .withVisibility(this.visibility)
                .withLine(0, new Hologram.Line(this.generatorName))
                .withLine(TIER_LINE, new Hologram.Line(player -> GameFramework.get().getLanguage().get(player, "generator.header.tier")
                        .replace("%tier%", generator.getTier().getName().apply(player))))
                .withLine(2, new Hologram.Line(player -> this.timerLine(player, generator))
                        .withUpdate(20, line -> {}))
                .build();
    }

    private String timerLine(Player player, Generator generator) {
        return GameFramework.get().getLanguage().get(player, "generator.header.timer")
                .replace("%seconds%", String.valueOf(generator.getSecondsBeforeSpawn()));
    }

    @Override
    public void onUpgrade(Generator generator) {
        if (this.hologram != null) {
            this.hologram.updateLine(TIER_LINE);
        }
    }

    @Override
    public void onRemove(Generator generator) {
        if (this.hologram != null) {
            this.hologram.destroy();
        }
    }

}
