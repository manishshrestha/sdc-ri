package org.somda.sdc.dpws;

import java.io.OutputStream;

import org.somda.sdc.dpws.CommunicationLogSinkImpl;

/**
 * Communication log sink interface.
 */
public interface CommunicationLogSink {
    
    /**
     * Creates a branch based on the given key and path.
     * 
     * @param path path to save the key to branch mapping in.
     * @param key  key that shall map to the output branch.
     * @return an {@linkplain OutputStream}, that represents the branch to write to.
     */
    OutputStream getTargetStream(CommunicationLog.TransportType path, String key);
    
}
