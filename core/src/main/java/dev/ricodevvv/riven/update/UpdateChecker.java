package dev.ricodevvv.riven.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

/**
 * Checks the GitHub releases of a repo for a newer version and tells admins when they join.
 */
public class UpdateChecker implements Listener {

    private static final long CHECK_INTERVAL = 20L * 60L * 60L * 6L;

    private final JavaPlugin plugin;
    private final String repository;
    private final String permission;
    private final BiFunction<Player, String, String> message;
    private volatile String latest;

    /**
     * @param plugin plugin
     * @param repository owner/repo on GitHub
     * @param permission who gets told in game
     * @param message builds the in-game message from the player and the new version
     */
    public UpdateChecker(JavaPlugin plugin, String repository, String permission, BiFunction<Player, String, String> message) {
        this.plugin = plugin;
        this.repository = repository;
        this.permission = permission;
        this.message = message;
    }

    /**
     * Checks now and then every six hours.
     */
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::check, 100L, CHECK_INTERVAL);
    }

    private void check() {
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL("https://api.github.com/repos/" + this.repository + "/releases/latest").openConnection();

            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", this.plugin.getName());
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                return;
            }

            try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                final JsonElement tag = new JsonParser().parse(reader).getAsJsonObject().get("tag_name");
                final String version = tag == null ? null : tag.getAsString().replaceFirst("^[vV]", "");

                if (version != null && isNewer(version, this.plugin.getDescription().getVersion())) {
                    if (!version.equals(this.latest)) {
                        this.plugin.getLogger().info("There's a new version of " + this.plugin.getName() + ": " + version
                                + " (you have " + this.plugin.getDescription().getVersion() + ") https://github.com/" + this.repository + "/releases");
                    }
                    this.latest = version;
                }
            }
        } catch (Exception e) {
            this.plugin.getLogger().fine("Couldn't check for updates: " + e.getMessage());
        }
    }

    /**
     * Compares number by number and ignores whatever goes after a dash (like the -build.N of releases).
     *
     * @param candidate GitHub version
     * @param current installed version
     * @return true if the GitHub one is newer
     */
    public static boolean isNewer(String candidate, String current) {
        final String[] remote = candidate.split("-", 2)[0].split("\\.");
        final String[] local = current.split("-", 2)[0].split("\\.");

        for (int i = 0; i < Math.max(remote.length, local.length); i++) {
            final int remotePart = i < remote.length ? parse(remote[i]) : 0;
            final int localPart = i < local.length ? parse(local[i]) : 0;

            if (remotePart != localPart) {
                return remotePart > localPart;
            }
        }
        return false;
    }

    private static int parse(String part) {
        try {
            return Integer.parseInt(part.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getLatest() {
        return this.latest;
    }

    public String getRepository() {
        return this.repository;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if (this.latest != null && player.hasPermission(this.permission)) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> player.sendMessage(this.message.apply(player, this.latest)), 40L);
        }
    }

}
