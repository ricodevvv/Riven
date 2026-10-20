package dev.ricodevvv.riven.game.sign;

/**
 * Something a join sign can point at: usually a game or arena, local or on another server.
 */
public interface SignTarget {

    /**
     * @return unique id, stable while the target lives
     */
    String getId();

    /**
     * @return true if players can still join (waiting or starting)
     */
    boolean isJoinable();

    /**
     * @return the state shown on the sign, matches a key of the sign format (waiting, starting, playing, full...)
     */
    String getState();

    /**
     * Placeholders for the sign lines, like %map%, %players%, %max%, %mode%.
     *
     * @param line raw line
     * @return line with values in
     */
    String format(String line);

}
