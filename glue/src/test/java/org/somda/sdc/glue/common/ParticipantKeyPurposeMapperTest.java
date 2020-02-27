package org.somda.sdc.glue.common;

import org.ietf.jgss.Oid;
import org.junit.jupiter.api.Test;
import org.somda.sdc.glue.common.uri.ParticipantKeyPurposeMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticipantKeyPurposeMapperTest {
    @Test
    void fromOid() throws Exception {
        {
            String actualUri = ParticipantKeyPurposeMapper.fromOid(new Oid("1.2.840.10004.20701.1.1"));
            String expectedUri = "sdc.mds.pkp:1.2.840.10004.20701.1.1";
            assertEquals(expectedUri, actualUri);
        }
        {
            String actualUri = ParticipantKeyPurposeMapper.fromOid(new Oid("1.3.6.1.4.1"));
            String expectedUri = "sdc.mds.pkp:1.3.6.1.4.1";
            assertEquals(expectedUri, actualUri);
        }
    }

//    @Test
//    void fromUri() {
//        {
//            Optional<Oid> actualOid = ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp:1.2.840.10004.20701.1.1");
//            String expectedOid = "1.2.840.10004.20701.1.1";
//            assertTrue(actualOid.isPresent());
//            assertEquals(expectedOid, actualOid.get().toString());
//        }
//        {
//            Optional<Oid> actualOid = ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp:1.3.6.1.4.1");
//            String expectedOid = "1.3.6.1.4.1";
//            assertTrue(actualOid.isPresent());
//            assertEquals(expectedOid, actualOid.get().toString());
//        }
//        {
//            // Unexpected scheme
//            Optional<Oid> actualOid = ParticipantKeyPurposeMapper.fromString("sdc.pkp:1.3.6.1.4.1");
//            assertTrue(actualOid.isEmpty());
//        }
//        {
//            // Malformed OID
//            Optional<Oid> actualOid = ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp:3434.3.3434.34.555.1");
//            assertTrue(actualOid.isEmpty());
//        }
//    }
}