package it.org.somda.glue.provider;

import com.google.inject.Injector;
import it.org.somda.glue.IntegrationTestUtil;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.factory.DpwsFrameworkFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.common.MdibMapper;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;

public class RunTestSdcDevice {
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();

    public static void main(String[] args) throws IOException, PreprocessingException, JAXBException {
        final Injector injector = IT.getInjector();

        final NetworkInterface networkInterface = NetworkInterface.getByName("eth0");//InetAddress.getLocalHost());

        final DpwsFramework dpwsFramework = injector.getInstance(DpwsFrameworkFactory.class).createDpwsFramework(networkInterface);

        BaseTreeModificationsSet baseTreeModificationsSet = new BaseTreeModificationsSet(new MockEntryFactory(
                injector.getInstance(MdibTypeValidator.class)));

        final LocalMdibAccess localMdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        localMdibAccess.writeDescription(baseTreeModificationsSet.createFullyPopulatedTree());

        final SdcDevice sdcDevice = injector.getInstance(SdcDeviceFactory.class).createSdcDevice(new DeviceSettings() {
            @Override
            public EndpointReferenceType getEndpointReference() {
                return injector.getInstance(WsAddressingUtil.class)
                        .createEprWithAddress("urn:uuid:857bf583-8a51-475f-a77f-d0ca7de69b00");
            }

            @Override
            public NetworkInterface getNetworkInterface() {
                return networkInterface;
            }
        }, localMdibAccess, Collections.EMPTY_LIST);

        sdcDevice.getDiscoveryAccess().setTypes(Arrays.asList(new QName("http://standards.ieee.org/downloads/11073/11073-20702-2016", "MedicalDevice")));

        final MdibMapperFactory mapperFactory = injector.getInstance(MdibMapperFactory.class);
        final MdibXmlIo mdibXmlIo = injector.getInstance(MdibXmlIo.class);
        final MdibMapper mdibMapper = mapperFactory.createMdibMapper(localMdibAccess);
        final Mdib expectedMdib = mdibMapper.mapMdib();
        mdibXmlIo.writeMdib(expectedMdib, new File("C:\\temp\\mdib.xml"));

        dpwsFramework.startAsync().awaitRunning();
        sdcDevice.startAsync().awaitRunning();

        System.in.read();

        sdcDevice.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }
}
