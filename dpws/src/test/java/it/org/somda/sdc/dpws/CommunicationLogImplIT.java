package it.org.somda.sdc.dpws;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSinkImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.http.grizzly.GrizzlyHttpHandlerBroker;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import javax.servlet.ServletOutputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@linkplain CommunicationLogImpl}.
 */
public class CommunicationLogImplIT extends DpwsTest {

    @Test
    void grizzly() throws IOException, MarshallingException, TransportException {

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

            assertTrue(Arrays.equals(inputContent, logOutputStream.toByteArray()));

            logOutputStream.reset();

            argument.getValue().write(outputContent);
            argument.getValue().flush();

            assertTrue(Arrays.equals(outputContent, logOutputStream.toByteArray()));
            assertTrue(Arrays.equals(outputContent, serviceOutputStream.toByteArray()));

        }
    }

    @Test
    void jetty() throws IOException, MarshallingException, TransportException {

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

            when(communicationLogSinkImplMock.createBranch(eq(CommunicationLogSink.BranchPath.HTTP), anyString()))
                    .thenReturn(logOutputStream);

            ArgumentCaptor<OutputStream> argument = ArgumentCaptor.forClass(OutputStream.class);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

            JettyHttpServerHandler jettyHttpServerHandler = new JettyHttpServerHandler(communicationLogImpl, "_",
                    httpHandlerMock);
            
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

            assertTrue(Arrays.equals(inputContent, logOutputStream.toByteArray()));

            logOutputStream.reset();

            argument.getValue().write(outputContent);
            argument.getValue().flush();

            assertTrue(Arrays.equals(outputContent, logOutputStream.toByteArray()));
            assertTrue(Arrays.equals(outputContent, serviceOutputStream.toByteArray()));

        }
    }
    
}
