package dev.ricodevvv.riven.game;

/**
 * State of a game.
 */
public enum GameState {

    WAITING(true),
    READY(true),
    PLAYING(false),
    ENDED(false);

    private final boolean accessible;

    GameState(boolean accessible) {
        this.accessible = accessible;
    }

    /**
     * @return true if players can still join
     */
    public boolean isAccessible() {
        return this.accessible;
    }

}
