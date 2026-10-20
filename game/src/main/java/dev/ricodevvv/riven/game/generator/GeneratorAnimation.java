package dev.ricodevvv.riven.game.generator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

/**
 * Animation on top of the generator.
 */
public interface GeneratorAnimation {

    void start();

    void stop();

    /**
     * Hyrame's animation: an invisible armor stand with the block on its head, spinning faster and slower.
     */
    class Default implements GeneratorAnimation {

        private final JavaPlugin plugin;
        private final Location location;
        private final ItemStack item;

        private ArmorStand armorStand;
        private BukkitTask task;
        private double rotate;
        private boolean up = true;

        public Default(JavaPlugin plugin, Location location, ItemStack item) {
            this.plugin = plugin;
            this.location = location;
            this.item = item;
        }

        @Override
        public void start() {
            this.armorStand = this.location.getWorld().spawn(this.location.clone().add(0.0D, 0.6D, 0.0D), ArmorStand.class);
            this.armorStand.setVisible(false);
            this.armorStand.setHelmet(this.item);
            this.armorStand.setGravity(false);
            this.armorStand.setRemoveWhenFarAway(false);
            this.armorStand.setCanPickupItems(false);
            this.armorStand.setArms(false);
            this.armorStand.setBasePlate(false);
            this.armorStand.setMarker(true);

            this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::rotate, 0L, 1L);
        }

        private void rotate() {
            if (this.armorStand == null || this.armorStand.isDead()) {
                return;
            }

            if (this.up) {
                if (this.rotate >= 540) {
                    this.up = false;
                }
                this.rotate += this.rotate > 500 ? 1 : this.rotate > 470 ? 2 : this.rotate > 450 ? 3 : 4;
            } else {
                if (this.rotate <= 0) {
                    this.up = true;
                }
                this.rotate -= this.rotate > 120 ? 4 : this.rotate > 90 ? 3 : this.rotate > 70 ? 2 : 1;
            }

            this.armorStand.setHeadPose(new EulerAngle(0, Math.toRadians(this.rotate), 0));
        }

        @Override
        public void stop() {
            if (this.task != null) {
                this.task.cancel();
            }

            if (this.armorStand != null) {
                this.armorStand.remove();
            }
        }

    }

}
