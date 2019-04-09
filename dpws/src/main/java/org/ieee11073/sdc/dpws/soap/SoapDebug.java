package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;

import javax.xml.bind.JAXBElement;
import java.util.Optional;

/**
 * Utility class to create textual representation of {@link SoapMessage} objects.
 */
public class SoapDebug {
    /**
     * Get textual representation of a given SOAP message.
     *
     * @param msg The message to create a text representation from.
     * @return String consisting of the message's action, message id and message type.
     */
    public static String get(SoapMessage msg) {
        StringBuffer sb = new StringBuffer();
        sb.append("SoapMsg(");

        appendAction(sb, msg);
        appendMsgId(sb, msg);
        appendMsgType(sb, msg);

        sb.append(")");
        return sb.toString();
    }

    /**
     * Get brief textual representation of a given SOAP message.
     *
     * @param msg The message to create a text representation from.
     * @return String consisting of the message's message id.
     */
    public static String getBrief(SoapMessage msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("SoapMsg(");

        Optional<AttributedURIType> uri = msg.getWsAddressingHeader().getMessageId();
        if (uri.isPresent()) {
            sb.append(uri.get().getValue());
        } else {
            sb.append("<unknown>");
        }

        sb.append(")");
        return sb.toString();
    }

    private static void appendAction(StringBuffer sb, SoapMessage msg) {
        msg.getWsAddressingHeader().getAction().ifPresent(uri ->
                sb.append(String.format("action=[%s];", uri.getValue())));
    }

    private static void appendMsgId(StringBuffer sb, SoapMessage msg) {
        msg.getWsAddressingHeader().getMessageId().ifPresent(uri ->
                sb.append(String.format("msgId=[%s];", uri.getValue())));
    }

    private static void appendMsgType(StringBuffer sb, SoapMessage msg) {
        if (msg.getOriginalEnvelope().getBody().getAny().size() == 0) {
            sb.append("bodyType=[n/a];");
        } else {
            Object obj = msg.getOriginalEnvelope().getBody().getAny().get(0);
            String name = obj.getClass().getSimpleName();
            if (obj instanceof JAXBElement) {
                name = ((JAXBElement)obj).getName().toString();
            }
            sb.append(String.format("bodyType=[%s];", name));
        }
    }
}
