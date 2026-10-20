package dev.ricodevvv.riven.data.sql;

import java.sql.SQLException;

/**
 * A function that may throw {@link SQLException}, used to map result sets.
 *
 * @param <T> input
 * @param <R> output
 */
@FunctionalInterface
public interface SqlFunction<T, R> {

    R apply(T value) throws SQLException;

}
