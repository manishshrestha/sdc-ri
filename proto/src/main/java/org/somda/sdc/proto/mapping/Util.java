package org.somda.sdc.proto.mapping;

import com.google.protobuf.Duration;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.StringMetricState;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

class Util {
    static <T> void doIfNotNull(@Nullable T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    static Duration fromJavaDuration(java.time.Duration duration) {
        var builder = Duration.newBuilder();
        builder.setSeconds(duration.getSeconds());
        builder.setNanos(duration.getNano());
        return builder.build();
    }

    static java.time.Duration fromProtoDuration(Duration duration) {
        return java.time.Duration.ofSeconds(duration.getSeconds(), duration.getNanos());
    }

    static UInt64Value toUInt64(@Nullable BigInteger number) {
        var builder = UInt64Value.newBuilder();
        if (number != null && number.longValue() >= 0) {
            builder.setValue(number.longValue());
        }
        return builder.build();
    }

    static Int64Value toInt64(@Nullable BigInteger number) {
        var builder = Int64Value.newBuilder();
        if (number != null) {
            builder.setValue(number.longValue());
        }
        return builder.build();
    }

    static Int32Value toInt32(@Nullable Integer number) {
        var builder = Int32Value.newBuilder();
        if (number != null) {
            builder.setValue(number);
        }
        return builder.build();
    }

    static UInt32Value toUInt32(@Nullable Long number) {
        var builder = UInt32Value.newBuilder();
        if (number != null && number >= 0) {
            builder.setValue(number.intValue());
        }
        return builder.build();
    }

    static UInt32Value toUInt32(@Nullable Integer number) {
        var builder = UInt32Value.newBuilder();
        if (number != null && number >= 0) {
            builder.setValue(number);
        }
        return builder.build();
    }

    static StringValue toStringValue(@Nullable String value) {
        var builder = StringValue.newBuilder();
        if (value != null) {
            builder.setValue(value);
        }
        return builder.build();
    }

    static Long optionalLongOfInt(Object protoMsg, String typeName) {
        var value = optionalProtoPrimitive(protoMsg, typeName, Integer.class);
        if (value == null) {
            return null;
        } else {
            return value.longValue();
        }
    }

    static Integer optionalIntOfInt(Object protoMsg, String typeName) {
        return Optional.ofNullable(optionalProtoPrimitive(protoMsg, typeName, Integer.class)).orElse(null);
    }

    static BigInteger optionalBigIntOfLong(Object protoMsg, String typeName) {
        return Optional.ofNullable(optionalProtoPrimitive(protoMsg, typeName, Long.class))
                .map(BigInteger::valueOf)
                .orElse(null);
    }

    static String optionalStr(Object protoMsg, String protoTypeName) {
        return optionalProtoPrimitive(protoMsg, protoTypeName, String.class);
    }

    static <T> T optional(Object protoMsg, String protoTypeName, Class<T> resultingPojoClass) {
        try {
            var hasValue = "has" + protoTypeName;
            var getValue = "get" + protoTypeName;
            var getValueMethod = protoMsg.getClass().getMethod(getValue);
            var hasValueMethod = protoMsg.getClass().getMethod(hasValue);
            if (!(Boolean) hasValueMethod.invoke(protoMsg)) {
                return null;
            }

            return resultingPojoClass.cast(getValueMethod.invoke(protoMsg));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    static <T> T mapToPojoEnum(Object protoMsg, String protoEnumName, Class<T> pojoEnumType) {
        try {
            var hasEnum = "has" + protoEnumName;
            var getEnum = "get" + protoEnumName;
            var getEnumMethod = protoMsg.getClass().getMethod(getEnum);
            var hasEnumMethod = protoMsg.getClass().getMethod(hasEnum);
            if (!(Boolean) hasEnumMethod.invoke(protoMsg)) {
                return null;
            }
            var enumContainer = getEnumMethod.invoke(protoMsg);
            var getEnumValueMethod = enumContainer.getClass().getMethod("getEnumValue");
            var protoEnumValue = getEnumValueMethod.invoke(enumContainer);
            var protoEnumNameMethod = protoEnumValue.getClass().getMethod("name");
            var valueOfMethod = pojoEnumType.getMethod("valueOf", String.class);
            var value = (String) protoEnumNameMethod.invoke(protoEnumValue);
            return pojoEnumType.cast(valueOfMethod.invoke(null, value));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    static <T, V> T mapToProtoEnum(@Nullable V pojoEnumValue, Class<T> protoEnumMsgType) {
        try {
            var protoEnumMsg = protoEnumMsgType.getMethod("newBuilder").invoke(null);
            var buildMethod = protoEnumMsg.getClass().getMethod("build");
            if (pojoEnumValue == null) {
                return protoEnumMsgType.cast(buildMethod.invoke(protoEnumMsg));
            }

            var nameMethod = pojoEnumValue.getClass().getMethod("name");
            var enumStringValue = (String) nameMethod.invoke(pojoEnumValue);
            var enumType = Arrays.stream(protoEnumMsgType.getClasses())
                    .filter(aClass -> aClass.getName().endsWith("EnumType"))
                    .findFirst().orElseThrow(() -> new RuntimeException("No type named EnumType was found"));

            var setValueMethod = protoEnumMsg.getClass().getMethod("setEnumValue", enumType);
            var valueOfMethod = enumType.getMethod("valueOf", String.class);
            setValueMethod.invoke(protoEnumMsg, valueOfMethod.invoke(null, enumStringValue));
            return protoEnumMsgType.cast(buildMethod.invoke(protoEnumMsg));
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException
                | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T optionalProtoPrimitive(Object protoMsg, String typeName, Class<T> resultClass) {
        return optionalProtoPrimitive(protoMsg, typeName, "getValue", resultClass);
    }

    private static <T> T optionalProtoPrimitive(Object protoMsg,
                                                String typeName,
                                                String primitiveGetterName,
                                                Class<T> resultClass) {
        try {
            var hasValue = "has" + typeName;
            var getValue = "get" + typeName;
            var getValueMethod = protoMsg.getClass().getMethod(getValue);
            var hasValueMethod = protoMsg.getClass().getMethod(hasValue);
            if (!(Boolean) hasValueMethod.invoke(protoMsg)) {
                return null;
            }

            var primitiveContainer = getValueMethod.invoke(protoMsg);
            var getPrimitiveValue = primitiveContainer.getClass().getMethod(primitiveGetterName);
            return resultClass.cast(getPrimitiveValue.invoke(primitiveContainer));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    static AbstractState invalidState() {
        var state = new AbstractState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    static AbstractDescriptor invalidDescriptor() {
        var descr = new AbstractDescriptor();
        descr.setHandle("[mapping failed]");
        return descr;
    }

    static AbstractMetricState invalidMetricState() {
        var state = new AbstractMetricState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    static StringMetricState invalidStringMetricState() {
        var state = new StringMetricState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }
}
