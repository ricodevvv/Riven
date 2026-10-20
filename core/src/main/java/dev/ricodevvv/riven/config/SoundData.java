package dev.ricodevvv.riven.config;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * A sound with its volume and pitch, read from config as {@code SOUND:volume:pitch}.
 */
public class SoundData {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SoundData(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * Parses {@code LEVEL_UP}, {@code LEVEL_UP:1} or {@code LEVEL_UP:1:2}.
     *
     * @param text sound text
     * @return the sound, or null if the name doesn't exist
     */
    public static SoundData parse(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        final String[] parts = text.split(":");

        try {
            return new SoundData(Sound.valueOf(parts[0].trim().toUpperCase()),
                    parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0F,
                    parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0F);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void play(Player player) {
        player.playSound(player.getLocation(), this.sound, this.volume, this.pitch);
    }

    public void play(Location location) {
        location.getWorld().playSound(location, this.sound, this.volume, this.pitch);
    }

    public Sound getSound() {
        return this.sound;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    @Override
    public String toString() {
        return this.sound.name() + ":" + this.volume + ":" + this.pitch;
    }

}
