package dev.ricodevvv.riven.game.loot;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootGroupTest {

    private static LootItem item(Material material, double weight) {
        return new LootItem(new ItemStack(material), weight, 1, 1);
    }

    @Test
    void realChanceOfSingleRollIsTheWeightShare() {
        final LootItem stone = item(Material.STONE, 75);
        final LootItem dirt = item(Material.DIRT, 25);
        final LootGroup group = new LootGroup("blocks", 100, 1, 1, true, Arrays.asList(stone, dirt));

        assertEquals(75, group.getRealChance(stone), 0.0001);
        assertEquals(25, group.getRealChance(dirt), 0.0001);
    }

    @Test
    void groupChanceScalesRealChance() {
        final LootItem sword = item(Material.IRON_SWORD, 1);
        final LootGroup group = new LootGroup("swords", 50, 1, 1, true, Collections.singletonList(sword));

        assertEquals(50, group.getRealChance(sword), 0.0001);
    }

    @Test
    void uniqueGroupsNeverRepeat() {
        final LootGroup group = new LootGroup("armor", 100, 3, 3, true, Arrays.asList(item(Material.IRON_HELMET, 1), item(Material.IRON_BOOTS, 1), item(Material.IRON_LEGGINGS, 1)));
        final List<ItemStack> items = group.roll(new Random(1));

        assertEquals(3, items.size());
        assertEquals(3, items.stream().map(ItemStack::getType).distinct().count());
    }

    @Test
    void amountsStayInRange() {
        final LootItem arrows = new LootItem(new ItemStack(Material.ARROW), 1, 8, 16);
        final Random random = new Random(7);

        for (int i = 0; i < 100; i++) {
            final int amount = arrows.create(random).getAmount();

            assertTrue(amount >= 8 && amount <= 16);
        }
    }

}
