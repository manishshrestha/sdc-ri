package org.somda.sdc.glue.common.uri;

import org.ietf.jgss.Oid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void fromUri() throws UriMapperParsingException {
        {
            Oid actualOid = ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp:1.2.840.10004.20701.1.1");
            String expectedOid = "1.2.840.10004.20701.1.1";
            assertEquals(expectedOid, actualOid.toString());
        }
        {
            Oid actualOid = ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp:1.3.6.1.4.1");
            String expectedOid = "1.3.6.1.4.1";
            assertEquals(expectedOid, actualOid.toString());
        }
        {
            // Unexpected scheme
            assertThrows(UriMapperParsingException.class,
                    () -> ParticipantKeyPurposeMapper.fromString("sdc.pkp:1.3.6.1.4.1"));
        }
        {
            // Malformed OID
            assertThrows(UriMapperParsingException.class,
                    () -> ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp:3434.3.3434.34.555.1"));
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp://1.3.6.1.4.1"));
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp:/1.3.6.1.4.1"));
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp::1.3.6.1.4.1"));
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> ParticipantKeyPurposeMapper.fromString("sdc.mds.pkp::1.3.6.1.4.1?fac=1"));
        }
    }
}