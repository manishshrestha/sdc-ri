package org.ieee11073.sdc.dpws.ni;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class LocalAddressResolverTest {
    //@Test
    public void getLocalAddress() throws Exception {
        LocalAddressResolver lar = new LocalAddressResolverImpl();
        Optional<String> localAddress = lar.getLocalAddress(URI.create("http://www.google.com:80"));
        assertThat(localAddress, is(not(Optional.empty())));

        if (localAddress.isPresent()) {
            System.out.println("Found local address for given URI: " + localAddress.get());
        } else {
            System.out.println("No local address for given URI found. Maybe server is currently not reachable.");
        }
    }

}