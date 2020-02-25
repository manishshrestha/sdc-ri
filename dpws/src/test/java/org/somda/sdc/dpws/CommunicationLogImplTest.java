package org.somda.sdc.dpws;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class CommunicationLogImplTest extends DpwsTest {

    @Test
    void content() throws IOException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        byte[] content = UUID.randomUUID().toString().getBytes();

        try (ByteArrayOutputStream mockOutputStream = new ByteArrayOutputStream();
             ByteArrayInputStream inputTestInputStream = new ByteArrayInputStream(content);
             ByteArrayOutputStream outputTestOutputStream = new ByteArrayOutputStream();) {

            when(communicationLogSinkImplMock.getTargetStream(eq(CommunicationLog.TransportType.HTTP), anyString()))
                    .thenReturn(mockOutputStream);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

            InputStream resultingInputStream = communicationLogImpl
                    .logMessage(CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                            "_", 0, inputTestInputStream);

            assertArrayEquals(resultingInputStream.readAllBytes(), content);
            assertArrayEquals(mockOutputStream.toByteArray(), content);

            mockOutputStream.reset();

            try(OutputStream resultingOutputStream = communicationLogImpl.logMessage(
                    CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                    "_", 0, outputTestOutputStream);) {

                resultingOutputStream.write(content);
                resultingOutputStream.flush();
            }

            assertArrayEquals(outputTestOutputStream.toByteArray(), content);
            assertArrayEquals(mockOutputStream.toByteArray(), content);
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

                when(communicationLogSinkImplMock.getTargetStream(any(CommunicationLog.TransportType.class), anyString()))
                        .thenReturn(OutputStream.nullOutputStream());

                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, "_", 0,
                        inputStream);
                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, "_", 0,
                        OutputStream.nullOutputStream());
                verify(communicationLogSinkImplMock, times(2)).
                        getTargetStream(eq(CommunicationLog.TransportType.HTTP), anyString());
            }
        }
    }
}
