package org.somda.sdc.glue.common.uri;

import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;

import javax.annotation.Nullable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ContextIdentificationMapperTest {

    @Test
    void fromInstanceIdentifier() throws UriMapperGenerationArgumentException {
        {
            String actualUri = ContextIdentificationMapper.fromInstanceIdentifier(createInstanceIdentifier(null, null),
                    ContextIdentificationMapper.ContextSource.Location);
            String expectedUri = "sdc.ctxt.loc:/biceps.uri.unk/";
            assertEquals(expectedUri, actualUri);
        }

        {
            String actualUri = ContextIdentificationMapper.fromInstanceIdentifier(createInstanceIdentifier("http://root", null),
                    ContextIdentificationMapper.ContextSource.Location);
            String expectedUri = "sdc.ctxt.loc:/http%3A%2F%2Froot/";
            assertEquals(expectedUri, actualUri);
        }

        {
            String actualUri = ContextIdentificationMapper.fromInstanceIdentifier(createInstanceIdentifier("http://root", "extension"),
                    ContextIdentificationMapper.ContextSource.Patient);
            String expectedUri = "sdc.ctxt.pat:/http%3A%2F%2Froot/extension";
            assertEquals(expectedUri, actualUri);
        }

        {
            String actualUri = ContextIdentificationMapper.fromInstanceIdentifier(createInstanceIdentifier("http://root", "ext/enÖsion?"),
                    ContextIdentificationMapper.ContextSource.Ensemble);
            String expectedUri = "sdc.ctxt.ens:/http%3A%2F%2Froot/ext%2Fen%C3%96sion%3F";
            assertEquals(expectedUri, actualUri);
        }
    }

    @Test
    void fromURI() throws UriMapperParsingException {
        {
            InstanceIdentifier actualInstanceIdentifier = ContextIdentificationMapper.fromString("sdc.ctxt.loc:/biceps.uri.unk/",
                    ContextIdentificationMapper.ContextSource.Location);
            InstanceIdentifier expectedInstanceIdentifier = createInstanceIdentifier(null, null);
            compare(expectedInstanceIdentifier, actualInstanceIdentifier);
        }

        {
            InstanceIdentifier actualInstanceIdentifier = ContextIdentificationMapper.fromString("sdc.ctxt.loc:/http%3A%2F%2Froot/",
                    ContextIdentificationMapper.ContextSource.Location);
            InstanceIdentifier expectedInstanceIdentifier = createInstanceIdentifier("http://root", null);
            compare(expectedInstanceIdentifier, actualInstanceIdentifier);
        }

        {
            InstanceIdentifier actualInstanceIdentifier = ContextIdentificationMapper.fromString("sdc.ctxt.pat:/http%3A%2F%2Froot/extension",
                    ContextIdentificationMapper.ContextSource.Patient);
            InstanceIdentifier expectedInstanceIdentifier = createInstanceIdentifier("http://root", "extension");
            compare(expectedInstanceIdentifier, actualInstanceIdentifier);
        }

        {
            InstanceIdentifier actualInstanceIdentifier = ContextIdentificationMapper.fromString("sdc.ctxt.ens:/http%3A%2F%2Froot/ext%2Fen%C3%96sion%3F",
                    ContextIdentificationMapper.ContextSource.Ensemble);
            InstanceIdentifier expectedInstanceIdentifier = createInstanceIdentifier("http://root", "ext/enÖsion?");
            compare(expectedInstanceIdentifier, actualInstanceIdentifier);
        }

        {
            assertThrows(UriMapperParsingException.class,
                    () -> ContextIdentificationMapper.fromString(
                            "sdc.ctxt.loc:/http%3A%2F%2Froot/ext%2Fen%C3%96sion%3F",
                            ContextIdentificationMapper.ContextSource.Patient));
        }

        {
            assertThrows(UriMapperParsingException.class,
                    () -> ContextIdentificationMapper.fromString(
                            "sdc.ctxt.loc:/http%3A%2F%2Froot//ext%2Fen%C3%96sion%3F",
                            ContextIdentificationMapper.ContextSource.Location));
        }
    }

    private InstanceIdentifier createInstanceIdentifier(@Nullable String root, @Nullable String extension) {
        InstanceIdentifier instanceIdentifier = new InstanceIdentifier();
        instanceIdentifier.setRootName(root);
        instanceIdentifier.setExtensionName(extension);
        return instanceIdentifier;
    }

    private void compare(InstanceIdentifier expectedInstanceIdentifier,
                         InstanceIdentifier actualInstanceIdentifier) {
        assertEquals(expectedInstanceIdentifier.getRootName(), actualInstanceIdentifier.getRootName());
        assertEquals(expectedInstanceIdentifier.getExtensionName(), actualInstanceIdentifier.getExtensionName());
    }
}