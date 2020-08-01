package org.somda.sdc.proto.provider;

import org.somda.sdc.biceps.model.message.AbstractReport;

/**
 * Provider interface to send reports.
 */
public interface EventSource {
    /**
     * Send a notification.
     *
     * @param action the action that classifies the report to send.
     * @param report the report to send.
     * @throws Exception if serialization or transmission fails.
     */
    void sendNotification(String action, AbstractReport report) throws Exception;
}
