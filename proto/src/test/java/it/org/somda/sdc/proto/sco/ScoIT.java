package it.org.somda.sdc.proto.sco;

import com.google.inject.Injector;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import it.org.somda.sdc.dpws.soap.Ssl;
import it.org.somda.sdc.proto.IntegrationTestUtil;
import it.org.somda.sdc.proto.provider.ProviderImplIT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.protosdc.proto.model.addressing.Addressing;
import org.somda.protosdc.proto.model.common.QName;
import test.org.somda.common.LoggingTestWatcher;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

@ExtendWith(LoggingTestWatcher.class)
public class ScoIT {
    private static final Logger LOG = LogManager.getLogger(ProviderImplIT.class);
    private static final String PROVIDER_NAME = "Ṱ̺̺̕o͞ ̷i̲̬͇̪͙n̝̗͕v̟̜̘̦͟o̶̙̰̠kè͚̮̺̪̹̱̤ ̖t̝͕̳̣̻̪͞h̼͓̲̦̳̘̲e͇̣̰̦̬͎ ̢̼̻̱̘h͚͎͙̜̣̲ͅi̦̲̣̰̤v̻͍e̺̭̳̪̰-m̢iͅn̖̺̞̲̯̰d̵̼̟͙̩̼̘̳ ̞̥̱̳̭r̛̗̘e͙p͠r̼̞̻̭̗e̺̠̣͟s̘͇̳͍̝͉e͉̥̯̞̲͚̬͜ǹ̬͎͎̟̖͇̤t͍̬̤͓̼̭͘ͅi̪̱n͠g̴͉ ͏͉ͅc̬̟h͡a̫̻̯͘o̫̟̖͍̙̝͉s̗̦̲.̨̹͈̣";
    private Injector providerInjector;
    private DpwsFramework dpwsFramework;
    private Injector consumerInjector;
    private DpwsFramework consumerDpwsFramework;


    //@BeforeEach
    void beforeEach() throws SocketException {
        this.providerInjector = new IntegrationTestUtil(
                new AbstractConfigurationModule() {
                    @Override
                    protected void defaultConfigure() {
                        bind(CryptoConfig.CRYPTO_SETTINGS,
                                CryptoSettings.class,
                                Ssl.setupServer());
                    }
                }
        ).getInjector();
        this.consumerInjector = new IntegrationTestUtil(
                new AbstractConfigurationModule() {
                    @Override
                    protected void defaultConfigure() {
                        bind(CryptoConfig.CRYPTO_SETTINGS,
                                CryptoSettings.class,
                                Ssl.setupClient());
                    }
                }
        ).getInjector();

        this.dpwsFramework = providerInjector.getInstance(DpwsFramework.class);
        dpwsFramework.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        dpwsFramework.startAsync().awaitRunning();

        this.consumerDpwsFramework = consumerInjector.getInstance(DpwsFramework.class);
        consumerDpwsFramework.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        consumerDpwsFramework.startAsync().awaitRunning();
    }

    @Test
    @DisplayName("SCO SetString round trip")
    void testSetString() throws InvalidProtocolBufferException {
        var qName = StringValue.newBuilder().setValue("test").build();
        var any = Any.pack(qName);
        var addrOriginal = Addressing.newBuilder().addExtension(any).build();
        var bytes = addrOriginal.toByteArray();
        var addrCopy = Addressing.parseFrom(bytes);

        if (addrCopy.getExtension(0).getTypeUrl().endsWith(qName.getDescriptorForType().getFullName())) {
            var unpack = addrCopy.getExtension(0).unpack(StringValue.class);
            Assertions.assertEquals(qName.getValue(), unpack.getValue());
        } else {
            Assertions.fail();
        }
    }
}
