package org.ieee11073.sdc.glue.provider.services.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.device.EventSourceAccess;
import org.ieee11073.sdc.glue.provider.services.helper.ReportGenerator;

/**
 * Factory to create {@linkplain ReportGenerator} instances.
 */
public interface ReportGeneratorFactory {
    /**
     * Creates a new {@linkplain ReportGenerator}.
     *
     * @param eventSourceAccess the event source used to send reports to receivers.
     * @return a new instance.
     */
    ReportGenerator createReportGenerator(@Assisted EventSourceAccess eventSourceAccess);
}
