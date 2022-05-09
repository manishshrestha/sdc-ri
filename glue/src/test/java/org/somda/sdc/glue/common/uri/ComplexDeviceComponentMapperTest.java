package org.somda.sdc.glue.common.uri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import test.org.somda.common.LoggingTestWatcher;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(LoggingTestWatcher.class)
class ComplexDeviceComponentMapperTest {

    @Test
    void fromComplexDeviceComponent() throws UriMapperGenerationArgumentException {
        {
            assertThrows(
                    UriMapperGenerationArgumentException.class,
                    () -> ComplexDeviceComponentMapper.fromComplexDeviceComponent(new MdsDescriptor())
            );
        }
        {
            final String actualUri = ComplexDeviceComponentMapper.fromComplexDeviceComponent(
                    createComponent(null, "bar", "1"));
            String expectedUri = "sdc.cdc.type://bar/1";
            assertEquals(expectedUri, actualUri);
        }
        {
            final String actualUri = ComplexDeviceComponentMapper.fromComplexDeviceComponent(
                    createComponent("foo", "bar", "fii"));
            String expectedUri = "sdc.cdc.type:/foo/bar/fii";
            assertEquals(expectedUri, actualUri);
        }
        {
            final String actualUri = ComplexDeviceComponentMapper.fromComplexDeviceComponent(
                    createComponent("foo%2F%2F", "bar", "fii"));
            String expectedUri = "sdc.cdc.type:/foo%252F%252F/bar/fii";
            assertEquals(expectedUri, actualUri);
        }
        {
            assertThrows(UriMapperGenerationArgumentException.class,
                    () -> ComplexDeviceComponentMapper.fromComplexDeviceComponent(
                    createComponent("foo%2F%2F", "bar", null)));
        }
        {
            final String actualUri = ComplexDeviceComponentMapper.fromComplexDeviceComponent(
                    createComponent("", "@:NoPort", "1"));
            String expectedUri = "sdc.cdc.type://%40%3ANoPort/1";
            assertEquals(expectedUri, actualUri);
        }
    }

    @Test
    void fromCodedValue() throws UriMapperGenerationArgumentException {
        {
            String actualUri = ComplexDeviceComponentMapper.fromCodedValue(
                    CodedValueFactory.createCodedValue(null, "bar", "1"));
            String expectedUri = "sdc.cdc.type://bar/1";
            assertEquals(expectedUri, actualUri);
        }
        {
            String actualUri = ComplexDeviceComponentMapper.fromCodedValue(
                    CodedValueFactory.createCodedValue(null, "@:", "1"));
            String expectedUri = "sdc.cdc.type://%40%3A/1";
            assertEquals(expectedUri, actualUri);
        }
        {
            String actualUri = ComplexDeviceComponentMapper.fromCodedValue(
                    CodedValueFactory.createCodedValue("foo", "bar", "fii"));
            String expectedUri = "sdc.cdc.type:/foo/bar/fii";
            assertEquals(expectedUri, actualUri);
        }
    }

    @Test
    void fromUri() throws UriMapperParsingException {
        {
            assertThrows(UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri(""));
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.BAD.SCHEME:/foo/bar/fii"));
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type:/foo/bar"));
        }
        {
            assertThrows(
                    UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type:/foo/bar/"));
        }
        {
            assertThrows(
                    UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type:///"));
        }
        {
            assertThrows(
                    UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type://@@/1"));
        }
        {
            assertThrows(
                    UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type://@host@/1"));
        }
        {
            assertThrows(
                    UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type://@host:NoPort/1"));
        }
        {
            assertThrows(
                    UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type://@@host/1"));
        }
        {
            assertThrows(
                    UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type:/c///1"));
        }
        {
            assertThrows(
                    UriMapperParsingException.class,
                    () -> ComplexDeviceComponentMapper.fromUri("sdc.cdc.type://user@user@:1/2"));
        }
        {
            CodedValue actualCodedValue = ComplexDeviceComponentMapper.fromUri("sdc.cdc.type://@host:/1");
            CodedValue expectedCodedValue = createCodedValue(null, "@host:", "1");
            compare(expectedCodedValue, actualCodedValue);
        }
        {
            CodedValue actualCodedValue = ComplexDeviceComponentMapper.fromUri("sdc.cdc.type://:@host/1");
            CodedValue expectedCodedValue = createCodedValue(null, ":@host", "1");
            compare(expectedCodedValue, actualCodedValue);
        }
        {
            CodedValue actualCodedValue = ComplexDeviceComponentMapper.fromUri("sdc.cdc.type:/foo/bar/fii");
            CodedValue expectedCodedValue = createCodedValue("foo", "bar", "fii");
            compare(expectedCodedValue, actualCodedValue);
        }
        {
            CodedValue actualCodedValue = ComplexDeviceComponentMapper.fromUri("sdc.cdc.type://bar/fii");
            CodedValue expectedCodedValue = createCodedValue(null, "bar", "fii");
            compare(expectedCodedValue, actualCodedValue);
        }
        {
            CodedValue actualCodedValue = ComplexDeviceComponentMapper.fromUri("sdc.cdc.type:///1");
            CodedValue expectedCodedValue = createCodedValue(null, null, "1");
            compare(expectedCodedValue, actualCodedValue);
        }
    }

    private MdsDescriptor createComponent(@Nullable String codingSystem, @Nullable String codingSystemVersion, @Nullable String code) {
        return MdsDescriptor.builder()
            .withType(createCodedValue(codingSystem, codingSystemVersion, code))
            .build();
    }

    private CodedValue createCodedValue(@Nullable String codingSystem, @Nullable String codingSystemVersion, @Nullable String code) {
        return CodedValue.builder()
            .withCodingSystem(codingSystem)
            .withCodingSystemVersion(codingSystemVersion)
            .withCode(code)
            .build();
    }

    private void compare(CodedValue expectedCodedValue,
                         CodedValue actualCodedValue) {
        assertEquals(expectedCodedValue.getCodingSystem(), actualCodedValue.getCodingSystem());
        assertEquals(expectedCodedValue.getCodingSystemVersion(), actualCodedValue.getCodingSystemVersion());
        assertEquals(expectedCodedValue.getCode(), actualCodedValue.getCode());
    }
}