package dev.ricodevvv.riven.game;

/**
 * Game type (1v1, solo, doubles...). Usually an enum.
 */
public interface GameType {

    /**
     * @return internal name, upper case (SOLO, DOUBLES...)
     */
    String getName();

    /**
     * @return pretty name to show
     */
    String getDisplayName();

    int getMinPlayers();

    int getMaxPlayers();

}
