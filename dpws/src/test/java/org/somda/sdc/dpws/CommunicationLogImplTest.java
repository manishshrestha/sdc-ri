package org.somda.sdc.dpws;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@linkplain CommunicationLogImpl}.
 */
public class CommunicationLogImplTest extends DpwsTest {

    @Test
    void content() throws IOException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        byte[] content = UUID.randomUUID().toString().getBytes();

        try (ByteArrayOutputStream mockOutputStream = new ByteArrayOutputStream();
             ByteArrayInputStream inputTestInputStream = new ByteArrayInputStream(content);
             ByteArrayOutputStream outputTestOutputStream = new ByteArrayOutputStream();) {

            when(communicationLogSinkImplMock.createBranch(eq(CommunicationLog.TransportType.HTTP), anyString()))
                    .thenReturn(mockOutputStream);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

            InputStream resultingInputStream = communicationLogImpl
                    .logMessage(CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                            "_", 0, inputTestInputStream);

            assertThat(resultingInputStream.readAllBytes(), is(content));
            assertThat(mockOutputStream.toByteArray(), is(content));

            mockOutputStream.reset();

            OutputStream resultingOutputStream = communicationLogImpl.logMessage(
                    CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                    "_", 0, outputTestOutputStream);

            resultingOutputStream.write(content);
            resultingOutputStream.flush();

            assertThat(outputTestOutputStream.toByteArray(), is(content));
            assertThat(mockOutputStream.toByteArray(), is(content));
        }
    }

    @Test
    void branchPath() throws IOException {

        byte[] content = UUID.randomUUID().toString().getBytes();

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content);) {
            for (CommunicationLog.Direction dir : CommunicationLog.Direction.values()) {
                reset(communicationLogSinkImplMock);

                when(communicationLogSinkImplMock.createBranch(any(CommunicationLog.TransportType.class), anyString()))
                        .thenReturn(OutputStream.nullOutputStream());

                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, "_", 0,
                        inputStream);
                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, "_", 0,
                        OutputStream.nullOutputStream());
                verify(communicationLogSinkImplMock, times(2)).
                        createBranch(eq(CommunicationLog.TransportType.HTTP), anyString());
            }
        }
    }
}
