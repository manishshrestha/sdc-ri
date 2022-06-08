package org.somda.sdc.glue.provider.services;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.model.history.HistoryQueryType;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wseventing.model.FilterType;
import org.somda.sdc.dpws.soap.wseventing.model.Subscribe;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.provider.services.helper.MdibRevisionObserver;
import org.somda.sdc.glue.provider.services.helper.factory.MdibRevisionObserverFactory;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implementation of the history service.
 */
public class HistoryService extends WebService {
    private final LocalMdibAccess mdibAccess;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory faultFactory;
    private final ObjectFactory messageModelFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final WsAddressingUtil wsaUtil;
    private final SoapFaultFactory soapFaultFactory;
    private final MdibRevisionObserver mdibRevisionObserver;

    @AssistedInject
    HistoryService(@Assisted LocalMdibAccess mdibAccess,
                   SoapUtil soapUtil,
                   SoapFaultFactory faultFactory,
                   ObjectFactory messageModelFactory,
                   MdibVersionUtil mdibVersionUtil,
                   WsAddressingUtil wsaUtil,
                   SoapFaultFactory soapFaultFactory,
                   MdibRevisionObserverFactory mdibRevisionObserverFactory) {
        this.mdibAccess = mdibAccess;
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.messageModelFactory = messageModelFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.wsaUtil = wsaUtil;
        this.soapFaultFactory = soapFaultFactory;
        mdibRevisionObserver = mdibRevisionObserverFactory.createMdibRevisionObserver(this, mdibAccess);
        mdibRevisionObserver.createInitialReport(mdibAccess.getMdibVersion());
        mdibAccess.registerObserver(mdibRevisionObserver);
    }

    //TODO #142: how do we subscribe only to ACTION_HISTORY_MDIB_REPORT subscribe actions?
    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_SUBSCRIBE, direction = Direction.REQUEST)
    void historyMdibReportSubscribe(RequestResponseObject requestResponseObject) throws SoapFaultException {
        Subscribe subscribe = soapUtil.getBody(requestResponseObject.getRequest(), Subscribe.class)
                .orElseThrow(getSoapFaultExceptionSupplier("Failed to parse subscribe request body"));

        var filter = Optional.ofNullable(subscribe.getFilter())
                .orElseThrow(getSoapFaultExceptionSupplier("No filter given, but required."));

        if (filter.getContent().contains(ActionConstants.ACTION_HISTORY_MDIB_REPORT)) {
            // this is Historic MDIB subscribe
            filter.getContent().stream()
                    .filter(content -> content instanceof HistoryQueryType)
                    .findFirst().ifPresent(query -> queryHistoricalData((HistoryQueryType) query));
        }

    }

    private Supplier<SoapFaultException> getSoapFaultExceptionSupplier(String msg) {
        return () -> new SoapFaultException(soapFaultFactory.createSenderFault(msg));
    }

    private void queryHistoricalData(HistoryQueryType query) {
        //TODO #142: filter historical data and send notifications with the reports
        mdibRevisionObserver.getChangeSequenceReport(query);
    }
}
