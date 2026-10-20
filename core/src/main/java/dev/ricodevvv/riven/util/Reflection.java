package dev.ricodevvv.riven.util;

import java.lang.reflect.Field;

/**
 * Bare-minimum reflection to poke private fields on packets and NMS classes.
 */
public final class Reflection {

    private Reflection() {}

    /**
     * Sets a field, looking it up on the object's class and its parents.
     *
     * @param fieldName field name
     * @param object object to change
     * @param value new value
     */
    public static void setField(String fieldName, Object object, Object value) {
        try {
            final Field field = findField(object.getClass(), fieldName);

            field.setAccessible(true);
            field.set(object, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't write field " + fieldName + " on " + object.getClass().getName(), e);
        }
    }

    /**
     * Reads a field, looking it up on the object's class and its parents.
     *
     * @param fieldName field name
     * @param object object to read
     * @param <T> expected type
     * @return the field value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getField(String fieldName, Object object) {
        try {
            final Field field = findField(object.getClass(), fieldName);

            field.setAccessible(true);

            return (T) field.get(object);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't read field " + fieldName + " on " + object.getClass().getName(), e);
        }
    }

    /**
     * Reads a static field.
     *
     * @param clazz class holding the field
     * @param fieldName field name
     * @param <T> expected type
     * @return the field value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getStaticField(Class<?> clazz, String fieldName) {
        try {
            final Field field = findField(clazz, fieldName);

            field.setAccessible(true);

            return (T) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't read static field " + fieldName + " on " + clazz.getName(), e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

}
