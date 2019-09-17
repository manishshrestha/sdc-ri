package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotificationSourceImplTest extends DpwsTest {
    private List<String> dispatchedSequence;
    
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
        dispatchedSequence = new ArrayList<>();
    }

    @Test
    public void sendNotification() throws Exception {
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));

        SoapMessageFactory soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        SoapMessage notification = soapMessageFactory.createSoapMessage(soapEnv);

       NotificationSourceFactory clientFactory = getInjector().getInstance(NotificationSourceFactory.class);
        NotificationSource nSource = clientFactory.createNotificationSource(not -> dispatchedSequence.add("NETWORK"));

        nSource.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.NOTIFICATION)
            InterceptorResult onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        nSource.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.NOTIFICATION, sequenceNumber = 5)
            InterceptorResult onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(5)");
                return InterceptorResult.PROCEED;
            }
        });

        nSource.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            InterceptorResult onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(ACTION, MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        // Shall be skipped since argument is missing
        nSource.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            InterceptorResult onDelete() {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        // Shall be skipped since argument is Request, but should be Notification
        nSource.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            InterceptorResult onDelete(RequestObject rInfo) {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        // Shall be skipped since direction is Request, but should be Notification
        nSource.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            InterceptorResult onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
                return InterceptorResult.PROCEED;
            }
        });

        nSource.sendNotification(notification);

        assertEquals(4, dispatchedSequence.size());
        assertEquals("NOTIFICATION(5)", dispatchedSequence.get(0));
        assertEquals("NOTIFICATION(MAX)", dispatchedSequence.get(1));
        assertEquals("NOTIFICATION(ACTION, MAX)", dispatchedSequence.get(2));
        assertEquals("NETWORK", dispatchedSequence.get(3));
    }
}