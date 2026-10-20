package dev.ricodevvv.riven.game.setup;

import dev.ricodevvv.riven.game.setup.handler.ConfigOptionHandler;

import java.lang.reflect.Field;

/**
 * A value to define during the setup (HyriBuild's ConfigStep): which field it is, which object it goes into
 * and which category or list element it comes from.
 */
public class ConfigStep {

    private final ConfigOptionCategory category;
    private final Object target;
    private final String listMemberId;
    private final ConfigOption option;
    private final Field field;

    private ConfigOptionHandler<?> handler;

    /**
     * @param category value category, or null
     * @param target object the field gets written into
     * @param listMemberId list element it belongs to (like "Red"), or null
     * @param option option data
     * @param field field to write
     */
    public ConfigStep(ConfigOptionCategory category, Object target, String listMemberId, ConfigOption option, Field field) {
        this.category = category;
        this.target = target;
        this.listMemberId = listMemberId;
        this.option = option;
        this.field = field;
    }

    void write(Object value) {
        try {
            this.field.setAccessible(true);
            this.field.set(this.target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Couldn't write " + this.field.getName(), e);
        }
    }

    public ConfigOptionCategory getCategory() {
        return this.category;
    }

    public String getListMemberId() {
        return this.listMemberId;
    }

    public ConfigOption getOption() {
        return this.option;
    }

    public ConfigOptionHandler<?> getHandler() {
        return this.handler;
    }

    void setHandler(ConfigOptionHandler<?> handler) {
        this.handler = handler;
    }

}
