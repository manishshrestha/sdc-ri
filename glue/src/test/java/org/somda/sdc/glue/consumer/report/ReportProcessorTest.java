package org.somda.sdc.glue.consumer.report;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.GetContextStates;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.consumer.report.helper.ReportWriter;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class ReportProcessorTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private ReportProcessor reportProcessor;
    private ReportWriter reportWriter;
    private ArgumentCaptor<AbstractReport> reportCaptor;
    private ArgumentCaptor<RemoteMdibAccess> mdibAccessCaptor;
    private RemoteMdibAccess mdibAccess;
    private ArgumentCaptor<MdibVersion> mdibVersionCaptor;
    private ArgumentCaptor<MdibStateModifications> mdibStateModificationsCaptor;

    @BeforeEach
    void beforeEach() {
        reportWriter = mock(ReportWriter.class);
        Injector injector = UT.createInjectorWithOverrides(new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                bind(ReportWriter.class).toInstance(reportWriter);
            }
        });

        reportProcessor = injector.getInstance(ReportProcessor.class);
        reportCaptor = ArgumentCaptor.forClass(AbstractReport.class);
        mdibAccessCaptor = ArgumentCaptor.forClass(RemoteMdibAccess.class);

        mdibVersionCaptor = ArgumentCaptor.forClass(MdibVersion.class);
        mdibStateModificationsCaptor= ArgumentCaptor.forClass(MdibStateModifications.class);

        mdibAccess = mock(RemoteMdibAccess.class);
    }

    @Test
    void applyMdibWithoutPreviousReportsWithoutContexts() throws PreprocessingException, ReportProcessingException {
        reportProcessor.startApplyingReportsOnMdib(mdibAccess, null);
        verifyZeroInteractions(mdibAccess);
    }

    @Test
    void applyMdibWithoutPreviousReportsWithContexts() throws PreprocessingException, ReportProcessingException {
        {
            MdibVersion mdibVersion = MdibVersion.create();
            when(mdibAccess.getMdibVersion()).thenReturn(mdibVersion);
            GetContextStatesResponse contextStatesResponse = createContextStatesResponse(mdibVersion);
            reportProcessor.startApplyingReportsOnMdib(mdibAccess, contextStatesResponse);
            verifyZeroInteractions(mdibAccess);
        }

        {
            MdibVersion mdibVersion = MdibVersion.create();
            MdibVersion mdibVersionIncremented = MdibVersion.increment(mdibVersion);
            when(mdibAccess.getMdibVersion()).thenReturn(mdibVersion);
            GetContextStatesResponse contextStatesResponse = createContextStatesResponse(mdibVersionIncremented);
            reportProcessor.startApplyingReportsOnMdib(mdibAccess, contextStatesResponse);
            verify(mdibAccess).writeStates(mdibVersionCaptor.capture(), mdibStateModificationsCaptor.capture());
            assertEquals(mdibVersionIncremented, mdibVersionCaptor.getValue());
        }

    }

    @Test
    void applyWithPreviousReportsMdibWithoutContexts() {

    }

    @Test
    void applyWithPreviousReportsMdibWithContexts() {

    }

    private GetContextStatesResponse createContextStatesResponse(MdibVersion mdibVersion) {
        GetContextStatesResponse response =  new GetContextStatesResponse();
        try {
            UT.getInjector().getInstance(MdibVersionUtil.class).setMdibVersion(mdibVersion, response);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        return response;
    }

    private EpisodicMetricReport createReport(MdibVersion mdibVersion) {
        EpisodicMetricReport report =  new EpisodicMetricReport();
        try {
            UT.getInjector().getInstance(MdibVersionUtil.class).setMdibVersion(mdibVersion, report);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        return report;
    }
}