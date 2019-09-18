package org.ieee11073.sdc.dpws.soap.wsaddressing;

import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Convenience class to represent WS-Addressing header information.
 *
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops">Message Addressing Properties</a>
 */
public class WsAddressingHeader {
    private AttributedURIType action;
    private AttributedURIType messageId;
    private AttributedURIType to;
    private AttributedURIType relatesTo;

    public Optional<AttributedURIType> getAction() {
        return Optional.ofNullable(action);
    }

    public void setAction(@Nullable AttributedURIType action) {
        this.action = action;
    }

    public Optional<AttributedURIType> getMessageId() {
        return Optional.ofNullable(messageId);
    }

    public void setMessageId(@Nullable AttributedURIType messageId) {
        this.messageId = messageId;
    }

    public Optional<AttributedURIType> getTo() {
        return Optional.ofNullable(to);
    }

    public void setTo(@Nullable AttributedURIType to) {
        this.to = to;
    }

    public Optional<AttributedURIType> getRelatesTo() {
        return Optional.ofNullable(relatesTo);
    }

    public void setRelatesTo(@Nullable AttributedURIType relatesTo) {
        this.relatesTo = relatesTo;
    }
}
