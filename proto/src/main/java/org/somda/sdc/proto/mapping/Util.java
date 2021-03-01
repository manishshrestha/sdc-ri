package org.somda.sdc.proto.mapping;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.model.biceps.HandleRefMsg;
import org.somda.sdc.proto.model.biceps.MdsOperatingModeMsg;
import org.somda.sdc.proto.model.biceps.ReferencedVersionMsg;
import org.somda.sdc.proto.model.biceps.TimestampMsg;
import org.somda.sdc.proto.model.biceps.VersionCounterMsg;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public class Util {
    public static <T> void doIfNotNull(@Nullable T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public static Duration fromJavaDuration(java.time.Duration duration) {
        var builder = Duration.newBuilder();
        builder.setSeconds(duration.getSeconds());
        builder.setNanos(duration.getNano());
        return builder.build();
    }

    public static java.time.Duration fromProtoDuration(Duration duration) {
        return java.time.Duration.ofSeconds(duration.getSeconds(), duration.getNanos());
    }

    public static UInt64Value toUInt64(@Nullable BigInteger number) {
        var builder = UInt64Value.newBuilder();
        if (number != null && number.longValue() >= 0) {
            builder.setValue(number.longValue());
        }
        return builder.build();
    }

    public static Int64Value toInt64(@Nullable BigInteger number) {
        var builder = Int64Value.newBuilder();
        if (number != null) {
            builder.setValue(number.longValue());
        }
        return builder.build();
    }

    public static Int64Value toInt64(@Nullable Long number) {
        var builder = Int64Value.newBuilder();
        if (number != null) {
            builder.setValue(number);
        }
        return builder.build();
    }

    public static Int32Value toInt32(@Nullable Integer number) {
        var builder = Int32Value.newBuilder();
        if (number != null) {
            builder.setValue(number);
        }
        return builder.build();
    }

    public static UInt32Value toUInt32(@Nullable Long number) {
        var builder = UInt32Value.newBuilder();
        if (number != null && number >= 0) {
            builder.setValue(number.intValue());
        }
        return builder.build();
    }

    public static UInt32Value toUInt32(@Nullable Integer number) {
        var builder = UInt32Value.newBuilder();
        if (number != null && number >= 0) {
            builder.setValue(number);
        }
        return builder.build();
    }

    public static StringValue toStringValue(@Nullable String value) {
        var builder = StringValue.newBuilder();
        if (value != null) {
            builder.setValue(value);
        }
        return builder.build();
    }

    public static BoolValue toBoolValue(@Nullable Boolean value) {
        var builder = BoolValue.newBuilder();
        if (value != null) {
            builder.setValue(value);
        }
        return builder.build();
    }

    public static Long optionalLongOfInt(Object protoMsg, String typeName) {
        var value = optionalProtoPrimitive(protoMsg, typeName, Integer.class);
        if (value == null) {
            return null;
        } else {
            return value.longValue();
        }
    }

    public static Integer optionalIntOfInt(Object protoMsg, String typeName) {
        return Optional.ofNullable(optionalProtoPrimitive(protoMsg, typeName, Integer.class)).orElse(null);
    }

    public static BigInteger optionalBigIntOfLong(Object protoMsg, String typeName) {
        return Optional.ofNullable(optionalProtoPrimitive(protoMsg, typeName, Long.class))
                .map(BigInteger::valueOf)
                .orElse(null);
    }

    public static BigDecimal optionalBigDecimalOfString(Object protoMsg, String typeName) {
        return Optional.ofNullable(optionalProtoPrimitive(protoMsg, typeName, String.class))
                .map(BigDecimal::new)
                .orElse(null);
    }

    public static String optionalStr(Object protoMsg, String protoTypeName) {
        return optionalProtoPrimitive(protoMsg, protoTypeName, String.class);
    }

    public static <T> T optional(Object protoMsg, String protoTypeName, Class<T> resultingPojoClass) {
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

    public static Instant optionalTimestamp(Object protoMsg, String protoTypeName, TimestampAdapter timestampAdapter) {
        var timestampMsg = Util.optional(protoMsg, protoTypeName, TimestampMsg.class);
        if (timestampMsg != null) {
            return timestampAdapter.unmarshal(BigInteger.valueOf(timestampMsg.getUnsignedLong()));
        }
        return null;
    }

    public static BigInteger optionalReferencedVersion(Object protoMsg, String protoTypeName) {
        var version = Util.optional(protoMsg, protoTypeName, ReferencedVersionMsg.class);
        if (version != null) {
            return BigInteger.valueOf(version.getVersionCounter().getUnsignedLong());
        }
        return null;
    }

    public static BigInteger optionalVersionCounter(Object protoMsg, String protoTypeName) {
        var version = Util.optional(protoMsg, protoTypeName, VersionCounterMsg.class);
        if (version != null) {
            return BigInteger.valueOf(version.getUnsignedLong());
        }
        return null;
    }

    public static String optionalHandleRef(Object protoMsg, String protoTypeName) {
        var handleRef = Util.optional(protoMsg, protoTypeName, HandleRefMsg.class);
        if (handleRef != null) {
            return handleRef.getString();
        }
        return null;
    }

    public static <T> T mapToPojoEnum(Object protoMsg, String protoEnumName, Class<T> pojoEnumType) {
        try {
            var hasEnum = "has" + protoEnumName;
            var getEnum = "get" + protoEnumName;
            var getEnumMethod = protoMsg.getClass().getMethod(getEnum);
            var hasEnumMethod = protoMsg.getClass().getMethod(hasEnum);
            if (!(Boolean) hasEnumMethod.invoke(protoMsg)) {
                return null;
            }
            var enumContainer = getEnumMethod.invoke(protoMsg);
            var getEnumValueMethod = enumContainer.getClass().getMethod("getEnumType");
            var protoEnumValue = getEnumValueMethod.invoke(enumContainer);
            var protoEnumNameMethod = protoEnumValue.getClass().getMethod("name");
            var valueOfMethod = pojoEnumType.getMethod("valueOf", String.class);
            var value = (String) protoEnumNameMethod.invoke(protoEnumValue);
            return pojoEnumType.cast(valueOfMethod.invoke(null, value));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T, V> T mapToProtoEnum(@Nullable V pojoEnumValue, Class<T> protoEnumMsgType) {
        try {
            var protoEnumMsg = protoEnumMsgType.getMethod("newBuilder").invoke(null);
            var buildMethod = protoEnumMsg.getClass().getMethod("build");
            if (pojoEnumValue == null) {
                return protoEnumMsgType.cast(buildMethod.invoke(protoEnumMsg));
            }

            var nameMethod = pojoEnumValue.getClass().getMethod("name");
            var enumStringValue = (String) nameMethod.invoke(pojoEnumValue);
            var enumType = Arrays.stream(protoEnumMsgType.getDeclaredClasses())
                    .filter(aClass -> aClass.getName().endsWith("EnumType"))
                    .findFirst().orElseThrow(() -> new RuntimeException("No type named EnumType was found"));

            var setValueMethod = protoEnumMsg.getClass().getMethod("setEnumType", enumType);
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

    public static AbstractState invalidState() {
        var state = new AbstractState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    public static AbstractDescriptor invalidDescriptor() {
        var descr = new AbstractDescriptor();
        descr.setHandle("[mapping failed]");
        return descr;
    }

    public static AbstractMetricState invalidMetricState() {
        var state = new AbstractMetricState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    public static StringMetricState invalidStringMetricState() {
        var state = new StringMetricState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    public static AbstractAlertState invalidAlertState() {
        var state = new AbstractAlertState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    public static AbstractAlertDescriptor invalidAlertDescriptor() {
        var descriptor = new AbstractAlertDescriptor();
        descriptor.setHandle("[mapping failed]");
        return descriptor;
    }

    public static AbstractOperationDescriptor invalidOperationDescriptor() {
        var descriptor = new AbstractOperationDescriptor();
        descriptor.setHandle("[mapping failed]");
        return descriptor;
    }

    public static AbstractSetStateOperationDescriptor invalidSetStateOperationDescriptor() {
        var descriptor = new AbstractSetStateOperationDescriptor();
        descriptor.setHandle("[mapping failed]");
        return descriptor;
    }

    public static AbstractOperationState invalidOperationState() {
        var state = new AbstractOperationState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    public static AbstractContextState invalidContextState() {
        var state = new AbstractContextState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    public static AbstractDeviceComponentState invalidDeviceComponentState() {
        var state = new AbstractDeviceComponentState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    public static PatientDemographicsCoreData invalidPatientDemographicsCoreData() {
        return new PatientDemographicsCoreData();
    }

    public static PersonReference invalidPersonReference() {
        return new PersonReference();
    }

    public static InstanceIdentifier invalidInstanceIdentifier() {
        return new InstanceIdentifier();
    }

    public static BaseDemographics invalidBaseDemographics() {
        return new BaseDemographics();
    }
}
