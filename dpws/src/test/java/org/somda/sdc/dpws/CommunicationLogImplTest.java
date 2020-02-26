package org.somda.sdc.dpws;

import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.*;
import java.util.Collections;
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

            var requestCommContext = new CommunicationContext(
                    null,
                    new TransportInfo(
                        "",
                        null, null,
                        "_", 0,
                        Collections.emptyList()
                    )
            );

            InputStream resultingInputStream = communicationLogImpl
                    .logMessage(CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                            requestCommContext, inputTestInputStream);

            assertArrayEquals(resultingInputStream.readAllBytes(), content);
            assertArrayEquals(mockOutputStream.toByteArray(), content);

            mockOutputStream.reset();

            try(OutputStream resultingOutputStream = communicationLogImpl.logMessage(
                    CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                    requestCommContext, outputTestOutputStream);) {

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

        var requestCommContext = new CommunicationContext(
                null,
                new TransportInfo(
                        "",
                        null, null,
                        "_", 0,
                        Collections.emptyList()
                )
        );

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content);) {
            for (CommunicationLog.Direction dir : CommunicationLog.Direction.values()) {
                reset(communicationLogSinkImplMock);

                when(communicationLogSinkImplMock.getTargetStream(any(CommunicationLog.TransportType.class), anyString()))
                        .thenReturn(OutputStream.nullOutputStream());

                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, requestCommContext,
                        inputStream);
                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, requestCommContext,
                        OutputStream.nullOutputStream());
                verify(communicationLogSinkImplMock, times(2)).
                        getTargetStream(eq(CommunicationLog.TransportType.HTTP), anyString());
            }
        }
    }
}
