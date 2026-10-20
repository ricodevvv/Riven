package dev.ricodevvv.riven.game.setup;

import dev.ricodevvv.riven.game.setup.handler.AreaHandler;
import dev.ricodevvv.riven.game.setup.handler.AreasHandler;
import dev.ricodevvv.riven.game.setup.handler.BooleanHandler;
import dev.ricodevvv.riven.game.setup.handler.ConfigOptionHandler;
import dev.ricodevvv.riven.game.setup.handler.IntegerHandler;
import dev.ricodevvv.riven.game.setup.handler.LocationHandler;
import dev.ricodevvv.riven.game.setup.handler.LocationsHandler;
import dev.ricodevvv.riven.game.setup.handler.TextHandler;

import java.util.function.Function;

/**
 * Value types the guided setup knows how to ask for, each with its handler. HyriBuild's ones plus TEXT.
 */
public enum ConfigOptionType {

    BOOLEAN(BooleanHandler::new),
    INTEGER(IntegerHandler::new),
    TEXT(TextHandler::new),

    LOCATION(process -> new LocationHandler(process, false, false)),
    BLOCK_LOCATION(process -> new LocationHandler(process, false, true)),
    PLAYER_LOCATION(process -> new LocationHandler(process, true, false)),

    LOCATIONS(process -> new LocationsHandler(process, false, false)),
    BLOCK_LOCATIONS(process -> new LocationsHandler(process, false, true)),
    PLAYER_LOCATIONS(process -> new LocationsHandler(process, true, false)),

    AREA(AreaHandler::new),
    AREAS(AreasHandler::new);

    private final Function<ConfigProcess<?>, ConfigOptionHandler<?>> provider;

    ConfigOptionType(Function<ConfigProcess<?>, ConfigOptionHandler<?>> provider) {
        this.provider = provider;
    }

    /**
     * @param process process asking for it
     * @return a new handler for this type
     */
    public ConfigOptionHandler<?> newHandler(ConfigProcess<?> process) {
        return this.provider.apply(process);
    }

}
