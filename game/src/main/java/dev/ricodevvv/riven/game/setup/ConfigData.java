package dev.ricodevvv.riven.game.setup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a map configuration that the guided setup can fill. Same as in HyriBuild.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigData {

    /**
     * @return name the builder sees (can be a language key)
     */
    String name();

}
