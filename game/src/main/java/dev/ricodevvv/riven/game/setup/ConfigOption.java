package dev.ricodevvv.riven.game.setup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A value to define during the guided setup. Every field with it becomes a step. Same as in HyriBuild.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigOption {

    /**
     * @return value type, decides how it's asked (location, area, number...)
     */
    ConfigOptionType type();

    /**
     * @return value id
     */
    String id();

    /**
     * @return name the builder sees (language key or text)
     */
    String name();

    /**
     * @return what has to go there (language key or text)
     */
    String description();

    /**
     * @return true to let the builder skip it with the "skip" item
     */
    boolean optional() default false;

}
