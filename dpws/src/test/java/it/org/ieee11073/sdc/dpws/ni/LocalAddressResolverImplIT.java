package it.org.ieee11073.sdc.dpws.ni;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.ieee11073.sdc.dpws.ni.LocalAddressResolver;
import org.ieee11073.sdc.dpws.ni.LocalAddressResolverImpl;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class LocalAddressResolverImplIT {
    @Test
    public void getLocalAddress() throws Exception {
        Injector inj = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(LocalAddressResolver.class).to(LocalAddressResolverImpl.class).asEagerSingleton();
                    }
                });
        LocalAddressResolver lar = inj.getInstance(LocalAddressResolver.class);
        Optional<String> localAddress = lar.getLocalAddress(URI.create("http://www.google.com:80"));
        assertThat(localAddress, is(not(Optional.empty())));

        if (localAddress.isPresent()) {
            System.out.println("Found local address for given URI: " + localAddress.get());
        } else {
            System.out.println("No local address for given URI found. Maybe server is currently not reachable.");
        }
    }

}