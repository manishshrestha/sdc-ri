package org.somda.sdc.dpws;

import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunicationLogImplTest extends DpwsTest {

    @Test
    void content() throws IOException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        byte[] content = UUID.randomUUID().toString().getBytes();

        try (ByteArrayOutputStream mockAppLevelOutputStream = spy(new ByteArrayOutputStream());
             ByteArrayOutputStream mockNetLevelOutputStream = spy(new ByteArrayOutputStream());
             ByteArrayInputStream inputTestInputStream = new ByteArrayInputStream(content);
             ByteArrayOutputStream outputTestOutputStream = new ByteArrayOutputStream()) {

            when(communicationLogSinkImplMock.createTargetStream(eq(CommunicationLog.TransportType.HTTP), any(), any(), any(), eq(CommunicationLog.Level.APPLICATION)))
                    .thenReturn(mockAppLevelOutputStream);
            when(communicationLogSinkImplMock.createTargetStream(eq(CommunicationLog.TransportType.HTTP), any(), any(), any(), eq(CommunicationLog.Level.NETWORK)))
                .thenReturn(mockNetLevelOutputStream);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock, "abcd");

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
                            CommunicationLog.MessageType.UNKNOWN,
                            requestCommContext, CommunicationLog.Level.APPLICATION, inputTestInputStream);

            assertArrayEquals(resultingInputStream.readAllBytes(), content);
            assertArrayEquals(mockAppLevelOutputStream.toByteArray(), content);
            verify(mockAppLevelOutputStream, times(1)).close();

            mockAppLevelOutputStream.reset();

            try (OutputStream resultingOutputStream = communicationLogImpl.logMessage(
                    CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                    CommunicationLog.MessageType.UNKNOWN,
                    requestCommContext, CommunicationLog.Level.APPLICATION, outputTestOutputStream)) {

                resultingOutputStream.write(content);
                resultingOutputStream.flush();
            }

            assertArrayEquals(outputTestOutputStream.toByteArray(), content);
            assertArrayEquals(mockAppLevelOutputStream.toByteArray(), content);
        }
    }

    @Test
    void branchPath() throws IOException {

        byte[] content = UUID.randomUUID().toString().getBytes();

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock, "abcd");

        var requestCommContext = new CommunicationContext(
                null,
                new TransportInfo(
                        "",
                        null, null,
                        "_", 0,
                        Collections.emptyList()
                )
        );

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            for (CommunicationLog.Direction dir : CommunicationLog.Direction.values()) {
                reset(communicationLogSinkImplMock);

                when(communicationLogSinkImplMock.createTargetStream(any(CommunicationLog.TransportType.class), any(), any(), any(), any()))
                        .thenReturn(OutputStream.nullOutputStream());

                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, CommunicationLog.MessageType.UNKNOWN,
                        requestCommContext, CommunicationLog.Level.APPLICATION,
                        inputStream);
                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, CommunicationLog.MessageType.UNKNOWN,
                        requestCommContext, CommunicationLog.Level.APPLICATION,
                        OutputStream.nullOutputStream());
                verify(communicationLogSinkImplMock, times(2)).
                        createTargetStream(eq(CommunicationLog.TransportType.HTTP), any(), any(), any(), eq(CommunicationLog.Level.APPLICATION));
            }
        }
    }

    // TODO: test Network Level Messages as well.
}
