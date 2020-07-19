package org.somda.sdc.proto.addressing;

import com.google.common.collect.EvictingQueue;
import com.google.inject.name.Named;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConfig;

import javax.inject.Inject;
import java.net.URI;
import java.util.Optional;

public class MessageDuplicateDetection {
    private final EvictingQueue<URI> messageIdCache;

    @Inject
    MessageDuplicateDetection(@Named(WsAddressingConfig.MESSAGE_ID_CACHE_SIZE) Integer messageIdCacheSize) {
        this.messageIdCache = EvictingQueue.create(messageIdCacheSize);
    }

    // note the synchronized keyword as MessageDuplicateDetection is allowed to be shared between multiple threads
    public synchronized boolean isDuplicate(String messageId) {
        var messageIdUri = URI.create(messageId);
        Optional<URI> foundMessageId = messageIdCache.stream()
                .filter(messageIdUri::equals)
                .findFirst();
        if (foundMessageId.isPresent()) {
            return true;
        } else {
            messageIdCache.add(URI.create(messageId));
            return false;
        }
    }
}
