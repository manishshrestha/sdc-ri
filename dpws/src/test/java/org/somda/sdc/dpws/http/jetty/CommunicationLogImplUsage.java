package org.somda.sdc.dpws.http.jetty;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSinkImpl;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CommunicationLogImplUsage {

    @Test
    void streamUsage() throws IOException, MarshallingException, TransportException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);
        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class, RETURNS_SMART_NULLS);
        HttpServletResponse httpServletResponseMock = mock(HttpServletResponse.class, RETURNS_SMART_NULLS);
        org.eclipse.jetty.server.Request jettyServerRequestMock = mock(org.eclipse.jetty.server.Request.class,
                RETURNS_SMART_NULLS);
        HttpHandler httpHandlerMock = mock(HttpHandler.class);

        byte[] inputContent = UUID.randomUUID().toString().getBytes();
        byte[] outputContent = UUID.randomUUID().toString().getBytes();

        try (ByteArrayOutputStream logOutputStream = new ByteArrayOutputStream();
             ByteArrayOutputStream serviceOutputStream = new ByteArrayOutputStream();
             ByteArrayInputStream serviceInputStream = new ByteArrayInputStream(inputContent);
             ServletInputStream servletInputStream = mock(ServletInputStream.class);
             ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);) {

            when(communicationLogSinkImplMock.createBranch(eq(CommunicationLog.TransportType.HTTP), anyString()))
                    .thenReturn(logOutputStream);

            ArgumentCaptor<OutputStream> argument = ArgumentCaptor.forClass(OutputStream.class);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

            JettyHttpServerHandler jettyHttpServerHandler = new JettyHttpServerHandler("_",
                    httpHandlerMock, communicationLogImpl);

            doAnswer(i -> {
                return serviceInputStream.read(i.getArgument(0), i.getArgument(1), i.getArgument(2));
            }).when(servletInputStream).read(any(byte[].class), anyInt(), anyInt());

            doAnswer(i -> {
                serviceOutputStream.write(i.getArgument(0));
                return null;
            }).when(servletOutputStream).write(any(byte[].class));

            when(httpServletRequestMock.getInputStream()).thenReturn(servletInputStream);
            when(httpServletResponseMock.getOutputStream()).thenReturn(servletOutputStream);

            jettyHttpServerHandler.handle("_", jettyServerRequestMock, httpServletRequestMock, httpServletResponseMock);

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
