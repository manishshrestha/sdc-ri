package org.ieee11073.sdc.common.helper;

import java.lang.reflect.Field;
import java.util.function.Predicate;

/**
 * Utility to stringify objects in conformance with the SDCri coding conventions.
 */
public class ObjectStringifier {
    /**
     * Stringifies annotated fields of a given object.
     * <p>
     * This function considers every field that is annotated with {@link Stringified}.
     *
     * @param obj the object to stringify.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringify(T obj) {
        return stringifyWithFilter(obj, field -> field.isAnnotationPresent(Stringified.class));
    }

    /**
     * Stringifies some fields of a given object.
     *
     * @param obj the object to stringify.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringify(T obj, String[] fieldNames) {
        return stringifyWithFilter(obj, field -> {
            for (String fieldName : fieldNames) {
                if (field.getName().equals(fieldName)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Stringifies all fields of a given object.
     * <p>
     * In order to filter out certain fields please use either {@link #stringify(Object)} or
     * {@link #stringify(Object, String[])}.
     *
     * @param obj the object to stringify.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringifyAll(T obj) {
        return stringifyWithFilter(obj, field -> !field.getName().startsWith("this$"));
    }

    private static <T> String stringifyWithFilter(T obj, Predicate<Field> filter) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(obj.getClass().getSimpleName());
        stringBuffer.append("(");

        try {
            appendToStringProperties(stringBuffer, obj, filter);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    private static void appendToStringProperties(StringBuffer stringBuffer,
                                                 Object obj,
                                                 Predicate<Field> predicate) throws IllegalAccessException {
        Field[] fields = obj.getClass().getDeclaredFields();
        int insertedFields = 0;
        for (Field field: fields) {
            field.setAccessible(true);
            if (!predicate.test(field)) {
                continue;
            }
            if (insertedFields++ > 0) {
                stringBuffer.append(';');
            }

            stringBuffer.append(field.getName());
            stringBuffer.append('=');
            stringBuffer.append(field.get(obj)); // null value is ok; will be converted to string "null"
        }
    }
}
