package dev.ricodevvv.riven.game.setup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Groups values inside a nested object (the waiting room, each team, each island...). Same as in HyriBuild.
 * If the field is a list, every element is walked with its {@link ConfigListMemberId}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigOptionCategory {

    String id();

    /**
     * @return name the builder sees (language key or text)
     */
    String name();

}
