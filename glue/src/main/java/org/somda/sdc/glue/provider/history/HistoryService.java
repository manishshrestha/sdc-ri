package org.somda.sdc.glue.provider.history;

import org.somda.sdc.biceps.model.history.HistoryQueryType;

public interface HistoryService {

    void sendNotification(HistoryQueryType historyQuery);

    void subscribe(HistoryQueryType historyQuery);
}
