package org.somda.sdc.dpws;

import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.OutputStream;

/**
 * Communication log sink interface.
 */
public interface CommunicationLogSink {
    /**
     * Creates a branch based on the given key and path.
     *
     * @param path                 path to save the key to branch mapping in.
     * @param direction            direction of message to store.
     * @param messageType          the type of the message i.e. request, response.
     * @param communicationContext context which can be used to derive storage information.
     * @param level                is the message logged at application-level or network-level?
     * @return an {@linkplain OutputStream}, that represents the branch to write to.
     */
    OutputStream createTargetStream(CommunicationLog.TransportType path,
                                    CommunicationLog.Direction direction,
                                    CommunicationLog.MessageType messageType,
                                    CommunicationContext communicationContext,
                                    CommunicationLog.Level level);

    /**
     * Creates a related branch based on the given key and path.
     *
     * @param relatesTo            OutputStream created by a call to createTargetStream that
     *                             contains the Message that this one is related to.
     * @param path                 path to save the key to branch mapping in.
     * @param direction            direction of message to store.
     * @param messageType          the type of the message i.e. request, response.
     * @param communicationContext context which can be used to derive storage information.
     * @param level                is the message logged at application-level or network-level?
     * @return an {@linkplain OutputStream}, that represents the branch to write to.
     */
    OutputStream createRelatedTargetStream(OutputStream relatesTo,
                                        CommunicationLog.TransportType path,
                                        CommunicationLog.Direction direction,
                                        CommunicationLog.MessageType messageType,
                                        CommunicationContext communicationContext,
                                        CommunicationLog.Level level);
}
