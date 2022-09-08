package org.somda.sdc.common.util;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Predicate;

/**
 * Stringifies objects in conformance with the SDCri coding conventions.
 */
public class ObjectStringifier {
    /**
     * Stringifies annotated fields of a given object.
     * <p>
     * This function considers every field that is annotated with {@link Stringified}.
     *
     * @param obj the object to stringify.
     * @param <T> any object.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringify(T obj) {
        return stringifyWithFilter(obj, null, field -> field.isAnnotationPresent(Stringified.class));
    }

    /**
     * Stringifies annotated fields of a given object and appends defined map.
     *
     * @param obj       the object to stringify.
     * @param keyValues key-values to be additionally appended.
     * @param <T>       any object.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringify(T obj, SortedMap<String, Object> keyValues) {
        return stringifyWithFilter(obj, keyValues, field -> field.isAnnotationPresent(Stringified.class));
    }

    /**
     * Stringifies some fields of a given object.
     *
     * @param obj        the object to stringify.
     * @param fieldNames the fields of the class that are requested to be stringified.
     * @param <T>        any object.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringify(T obj, String[] fieldNames) {
        return stringifyWithFilter(obj, null, field -> {
            for (String fieldName : fieldNames) {
                if (field.getName().equals(fieldName)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Stringifies some fields of a given object and appends defined map.
     *
     * @param obj        the object to stringify.
     * @param fieldNames the fields of the class that are requested to be stringified.
     * @param keyValues  key-values to be additionally appended.
     * @param <T>        any object.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringify(T obj, String[] fieldNames, SortedMap<String, Object> keyValues) {
        return stringifyWithFilter(obj, keyValues, field -> {
            for (String fieldName : fieldNames) {
                if (field.getName().equals(fieldName)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Stringifies key-value pairs from a map.
     *
     * @param obj       the object used to access the class name.
     * @param keyValues key-value pairs to put as properties of the output-string
     * @param <T>       any object.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringifyMap(T obj, SortedMap<String, Object> keyValues) {
        StringBuilder stringBuilder = start(obj);
        appendMapValues(stringBuilder, keyValues);
        return finish(stringBuilder);
    }

    /**
     * Stringifies all fields of a given object.
     * <p>
     * In order to filter out certain fields please use either {@link #stringify(Object)} or
     * {@link #stringify(Object, String[])}.
     *
     * @param obj the object to stringify.
     * @param <T> any object.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringifyAll(T obj) {
        return stringifyWithFilter(obj, null, field -> !field.getName().startsWith("this$"));
    }

    private static void appendMapValues(StringBuilder stringBuilder, SortedMap<String, Object> keyValues) {
        final Iterator<Map.Entry<String, Object>> iterator = keyValues.entrySet().iterator();
        boolean firstRun = true;
        while (iterator.hasNext()) {
            if (!firstRun) {
                stringBuilder.append(';');
            }
            firstRun = false;
            final Map.Entry<String, Object> next = iterator.next();
            appendKeyValue(stringBuilder, next.getKey(), next.getValue());
        }
    }

    private static <T> String stringifyWithFilter(T obj,
                                                  @Nullable SortedMap<String, Object> keyValues,
                                                  Predicate<Field> filter) {
        StringBuilder stringBuilder = start(obj);

        try {
            appendToStringProperties(stringBuilder, obj, filter);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (keyValues != null && !keyValues.isEmpty()) {
            stringBuilder.append(';');
            appendMapValues(stringBuilder, keyValues);
        }

        return finish(stringBuilder);
    }

    private static <T> StringBuilder start(T obj) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(obj.getClass().getSimpleName());
        stringBuilder.append("(");
        return stringBuilder;
    }

    private static String finish(StringBuilder stringBuilder) {
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private static void appendToStringProperties(StringBuilder stringBuilder,
                                                 Object obj,
                                                 Predicate<Field> predicate) throws IllegalAccessException {
        Field[] fields = obj.getClass().getDeclaredFields();
        int insertedFields = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            if (!predicate.test(field)) {
                continue;
            }
            if (insertedFields++ > 0) {
                stringBuilder.append(';');
            }

            appendKeyValue(stringBuilder, field.getName(), field.get(obj));
        }
    }

    private static void appendKeyValue(StringBuilder stringBuilder, String key, @Nullable Object value) {
        stringBuilder.append(key);
        stringBuilder.append('=');
        stringBuilder.append(value); // null value is ok; will be converted to string "null"
    }
}
