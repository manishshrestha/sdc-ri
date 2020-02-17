package org.somda.sdc.dpws.http.apache;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.CommunicationLogSinkImpl;
import org.somda.sdc.dpws.TransportBindingException;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.model.Envelope;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CommunicationLogImplUsage {

    @Test
    void streamUsage() throws IOException, TransportBindingException, SoapFaultException, JAXBException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        HttpClient httpClient = mock(HttpClient.class);
        SoapMarshalling soapMarshallingMock = mock(SoapMarshalling.class);
        SoapUtil soapUtilMock = mock(SoapUtil.class, RETURNS_MOCKS);
        SoapMessage soapMessageMock = mock(SoapMessage.class);
        Envelope envelopeMock = mock(Envelope.class);
        HttpResponse httpResponseMock = mock(HttpResponse.class, RETURNS_MOCKS);
        HttpEntity httpEntityMock = mock(HttpEntity.class);

        byte[] inputContent = UUID.randomUUID().toString().getBytes();
        byte[] outputContent = UUID.randomUUID().toString().getBytes();

        when(soapMessageMock.getEnvelopeWithMappedHeaders()).thenReturn(envelopeMock);

        try (ByteArrayOutputStream logOutputStream = new ByteArrayOutputStream();
             ByteArrayInputStream serviceInputStream = new ByteArrayInputStream(inputContent);) {

            when(httpEntityMock.getContent()).thenReturn(serviceInputStream);
            when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
            when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponseMock);

            when(communicationLogSinkImplMock.createBranch(eq(CommunicationLogSink.BranchPath.HTTP), anyString()))
                    .thenReturn(logOutputStream);

            ArgumentCaptor<OutputStream> argument = ArgumentCaptor.forClass(OutputStream.class);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

            try (ClientTransportBinding clientTransportBinding = new ClientTransportBinding(communicationLogImpl,
                    httpClient, URI.create("/xyz/xyz"), soapMarshallingMock, soapUtilMock);) {

                clientTransportBinding.onRequestResponse(soapMessageMock);

                verify(soapMarshallingMock, times(1)).marshal(any(Envelope.class), argument.capture());

                assertArrayEquals(inputContent, logOutputStream.toByteArray());

                logOutputStream.reset();

                argument.getValue().write(outputContent);
                argument.getValue().flush();

                assertArrayEquals(outputContent, logOutputStream.toByteArray());
            }

        }

    }
}
