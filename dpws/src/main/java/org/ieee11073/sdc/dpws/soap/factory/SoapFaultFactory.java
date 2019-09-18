package org.ieee11073.sdc.dpws.soap.factory;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.SoapConstants;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.model.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingConstants;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory to create SOAP fault structures.
 *
 * - Parameter names are in accordance to SOAP fault description. Other parameter names are explained.
 * - The implied reason text's language is 'en'.
 *
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#soapfault">SOAP Fault</a>
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#id2270167">SOAP 1.2 Fault Binding</a>
 */
public class SoapFaultFactory {
    private final ObjectFactory soapFactory;
    private final SoapUtil soapUtil;

    @Inject
    SoapFaultFactory(ObjectFactory soapFactory,
                     SoapUtil soapUtil) {
        this.soapFactory = soapFactory;
        this.soapUtil = soapUtil;
    }

    /**
     * @param actionUri wsa:Action to put into resulting {@link SoapMessage} header.
     * @param code Soap fault code
     * @param subcode Soap fault subcode
     * @param reasonText reason for fault
     * @param detail fault details
     * @return a {@link SoapMessage} containing the fault
     */
    public SoapMessage createFault(String actionUri, QName code, QName subcode, String reasonText, @Nullable Object detail) {

        Subcode scObj = new Subcode();
        scObj.setValue(subcode);

        Faultcode fcObj = new Faultcode();
        fcObj.setValue(code);
        fcObj.setSubcode(scObj);

        Reasontext rtObj = new Reasontext();
        rtObj.setValue(reasonText);
        rtObj.setLang("en");
        List<Reasontext> rtListObj = new ArrayList<>();
        rtListObj.add(rtObj);

        Faultreason frObj = new Faultreason();
        frObj.setText(rtListObj);

        Fault f = new Fault();
        f.setCode(fcObj);
        f.setReason(frObj);

        if (detail != null) {
            List<Object> anyObjList = new ArrayList<>();
            anyObjList.add(detail);
            Detail dObj = new Detail();
            dObj.setAny(anyObjList);
            f.setDetail(dObj);
        }

        return soapUtil.createMessage(actionUri, soapFactory.createFault(f));
    }

    /**
     * @param actionUri wsa:Action to put into resulting {@link SoapMessage} header.
     * @param code Soap fault code
     * @param subcode Soap fault subcode
     * @param reasonText reason for fault
     * @return a {@link SoapMessage} containing the fault
     */
    public SoapMessage createFault(String actionUri, QName code, QName subcode, String reasonText) {
        return createFault(actionUri, code, subcode, reasonText, null);
    }

    /**
     * @param actionUri wsa:Action to put into resulting {@link SoapMessage} header.
     * @param code Soap fault code
     * @param subcode Soap fault subcode
     * @return a {@link SoapMessage} containing the fault
     */
    public SoapMessage createFault(String actionUri, QName code, QName subcode) {
        return createFault(actionUri, code, subcode, "", null);
    }

    /**
     * Create soap:Receiver fault with given text.
     * @param reasonText reason for fault
     * @return a {@link SoapMessage} containing the fault
     */
    public SoapMessage createReceiverFault(String reasonText) {
        return createFault(WsAddressingConstants.FAULT_ACTION, SoapConstants.RECEIVER, SoapConstants.DEFAULT_SUBCODE, reasonText);
    }

    /**
     * Create soap:Sender fault with given text.
     * @param reasonText reason for fault
     * @return a {@link SoapMessage} containing the fault
     */
    public SoapMessage createSenderFault(String reasonText) {
        return createFault(WsAddressingConstants.FAULT_ACTION, SoapConstants.SENDER, SoapConstants.DEFAULT_SUBCODE, reasonText);
    }
}
