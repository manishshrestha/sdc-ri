package org.somda.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.model.LocalizedStringType;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.service.helper.MetadataSectionUtil;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wstransfer.WsTransferConstants;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Server interceptor for hosting services to serve WS-TransferGet requests.
 * <p>
 * {@linkplain HostingServiceInterceptor} acts as a {@link HostingService} implementation at the same time.
 */
public class HostingServiceInterceptor implements HostingService {
    private static final Logger LOG = LogManager.getLogger(HostingServiceInterceptor.class);

    private final WsDiscoveryTargetService targetService;
    private final WsAddressingUtil wsaUtil;
    private final MetadataSectionUtil metadataSectionUtil;
    private final List<HostedService> hostedServices;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory mexFactory;
    private final org.somda.sdc.dpws.model.ObjectFactory dpwsFactory;
    private final Logger instanceLogger;
    private ThisModelType thisModel;
    private ThisDeviceType thisDevice;


    @AssistedInject
    HostingServiceInterceptor(@Assisted WsDiscoveryTargetService targetService,
                              SoapUtil soapUtil,
                              SoapFaultFactory soapFaultFactory,
                              ObjectFactory mexFactory,
                              org.somda.sdc.dpws.model.ObjectFactory dpwsFactory,
                              WsAddressingUtil wsaUtil,
                              MetadataSectionUtil metadataSectionUtil,
                              @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.targetService = targetService;
        this.wsaUtil = wsaUtil;
        this.metadataSectionUtil = metadataSectionUtil;
        this.hostedServices = new ArrayList<>();
        this.soapUtil = soapUtil;
        this.soapFaultFactory = soapFaultFactory;
        this.mexFactory = mexFactory;
        this.dpwsFactory = dpwsFactory;

        LocalizedStringType manufacturer = LocalizedStringType.builder().withValue("Unknown Manufacturer").build();
        LocalizedStringType modelName = LocalizedStringType.builder().withValue("Unknown ModelName").build();
        this.thisModel = ThisModelType.builder()
            .withManufacturer(manufacturer)
            .withModelName(modelName)
            .build();

        LocalizedStringType friendlyName = LocalizedStringType.builder().withValue("Unknown FriendlyName").build();
        this.thisDevice = ThisDeviceType.builder().withFriendlyName(friendlyName).build();
    }

    @Override
    public List<String> getXAddrs() {
        return targetService.getXAddrs();
    }

    @MessageInterceptor(value = WsTransferConstants.WSA_ACTION_GET, direction = Direction.REQUEST)
    void processGet(RequestResponseObject rrObj) throws SoapFaultException {
        if (!rrObj.getRequest().getOriginalEnvelope().getBody().getAny().isEmpty()) {
            throw new SoapFaultException(soapFaultFactory
                    .createSenderFault(String.format("SOAP envelope body for action %s shall be empty",
                            WsTransferConstants.WSA_ACTION_GET)),
                    rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
        }

        var metadata = Metadata.builder()
            .addMetadataSection(createThisModel())
            .addMetadataSection(createThisDevice())
            .addMetadataSection(metadataSectionUtil.createRelationship(targetService.getEndpointReference(),
                targetService.getTypes(), hostedServices))
            .build();

        rrObj.getResponse().getWsAddressingHeader().setAction(
                wsaUtil.createAttributedURIType(WsTransferConstants.WSA_ACTION_GET_RESPONSE));

        soapUtil.setBody(metadata, rrObj.getResponse());
    }

    private MetadataSection createThisModel() {
        return MetadataSection.builder()
            .withDialect(DpwsConstants.MEX_DIALECT_THIS_MODEL)
            .withAny(dpwsFactory.createThisModel(getThisModel())).build();
    }

    private MetadataSection createThisDevice() {
        return MetadataSection.builder()
            .withDialect(DpwsConstants.MEX_DIALECT_THIS_DEVICE)
            .withAny(dpwsFactory.createThisDevice(getThisDevice())).build();
    }

    @Override
    public String getEndpointReferenceAddress() {
        return targetService.getEndpointReference().getAddress().getValue();
    }

    @Override
    public ThisModelType getThisModel() {
        return thisModel;
    }

    @Override
    public void setThisModel(ThisModelType thisModel) {
        this.thisModel = thisModel.newCopyBuilder()
            .withManufacturer(cutMaxFieldSize(thisModel.getManufacturer()))
            .withModelName(cutMaxFieldSize(thisModel.getModelName()))
            .withModelNumber(cutMaxFieldSize(thisModel.getModelNumber()))
            .withManufacturerUrl(cutMaxUriSize(thisModel.getManufacturerUrl()))
            .withPresentationUrl(cutMaxUriSize(thisModel.getPresentationUrl()))
            .withModelUrl(cutMaxUriSize(thisModel.getModelUrl()))
            .build();
        targetService.setMetadataModified();
    }

    @Override
    public ThisDeviceType getThisDevice() {
        return thisDevice;
    }

    @Override
    public void setThisDevice(ThisDeviceType thisDevice) {
        this.thisDevice = thisDevice.newCopyBuilder()
            .withFriendlyName(cutMaxFieldSize(thisDevice.getFriendlyName()))
            .withFirmwareVersion(cutMaxFieldSize(thisDevice.getFirmwareVersion()))
            .withSerialNumber(cutMaxFieldSize(thisDevice.getSerialNumber()))
            .build();
        targetService.setMetadataModified();
    }

    @Override
    public void addHostedService(HostedService hostedService) {
        hostedServices.add(hostedService);
        targetService.setMetadataModified();
    }

    @Override
    public List<HostedService> getHostedServices() {
        return new ArrayList<>(hostedServices);
    }

    @Override
    public String toString() {
        String str = getEndpointReferenceAddress();
        if (!getThisDevice().getFriendlyName().isEmpty()) {
            str += " (" + getThisDevice().getFriendlyName().get(0).getValue() + ")";
        }
        return str;
    }

    private List<LocalizedStringType> cutMaxFieldSize(List<LocalizedStringType> texts) {
        var newList = new ArrayList<LocalizedStringType>(texts.size());
        for (var text : texts) {
            var localizedText = text.newCopyBuilder()
                .withValue(cutMaxFieldSize(text.getValue()))
                .build();
            newList.add(localizedText);
        }

        return newList;
    }

    private String cutMaxFieldSize(@Nullable String text) {
        return cutSize(text, DpwsConstants.MAX_FIELD_SIZE);
    }

    private String cutMaxUriSize(@Nullable String uri) {
        return cutSize(uri, DpwsConstants.MAX_URI_SIZE);
    }

    private String cutSize(@Nullable String text, int size) {
        if (text == null) {
            return null;
        }
        var textBytes = text.getBytes(StandardCharsets.UTF_8);
        if (textBytes.length >= size) {
            int maxLength = size - 1;
            var newText = new String(Arrays.copyOf(textBytes, maxLength),
                    StandardCharsets.UTF_8);
            instanceLogger.warn("The following text was cut due to DPWS length violations (allowed: {} octets, " +
                    "actual: {} octets). '{}' is now '{}'", maxLength, textBytes.length, text, newText);
            return newText;
        } else {
            return text;
        }
    }
}
