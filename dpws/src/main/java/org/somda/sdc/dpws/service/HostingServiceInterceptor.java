package org.somda.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.common.util.ObjectUtil;
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
    private static final Logger LOG = LoggerFactory.getLogger(HostingServiceInterceptor.class);

    private final WsDiscoveryTargetService targetService;
    private final ObjectUtil objectUtil;
    private final WsAddressingUtil wsaUtil;
    private final MetadataSectionUtil metadataSectionUtil;
    private final List<HostedService> hostedServices;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory mexFactory;
    private final org.somda.sdc.dpws.model.ObjectFactory dpwsFactory;
    private ThisModelType thisModel;
    private ThisDeviceType thisDevice;

    @Override
    public List<String> getXAddrs() {
        return targetService.getXAddrs();
    }

    @AssistedInject
    HostingServiceInterceptor(@Assisted WsDiscoveryTargetService targetService,
                              SoapUtil soapUtil,
                              SoapFaultFactory soapFaultFactory,
                              ObjectFactory mexFactory,
                              org.somda.sdc.dpws.model.ObjectFactory dpwsFactory,
                              ObjectUtil objectUtil,
                              WsAddressingUtil wsaUtil,
                              MetadataSectionUtil metadataSectionUtil) {
        this.targetService = targetService;
        this.objectUtil = objectUtil;
        this.wsaUtil = wsaUtil;
        this.metadataSectionUtil = metadataSectionUtil;
        this.hostedServices = new ArrayList<>();
        this.soapUtil = soapUtil;
        this.soapFaultFactory = soapFaultFactory;
        this.mexFactory = mexFactory;
        this.dpwsFactory = dpwsFactory;

        this.thisModel = dpwsFactory.createThisModelType();
        LocalizedStringType manufacturer = dpwsFactory.createLocalizedStringType();
        manufacturer.setValue("Unknown Manufacturer");
        this.thisModel.getManufacturer().add(manufacturer);
        LocalizedStringType modelName = dpwsFactory.createLocalizedStringType();
        modelName.setValue("Unknown ModelName");
        this.thisModel.getModelName().add(modelName);

        this.thisDevice = dpwsFactory.createThisDeviceType();
        LocalizedStringType friendlyName = dpwsFactory.createLocalizedStringType();
        friendlyName.setValue("Unknown FriendlyName");
        this.thisDevice.getFriendlyName().add(friendlyName);
    }

    @MessageInterceptor(value = WsTransferConstants.WSA_ACTION_GET, direction = Direction.REQUEST)
    void processGet(RequestResponseObject rrObj) throws SoapFaultException {
        if (!rrObj.getRequest().getOriginalEnvelope().getBody().getAny().isEmpty()) {
            throw new SoapFaultException(soapFaultFactory
                    .createSenderFault(String.format("SOAP envelope body for action %s shall be empty",
                            WsTransferConstants.WSA_ACTION_GET)));
        }

        Metadata metadata = mexFactory.createMetadata();
        List<MetadataSection> metadataSection = metadata.getMetadataSection();

        metadataSection.add(createThisModel());
        metadataSection.add(createThisDevice());
        metadataSection.add(metadataSectionUtil.createRelationship(targetService.getEndpointReference(),
                targetService.getTypes(), hostedServices));

        metadata.setMetadataSection(metadataSection);

        rrObj.getResponse().getWsAddressingHeader().setAction(
                wsaUtil.createAttributedURIType(WsTransferConstants.WSA_ACTION_GET_RESPONSE));

        soapUtil.setBody(metadata, rrObj.getResponse());
    }

    private MetadataSection createThisModel() {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(DpwsConstants.MEX_DIALECT_THIS_MODEL);
        metadataSection.setAny(dpwsFactory.createThisModel(getThisModel()));
        return metadataSection;
    }

    private MetadataSection createThisDevice() {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(DpwsConstants.MEX_DIALECT_THIS_DEVICE);
        metadataSection.setAny(dpwsFactory.createThisDevice(getThisDevice()));
        return metadataSection;
    }

    @Override
    public String getEndpointReferenceAddress() {
        return targetService.getEndpointReference().getAddress().getValue();
    }

    @Override
    public ThisModelType getThisModel() {
        return objectUtil.deepCopy(thisModel);
    }

    @Override
    public void setThisModel(ThisModelType thisModel) {
        this.thisModel = objectUtil.deepCopy(thisModel);
        this.thisModel.setManufacturer(cutMaxFieldSize(thisModel.getManufacturer()));
        this.thisModel.setModelName(cutMaxFieldSize(thisModel.getModelName()));
        this.thisModel.setModelNumber(cutMaxFieldSize(thisModel.getModelNumber()));
        this.thisModel.setManufacturerUrl(cutMaxUriSize(thisModel.getManufacturerUrl()));
        this.thisModel.setPresentationUrl(cutMaxUriSize(thisModel.getPresentationUrl()));
        this.thisModel.setModelUrl(cutMaxUriSize(thisModel.getModelUrl()));
        targetService.setMetadataModified();
    }

    @Override
    public ThisDeviceType getThisDevice() {
        return objectUtil.deepCopy(thisDevice);
    }

    @Override
    public void setThisDevice(ThisDeviceType thisDevice) {
        this.thisDevice = objectUtil.deepCopy(thisDevice);
        this.thisDevice.setFriendlyName(cutMaxFieldSize(thisDevice.getFriendlyName()));
        this.thisDevice.setFirmwareVersion(cutMaxFieldSize(thisDevice.getFirmwareVersion()));
        this.thisDevice.setSerialNumber(cutMaxFieldSize(thisDevice.getSerialNumber()));
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
        String str = getEndpointReferenceAddress().toString();
        if (!getThisDevice().getFriendlyName().isEmpty()) {
            str += " (" + getThisDevice().getFriendlyName().get(0).getValue() + ")";
        }
        return str;
    }

    private List<LocalizedStringType> cutMaxFieldSize(List<LocalizedStringType> texts) {
        var newList = new ArrayList<LocalizedStringType>(texts.size());
        for (var text : texts) {
            var localizedText = objectUtil.deepCopy(text);
            localizedText.setValue(cutMaxFieldSize(text.getValue()));
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
            LOG.warn("The following text was cut due to DPWS length violations (allowed: {} octets, " +
                    "actual: {} octets). '{}' is now '{}'", maxLength, textBytes.length, text, newText);
            return newText;
        } else {
            return text;
        }
    }
}
