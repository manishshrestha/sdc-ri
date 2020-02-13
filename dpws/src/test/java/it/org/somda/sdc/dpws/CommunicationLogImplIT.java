package it.org.somda.sdc.dpws;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSinkImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.http.grizzly.GrizzlyHttpHandlerBroker;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;


import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.is;

/**
 *  Tests {@linkplain CommunicationLogImpl}.
 */
public class CommunicationLogImplIT extends DpwsTest {

    @Test
    void grizzly() throws IOException, MarshallingException, TransportException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);
        Request requestMock = mock(Request.class,  RETURNS_SMART_NULLS);
        Response responseMock = mock(Response.class,  RETURNS_SMART_NULLS);
        HttpHandler httpHandlerMock = mock(HttpHandler.class);

        byte[] contentInputStream = UUID.randomUUID().toString().getBytes();
        byte[] contentOutputStream = UUID.randomUUID().toString().getBytes();

        try (ByteArrayOutputStream logOutputStream = new ByteArrayOutputStream();
                ByteArrayOutputStream serviceOutputStream = new ByteArrayOutputStream();
                ByteArrayInputStream serviceInputStream = new ByteArrayInputStream(contentInputStream);) {

            when(communicationLogSinkImplMock.createBranch(eq(CommunicationLogSink.BranchPath.HTTP), anyString()))
                    .thenReturn(logOutputStream);
            
            ArgumentCaptor<OutputStream> argument = ArgumentCaptor.forClass(OutputStream.class);
            
            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);
            
            GrizzlyHttpHandlerBroker grizzlyHttpHandlerBroker =  new GrizzlyHttpHandlerBroker(communicationLogImpl, "_", httpHandlerMock, "/xyz/xyz");
            
            when(requestMock.getInputStream()).thenReturn(serviceInputStream);
            when(responseMock.getOutputStream()).thenReturn(serviceOutputStream);
            doNothing().when(responseMock).setStatus(any(HttpStatus.class));
            doNothing().when(responseMock).setContentType(anyString());
            
            grizzlyHttpHandlerBroker.service(requestMock, responseMock);
            
            verify(httpHandlerMock, times(1)).process(any(InputStream.class), argument.capture(),  any(TransportInfo.class));
            
            assertEquals(contentInputStream, logOutputStream.toByteArray());
            
            logOutputStream.reset();
            
            argument.getValue().write(contentOutputStream);
            argument.getValue().flush();
            
            assertEquals(contentOutputStream, logOutputStream.toByteArray());
            
        }
    }
}
