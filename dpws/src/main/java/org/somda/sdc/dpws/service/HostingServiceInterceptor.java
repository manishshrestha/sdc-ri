package org.somda.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.common.util.ObjectUtil;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.model.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Server interceptor for hosting services to serve WS-TransferGet requests.
 * <p>
 * {@linkplain HostingServiceInterceptor} acts as a {@link HostingService} implementation at the same time.
 */
public class HostingServiceInterceptor implements HostingService {
    private static final Logger LOG = LoggerFactory.getLogger(HostingServiceInterceptor.class);

    private final WsDiscoveryTargetService targetService;
    private final ObjectUtil beanUtil;
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
    public List<URI> getXAddrs() {
        return targetService.getXAddrs().parallelStream()
                .map(URI::create)
                .collect(Collectors.toList());
    }

    @AssistedInject
    HostingServiceInterceptor(@Assisted WsDiscoveryTargetService targetService,
                              SoapUtil soapUtil,
                              SoapFaultFactory soapFaultFactory,
                              ObjectFactory mexFactory,
                              org.somda.sdc.dpws.model.ObjectFactory dpwsFactory,
                              ObjectUtil beanUtil,
                              WsAddressingUtil wsaUtil,
                              MetadataSectionUtil metadataSectionUtil) {
        this.targetService = targetService;
        this.beanUtil = beanUtil;
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
    public URI getEndpointReferenceAddress() {
        return URI.create(targetService.getEndpointReference().getAddress().getValue());
    }

    @Override
    public ThisModelType getThisModel() {
        return beanUtil.deepCopy(thisModel);
    }

    @Override
    public void setThisModel(ThisModelType thisModel) {
        this.thisModel = beanUtil.deepCopy(thisModel);
        targetService.setMetadataModified();
    }

    @Override
    public ThisDeviceType getThisDevice() {
        return beanUtil.deepCopy(thisDevice);
    }

    @Override
    public void setThisDevice(ThisDeviceType thisDevice) {
        this.thisDevice = beanUtil.deepCopy(thisDevice);
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
}
