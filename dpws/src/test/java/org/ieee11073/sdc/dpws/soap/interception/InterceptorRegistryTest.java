package org.ieee11073.sdc.dpws.soap.interception;

import org.ieee11073.sdc.dpws.DpwsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InterceptorRegistryTest extends DpwsTest{

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testAddInterceptor() {
        final String action = "http://action";
        InterceptorRegistry registry = getInjector().getInstance(InterceptorRegistry.class);
        registry.addInterceptor(new Object() {
            @MessageInterceptor
            InterceptorResult test(RequestResponseObject rrInfo) {
                return InterceptorResult.PROCEED;
            }
        });
        registry.addInterceptor(new Object() {
            @MessageInterceptor(sequenceNumber = 5)
            InterceptorResult test(RequestResponseObject rrInfo) {
                return InterceptorResult.PROCEED;
            }
        });

        registry.addInterceptor(new Object() {
            @MessageInterceptor(action)
            InterceptorResult test(RequestResponseObject rrInfo) {
                return InterceptorResult.PROCEED;
            }
        });

        List<InterceptorInfo> defaultInterceptors = registry.getDefaultInterceptors();
        assertEquals(2, defaultInterceptors.size());
        assertEquals(5, defaultInterceptors.get(0).getSequenceNumber());
        assertEquals(Integer.MAX_VALUE, defaultInterceptors.get(1).getSequenceNumber());
        List<InterceptorInfo> interceptors = registry.getInterceptors(action);
        assertEquals(1, interceptors.size());
    }
}