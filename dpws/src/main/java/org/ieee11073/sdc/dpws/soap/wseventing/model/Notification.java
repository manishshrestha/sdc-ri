package org.ieee11073.sdc.dpws.soap.wseventing.model;

import org.ieee11073.sdc.dpws.soap.SoapMessage;

import java.time.LocalDateTime;

/**
 * Notification queue item that saves a construction time to allow detection of stale notifications.
 */
public class Notification {
    private final SoapMessage payload;
    private final LocalDateTime constructionTime;

    public Notification(SoapMessage payload)
    {
        this.constructionTime = LocalDateTime.now();
        this.payload = payload;
    }

    public SoapMessage getPayload() {
        return payload;
    }

    public LocalDateTime getConstructionTime() {
        return constructionTime;
    }
}
