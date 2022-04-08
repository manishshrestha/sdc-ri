package org.somda.sdc.glue.consumer.sco.factory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.common.BicepsModelCloning;
import org.somda.sdc.glue.consumer.sco.ScoTransactionImpl;
import org.somda.sdc.glue.consumer.sco.ScoUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ScoTransactionFactory {
    @Inject
    private Injector injector;

    public <T extends AbstractSetResponse> ScoTransactionImpl<T> createScoTransaction(
            @Assisted T response,
            @Assisted @Nullable Consumer<OperationInvokedReport.ReportPart> reportListener) {
        return new ScoTransactionImpl<>(response, reportListener,
                injector.getInstance(BicepsModelCloning.class),
                injector.getInstance(ScoUtil.class));
    }
}
