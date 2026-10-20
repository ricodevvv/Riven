package dev.ricodevvv.riven.game.timer;

import dev.ricodevvv.riven.game.GameFramework;
import dev.ricodevvv.riven.game.Game;
import dev.ricodevvv.riven.game.GamePlayer;
import dev.ricodevvv.riven.game.GameState;
import dev.ricodevvv.riven.util.Title;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Countdown before starting. Port of HyriGameStartingTimer: it starts on its own when there are enough
 * people, gets shorter when the game fills up and cancels if someone leaves.
 */
public class GameStartingTimer extends BukkitRunnable {

    private static final int FULL_TIME = 10;

    private final Game<?> game;
    private final int startingTime;
    private final List<Consumer<Integer>> listeners = new CopyOnWriteArrayList<>();

    private int time;
    private boolean running;
    private boolean forceStarting;

    /**
     * @param game game
     * @param startingTime normal countdown seconds
     */
    public GameStartingTimer(Game<?> game, int startingTime) {
        this.game = game;
        this.startingTime = startingTime;
        this.time = startingTime;
    }

    @Override
    public void run() {
        if (this.game.getState() == GameState.PLAYING || this.game.getState() == GameState.ENDED) {
            this.cancel();
            return;
        }

        if (this.game.canStart() || this.forceStarting) {
            this.tick();
        } else if (this.running) {
            this.reset(true);
        }
    }

    private void tick() {
        if (!this.running) {
            this.running = true;
            this.game.setState(GameState.READY);
        }

        if (this.game.isFull() && this.time > FULL_TIME) {
            this.time = FULL_TIME;
        }

        for (Consumer<Integer> listener : this.listeners) {
            listener.accept(this.time);
        }

        for (GamePlayer gamePlayer : this.game.getOnlinePlayers()) {
            final Player player = gamePlayer.getPlayer();

            player.setLevel(this.time);

            if (this.time <= 0) {
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
            } else if (this.time <= 5) {
                Title.sendTitle(player, ChatColor.AQUA + String.valueOf(this.time), "", 0, 20, 0);
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
            } else if (this.time == 30 || this.time == 20 || this.time == 10) {
                Title.sendTitle(player, ChatColor.DARK_AQUA + String.valueOf(this.time), "", 0, 20, 0);
                player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
            }
        }

        if (this.time <= 0) {
            this.cancel();
            this.game.start();
            return;
        }

        this.time--;
    }

    /**
     * Forces the start with 10 seconds even without enough people.
     */
    public void forceStart() {
        this.forceStarting = true;

        if (this.time > FULL_TIME) {
            this.time = FULL_TIME;
        }
    }

    private void reset(boolean notify) {
        this.running = false;
        this.forceStarting = false;
        this.time = this.startingTime;

        this.game.setState(GameState.WAITING);

        for (Consumer<Integer> listener : this.listeners) {
            listener.accept(-1);
        }

        if (!notify) {
            return;
        }

        for (GamePlayer gamePlayer : this.game.getOnlinePlayers()) {
            final Player player = gamePlayer.getPlayer();

            player.setLevel(0);

            Title.sendTitle(player, GameFramework.get().getLanguage().get(player, "game.starting.cancelled"), "", 0, 20, 0);
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 1.0F, 1.0F);
        }
    }

    /**
     * Shortens the countdown if it's above the given seconds.
     *
     * @param seconds cap
     */
    public void shortenTo(int seconds) {
        if (this.time > seconds) {
            this.time = seconds;
        }
    }

    /**
     * Adds something that runs every time the time changes (-1 when it gets cancelled).
     *
     * @param listener action
     */
    public void addListener(Consumer<Integer> listener) {
        this.listeners.add(listener);
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    /**
     * Moves the listeners to another countdown.
     *
     * @param other new countdown
     */
    public void transferListeners(GameStartingTimer other) {
        other.listeners.addAll(this.listeners);
        this.listeners.clear();
    }

    public int getTime() {
        return this.time;
    }

    public boolean isRunning() {
        return this.running;
    }

}
