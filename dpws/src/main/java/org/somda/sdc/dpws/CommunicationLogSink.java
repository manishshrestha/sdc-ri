package org.somda.sdc.dpws;

import java.io.OutputStream;

import org.somda.sdc.dpws.CommunicationLogSinkImpl;

/**
 * Communication log interface.
 */
public interface CommunicationLogSink {
    
    /**
     * Creates a branch based on the given key and path.
     * 
     * @param path path to save the key to branch mapping in
     * @param key  key that shall map to the output branch
     * @return an {@linkplain OutputStream}, that represents the branch to write to.
     */
    OutputStream createBranch(CommunicationLogSinkImpl.BranchPath path, String key);
    
    
    /**
     * Defines the paths for which branches can be created.
     */
    public enum BranchPath {
        UDP("udp"), HTTP("http");

        private final String stringRepresentation;

        BranchPath(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }
    }
    
}
