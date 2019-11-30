package org.somda.sdc.glue.consumer;

import com.google.common.util.concurrent.ListenableFuture;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.glue.consumer.sco.InvocationException;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;

import javax.annotation.Nullable;

public interface SetServiceAccess {
    <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(
            T setRequest,
            Class<V> responseClass);

    <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(
            T setRequest,
            @Nullable java.util.function.Consumer<OperationInvokedReport.ReportPart> reportListener,
            Class<V> responseClass);
}
