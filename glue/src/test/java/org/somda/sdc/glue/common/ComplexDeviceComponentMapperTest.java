package org.somda.sdc.glue.common;

import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexDeviceComponentMapperTest {

    @Test
    void fromComplexDeviceComponent() {
        {
            assertTrue(ComplexDeviceComponentMapper.fromComplexDeviceComponent(new MdsDescriptor()).isEmpty());
        }
        {
            final Optional<URI> actualUri = ComplexDeviceComponentMapper.fromComplexDeviceComponent(
                    createComponent(null, "bar", null));
            String expectedUri = "sdc.cdc.type://bar/";
            assertTrue(actualUri.isPresent());
            assertEquals(expectedUri, actualUri.get().toString());
        }
        {
            final Optional<URI> actualUri = ComplexDeviceComponentMapper.fromComplexDeviceComponent(
                    createComponent("foo", "bar", "fii"));
            String expectedUri = "sdc.cdc.type:/foo/bar/fii";
            assertTrue(actualUri.isPresent());
            assertEquals(expectedUri, actualUri.get().toString());
        }
    }

    @Test
    void fromCodedValue() {
        {
            var actualUri = ComplexDeviceComponentMapper.fromCodedValue(
                    CodedValueFactory.createCodedValue(null, "bar", null));
            String expectedUri = "sdc.cdc.type://bar/";
            assertEquals(expectedUri, actualUri.toString());
        }
        {
            var actualUri = ComplexDeviceComponentMapper.fromCodedValue(
                    CodedValueFactory.createCodedValue("foo", "bar", "fii"));
            String expectedUri = "sdc.cdc.type:/foo/bar/fii";
            assertEquals(expectedUri, actualUri.toString());
        }
    }

    @Test
    void fromUri() {
        {
            assertTrue(ComplexDeviceComponentMapper.fromUri(URI.create("sdc.BAD.SCHEME:/foo/bar/fii")).isEmpty());
        }
        {
            assertTrue(ComplexDeviceComponentMapper.fromUri(URI.create("sdc.cdc.type:/foo/bar")).isEmpty());
        }
        {
            Optional<CodedValue> actualCodedValue = ComplexDeviceComponentMapper.fromUri(URI.create("sdc.cdc.type:/foo/bar/fii"));
            assertTrue(actualCodedValue.isPresent());
            CodedValue expectedCodedValue = createCodedValue("foo", "bar", "fii");
            compare(expectedCodedValue, actualCodedValue.get());
        }
        {
            Optional<CodedValue> actualCodedValue = ComplexDeviceComponentMapper.fromUri(URI.create("sdc.cdc.type://bar/fii"));
            assertTrue(actualCodedValue.isPresent());
            CodedValue expectedCodedValue = createCodedValue(null, "bar", "fii");
            compare(expectedCodedValue, actualCodedValue.get());
        }
        {
            Optional<CodedValue> actualCodedValue = ComplexDeviceComponentMapper.fromUri(URI.create("sdc.cdc.type:///"));
            assertTrue(actualCodedValue.isPresent());
            CodedValue expectedCodedValue = createCodedValue(null, null, null);
            compare(expectedCodedValue, actualCodedValue.get());
        }
    }

    private MdsDescriptor createComponent(@Nullable String codingSystem, @Nullable String codingSystemVersion, @Nullable String code) {
        MdsDescriptor mdsDescriptor = new MdsDescriptor();
        mdsDescriptor.setType(createCodedValue(codingSystem, codingSystemVersion, code));
        return mdsDescriptor;
    }

    private CodedValue createCodedValue(@Nullable String codingSystem, @Nullable String codingSystemVersion, @Nullable String code) {
        CodedValue codedValue = new CodedValue();
        codedValue.setCodingSystem(codingSystem);
        codedValue.setCodingSystemVersion(codingSystemVersion);
        codedValue.setCode(code);
        return codedValue;
    }

    private void compare(CodedValue expectedCodedValue,
                         CodedValue actualCodedValue) {
        assertEquals(expectedCodedValue.getCodingSystem(), actualCodedValue.getCodingSystem());
        assertEquals(expectedCodedValue.getCodingSystemVersion(), actualCodedValue.getCodingSystemVersion());
        assertEquals(expectedCodedValue.getCode(), actualCodedValue.getCode());
    }
}