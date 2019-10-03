package org.ieee11073.sdc.common.util;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Predicate;

/**
 * Utility to stringifyMap objects in conformance with the SDCri coding conventions.
 */
public class ObjectStringifier {
    /**
     * Stringifies annotated fields of a given object.
     * <p>
     * This function considers every field that is annotated with {@link Stringified}.
     *
     * @param obj the object to stringifyMap.
     * @param <T> any object.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringify(T obj) {
        return stringifyWithFilter(obj, null, field -> field.isAnnotationPresent(Stringified.class));
    }

    /**
     * Stringifies annotated fields of a given object and appends defined map.
     *
     * @param obj       the object to stringifyMap.
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
     * @param obj the object to stringifyMap.
     * @param <T> any object.
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
     * @param obj       the object to stringifyMap.
     * @param keyValues key-values to be additionally appended.
     * @param <T>       any object.
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
     * @return
     */
    public static <T> String stringifyMap(T obj, SortedMap<String, Object> keyValues) {
        StringBuffer stringBuffer = start(obj);
        appendMapValues(stringBuffer, keyValues);
        return finish(stringBuffer);
    }

    /**
     * Stringifies all fields of a given object.
     * <p>
     * In order to filter out certain fields please use either {@link #stringify(Object)} or
     * {@link #stringify(Object, String[])}.
     *
     * @param obj the object to stringifyMap.
     * @param <T> any object.
     * @return the stringified object in conformance with the SDCri coding conventions.
     */
    public static <T> String stringifyAll(T obj) {
        return stringifyWithFilter(obj, null, field -> !field.getName().startsWith("this$"));
    }

    private static <T> void appendMapValues(StringBuffer stringBuffer, SortedMap<String, Object> keyValues) {
        final Iterator<Map.Entry<String, Object>> iterator = keyValues.entrySet().iterator();
        boolean firstRun = true;
        while (iterator.hasNext()) {
            if (!firstRun) {
                stringBuffer.append(';');
            }
            firstRun = false;
            final Map.Entry<String, Object> next = iterator.next();
            appendKeyValue(stringBuffer, next.getKey(), next.getValue());
        }
    }

    private static <T> String stringifyWithFilter(T obj, @Nullable SortedMap<String, Object> keyValues, Predicate<Field> filter) {
        StringBuffer stringBuffer = start(obj);

        try {
            appendToStringProperties(stringBuffer, obj, filter);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (keyValues != null && !keyValues.isEmpty()) {
            stringBuffer.append(';');
            appendMapValues(stringBuffer, keyValues);
        }

        return finish(stringBuffer);
    }

    private static <T> StringBuffer start(T obj) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(obj.getClass().getSimpleName());
        stringBuffer.append("(");
        return stringBuffer;
    }

    private static String finish(StringBuffer stringBuffer) {
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    private static void appendToStringProperties(StringBuffer stringBuffer,
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
                stringBuffer.append(';');
            }

            appendKeyValue(stringBuffer, field.getName(), field.get(obj));
        }
    }

    private static void appendKeyValue(StringBuffer stringBuffer, String key, @Nullable Object value) {
        stringBuffer.append(key);
        stringBuffer.append('=');
        stringBuffer.append(value); // null value is ok; will be converted to string "null"
    }
}
