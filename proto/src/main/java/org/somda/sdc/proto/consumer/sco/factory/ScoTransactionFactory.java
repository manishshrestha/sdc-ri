package org.somda.sdc.proto.consumer.sco.factory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.common.util.ObjectUtil;
import org.somda.sdc.proto.consumer.sco.ScoTransactionImpl;
import org.somda.sdc.proto.consumer.sco.ScoUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ScoTransactionFactory {
    @Inject
    private Injector injector;

    public <T extends AbstractSetResponse> ScoTransactionImpl<T> createScoTransaction(
            @Assisted T response,
            @Assisted @Nullable Consumer<OperationInvokedReport.ReportPart> reportListener) {
        return new ScoTransactionImpl<>(response, reportListener,
                injector.getInstance(ObjectUtil.class),
                injector.getInstance(ScoUtil.class));
    }
}
