package org.ieee11073.sdc.common.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectStringifierTest {

    private TestClassDerived testObject;

    @BeforeEach
    void beforeEach() {
        String expectedStringBase = "foo";
        Integer expectedIntBase = 50;

        String expectedStringDerived = "bar";
        Integer expectedIntDerived = 100;


        testObject = new TestClassDerived(
                expectedStringDerived,
                expectedIntDerived,
                expectedStringBase,
                expectedIntBase);
    }


    @Test
    void stringifyByAnnotation() {
        {
            String expectedString = String.format("%s(anotherString=%s)",
                    TestClassDerived.class.getSimpleName(),
                    testObject.getAnotherString());

            assertEquals(expectedString, ObjectStringifier.stringify(testObject));
        }

        {
            testObject = new TestClassDerived(
                    null,
                    testObject.getAnotherInt(),
                    testObject.getaString(),
                    testObject.getAnInt());
            String expectedString = String.format("%s(anotherString=null)",
                    TestClassDerived.class.getSimpleName());

            assertEquals(expectedString, ObjectStringifier.stringify(testObject));
        }
    }


    @Test
    void stringifyFieldNames() {
        {
            String expectedString = String.format("%s(anotherInt=%s)",
                    TestClassDerived.class.getSimpleName(),
                    testObject.getAnotherInt());

            assertEquals(expectedString, ObjectStringifier.stringify(testObject, new String[]{"anotherInt"}));
        }

        {
            testObject = new TestClassDerived(
                    null,
                    testObject.getAnotherInt(),
                    testObject.getaString(),
                    testObject.getAnInt());
            String expectedString = String.format("%s(anotherString=null;anotherInt=%s)",
                    TestClassDerived.class.getSimpleName(),
                    testObject.getAnotherInt());

            assertEquals(expectedString, ObjectStringifier.stringify(testObject, new String[]{"anotherString", "anotherInt"}));
        }
    }

    @Test
    void stringifyAll() {
        String expectedString = String.format("%s(anotherString=%s;anotherInt=%s)",
                TestClassDerived.class.getSimpleName(),
                testObject.getAnotherString(),
                testObject.getAnotherInt());

        assertEquals(expectedString, ObjectStringifier.stringifyAll(testObject));

    }

    class TestClassBase {
        @Stringified
        private String aString;
        protected Integer anInt;

        public TestClassBase(String aString, Integer anInt) {
            this.aString = aString;
            this.anInt = anInt;
        }

        public String getaString() {
            return aString;
        }

        public Integer getAnInt() {
            return anInt;
        }
    }

    class TestClassDerived extends TestClassBase {
        @Stringified
        private String anotherString;
        private Integer anotherInt;

        public TestClassDerived(@Nullable String anotherString, Integer anotherInt, String aString, Integer anInt) {
            super(aString, anInt);
            this.anotherString = anotherString;
            this.anotherInt = anotherInt;
        }

        public String getAnotherString() {
            return anotherString;
        }

        public Integer getAnotherInt() {
            return anotherInt;
        }
    }
}