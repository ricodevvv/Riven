package dev.ricodevvv.riven.game.setup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * String field that names an element of a list during the setup (like "Red" or "Island 3"). Same as in HyriBuild.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigListMemberId {}
