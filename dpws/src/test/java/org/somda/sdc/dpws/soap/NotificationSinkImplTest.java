package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotificationSinkImplTest extends DpwsTest {
    private List<String> dispatchedSequence;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
        dispatchedSequence = new ArrayList<>();
    }

    @Test
    public void receiveRequestResponse() throws Exception {
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);

        NotificationSink nSink = getInjector().getInstance(NotificationSink.class);
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));

        SoapMessageFactory soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        SoapMessage notification = soapMessageFactory.createSoapMessage(soapEnv);

        nSink.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.NOTIFICATION)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(MAX)");
            }
        });

        nSink.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.NOTIFICATION, sequenceNumber = 5)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(5)");
            }
        });

        nSink.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(ACTION, MAX)");
            }
        });

        // Shall be skipped since argument is missing
        nSink.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete() {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
            }
        });

        // Shall be skipped since direction is invalid
        nSink.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.RESPONSE)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("INVALID NOTIFICATION(MAX)");
            }
        });

        // Shall be skipped since parameter is Request, but should be Notification
        nSink.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete(RequestObject rInfo) {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
            }
        });

        nSink.receiveNotification(notification);

        assertEquals(3, dispatchedSequence.size());
        assertEquals("NOTIFICATION(5)", dispatchedSequence.get(0));
        assertEquals("NOTIFICATION(MAX)", dispatchedSequence.get(1));
        assertEquals("NOTIFICATION(ACTION, MAX)", dispatchedSequence.get(2));
    }
}