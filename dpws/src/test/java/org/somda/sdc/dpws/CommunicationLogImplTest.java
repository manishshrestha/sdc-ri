package org.somda.sdc.dpws;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.common.guice.DefaultHelperModule;
import test.org.somda.common.TestLogging;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSinkImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.udp.UdpMessage;

import java.util.List;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * 
 */
public class CommunicationLogImplTest extends DpwsTest {

    @Test
    void content() throws IOException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        byte[] content = UUID.randomUUID().toString().getBytes();

        try (ByteArrayOutputStream httpMockOutputStream = new ByteArrayOutputStream();
                ByteArrayOutputStream udpMockOutputStream = new ByteArrayOutputStream();
                ByteArrayInputStream inputTestInputStream = new ByteArrayInputStream(content);
                ByteArrayOutputStream outputTestOutputStream = new ByteArrayOutputStream();) {

            when(communicationLogSinkImplMock.createBranch(eq(CommunicationLogSink.BranchPath.HTTP), anyString()))
                    .thenReturn(httpMockOutputStream);
            when(communicationLogSinkImplMock.createBranch(eq(CommunicationLogSink.BranchPath.UDP), anyString()))
                    .thenReturn(udpMockOutputStream);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

            InputStream resultingInputStream = communicationLogImpl
                    .logHttpMessage(CommunicationLogImpl.HttpDirection.OUTBOUND_REQUEST, "_", 0, inputTestInputStream);

            assertThat(resultingInputStream.readAllBytes(), is(content));
            assertThat(httpMockOutputStream.toByteArray(), is(content));

            httpMockOutputStream.reset();

            OutputStream resultingOutputStream = communicationLogImpl.logHttpMessage(
                    CommunicationLogImpl.HttpDirection.OUTBOUND_REQUEST, "_", 0, outputTestOutputStream);

            resultingOutputStream.write(content);
            resultingOutputStream.flush();

            assertThat(outputTestOutputStream.toByteArray(), is(content));
            assertThat(httpMockOutputStream.toByteArray(), is(content));

            communicationLogImpl.logUdpMessage(CommunicationLogImpl.UdpDirection.INBOUND, "_", 0,
                    new UdpMessage(content, content.length));

            assertThat(udpMockOutputStream.toByteArray(), is(content));
        }
    }

    @Test
    void branchPath() throws IOException {

        byte[] content = UUID.randomUUID().toString().getBytes();

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content);) {
            for (CommunicationLogImpl.HttpDirection httpDir : CommunicationLogImpl.HttpDirection.values()) {
                reset(communicationLogSinkImplMock);

                when(communicationLogSinkImplMock.createBranch(any(CommunicationLogSink.BranchPath.class), anyString()))
                        .thenReturn(OutputStream.nullOutputStream());

                communicationLogImpl.logHttpMessage(httpDir, "_", 0, inputStream);
                communicationLogImpl.logHttpMessage(httpDir, "_", 0, OutputStream.nullOutputStream());
                verify(communicationLogSinkImplMock, times(2)).createBranch(eq(CommunicationLogSink.BranchPath.HTTP),
                        anyString());
            }
        }

        for (CommunicationLogImpl.UdpDirection udpDir : CommunicationLogImpl.UdpDirection.values()) {
            reset(communicationLogSinkImplMock);

            when(communicationLogSinkImplMock.createBranch(any(CommunicationLogSink.BranchPath.class), anyString()))
                    .thenReturn(OutputStream.nullOutputStream());

            communicationLogImpl.logUdpMessage(udpDir, "_", 0, new UdpMessage(content, content.length));
            verify(communicationLogSinkImplMock, times(1)).createBranch(eq(CommunicationLogSink.BranchPath.UDP),
                    anyString());
        }
    }
}
