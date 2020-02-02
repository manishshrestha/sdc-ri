package org.somda.sdc.biceps.model.participant;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class MdibVersionTest {

    @Test
    void accessFunctions() {
        var mdibVersion = MdibVersion.create();
        assertEquals(BigInteger.ZERO, mdibVersion.getInstanceId());
        assertEquals(BigInteger.ZERO, mdibVersion.getVersion());
        assertNotNull(mdibVersion.getSequenceId());
        assertFalse(mdibVersion.getSequenceId().toString().isEmpty());
    }

    @Test
    void equality() {
        {
            var mdibVersion = MdibVersion.create();
            assertNotEquals(null, mdibVersion);
        }
        {
            var lhs = new MdibVersion(URI.create("equal"));
            var rhs = new MdibVersion(URI.create("equal"));
            assertEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion(URI.create("equal"));
            var rhs = new MdibVersion(URI.create("unequal"));
            assertNotEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE);
            assertEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ZERO);
            assertNotEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            assertEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ZERO);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            assertNotEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ZERO, BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            assertNotEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("unequal"), BigInteger.ONE, BigInteger.ONE);
            assertNotEquals(lhs, rhs);
        }
    }

    @Test
    void comparison() {
        {
            var mdibVersion = MdibVersion.create();
            assertTrue(mdibVersion.compareTo(null).isEmpty());
        }
        {
            var lhs = new MdibVersion(URI.create("equal"));
            var rhs = new MdibVersion(URI.create("equal"));
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(0), lhs.compareTo(rhs).get());
        }
        {
            var lhs = new MdibVersion(URI.create("equal"));
            var rhs = new MdibVersion(URI.create("unequal"));
            assertTrue(lhs.compareTo(rhs).isEmpty());
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE);
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(0), lhs.compareTo(rhs).get());
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(0), lhs.compareTo(rhs).get());
        }

        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ZERO, BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(-1), lhs.compareTo(rhs).get());
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ZERO, BigInteger.ONE);
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(1), lhs.compareTo(rhs).get());
        }

        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ZERO);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(-1), lhs.compareTo(rhs).get());
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ZERO);
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(1), lhs.compareTo(rhs).get());
        }

        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ZERO);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ZERO, BigInteger.ONE);
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(-1), lhs.compareTo(rhs).get());
        }
        {
            var lhs = new MdibVersion(URI.create("equal"), BigInteger.ZERO, BigInteger.ONE);
            var rhs = new MdibVersion(URI.create("equal"), BigInteger.ONE, BigInteger.ZERO);
            assertTrue(lhs.compareTo(rhs).isPresent());
            assertEquals(Integer.valueOf(1), lhs.compareTo(rhs).get());
        }
    }
}