package org.somda.sdc.dpws.http.grizzly;

import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.CommunicationLogSinkImpl;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;

import java.io.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CommunicationLogImplUsage {

    @Test
    void streamUsage() throws IOException, MarshallingException, TransportException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);
        org.glassfish.grizzly.http.server.Request requestMock = mock(org.glassfish.grizzly.http.server.Request.class,
                RETURNS_SMART_NULLS);
        Response responseMock = mock(Response.class, RETURNS_SMART_NULLS);
        HttpHandler httpHandlerMock = mock(HttpHandler.class);

        byte[] inputContent = UUID.randomUUID().toString().getBytes();
        byte[] outputContent = UUID.randomUUID().toString().getBytes();

        try (ByteArrayOutputStream logOutputStream = new ByteArrayOutputStream();
             ByteArrayOutputStream serviceOutputStream = new ByteArrayOutputStream();
             ByteArrayInputStream serviceInputStream = new ByteArrayInputStream(inputContent);) {

            when(communicationLogSinkImplMock.createBranch(eq(CommunicationLogSink.BranchPath.HTTP), anyString()))
                    .thenReturn(logOutputStream);

            ArgumentCaptor<OutputStream> argument = ArgumentCaptor.forClass(OutputStream.class);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

            GrizzlyHttpHandlerBroker grizzlyHttpHandlerBroker = new GrizzlyHttpHandlerBroker(communicationLogImpl, "_",
                    httpHandlerMock, "/xyz/xyz");

            when(requestMock.getInputStream()).thenReturn(serviceInputStream);
            when(responseMock.getOutputStream()).thenReturn(serviceOutputStream);
            doNothing().when(responseMock).setStatus(any(HttpStatus.class));
            doNothing().when(responseMock).setContentType(anyString());

            grizzlyHttpHandlerBroker.service(requestMock, responseMock);

            verify(httpHandlerMock, times(1)).process(any(InputStream.class), argument.capture(),
                    any(TransportInfo.class));

            assertArrayEquals(inputContent, logOutputStream.toByteArray());

            logOutputStream.reset();

            argument.getValue().write(outputContent);
            argument.getValue().flush();

            assertArrayEquals(outputContent, logOutputStream.toByteArray());
            assertArrayEquals(outputContent, serviceOutputStream.toByteArray());

        }
    }

}
