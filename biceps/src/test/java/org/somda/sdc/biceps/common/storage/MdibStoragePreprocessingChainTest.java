package org.somda.sdc.biceps.common.storage;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.factory.MdibStoragePreprocessingChainFactory;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(LoggingTestWatcher.class)
class MdibStoragePreprocessingChainTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private static MdibStoragePreprocessingChainFactory chainFactory;

    @BeforeAll
    static void beforeAll() {
        chainFactory = UT.getInjector().getInstance(MdibStoragePreprocessingChainFactory.class);
    }

    @Test
    void processDescriptionModifications() throws Exception {
        // Given a preprocessing chain with 3 segments
        final MdibStorage mockStorage = mock(MdibStorage.class);
        final DescriptionPreprocessingSegment segment1 = mock(DescriptionPreprocessingSegment.class);
        final DescriptionPreprocessingSegment segment2 = mock(DescriptionPreprocessingSegment.class);
        final DescriptionPreprocessingSegment segment3 = mock(DescriptionPreprocessingSegment.class);
        when(segment1.beforeFirstModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment2.beforeFirstModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment3.beforeFirstModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));

        when(segment1.process(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment2.process(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment3.process(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));

        when(segment1.afterLastModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment2.afterLastModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment3.afterLastModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));

        final List<DescriptionPreprocessingSegment> segments = Arrays.asList(segment1, segment2, segment3);
        final MdibStoragePreprocessingChain chain = chainFactory.createMdibStoragePreprocessingChain(
                mockStorage,
                segments,
                mock(List.class));

        final String expectedHandle = "foobarHandle";
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(expectedHandle, MdsDescriptor.builder()).build());

        {
            // When there is a regular call to process description modifications
            chain.processDescriptionModifications(modifications);

            // Then expect every segment to be processed once
            for (DescriptionPreprocessingSegment segment : segments) {
                verify(segment, times(1)).process(modifications.getModifications(), mockStorage);
            }
        }

        {
            // When there is a call that causes an exception during processing of segment2
            final String expectedErrorMessage = "foobarMessage";
            doThrow(new Exception(expectedErrorMessage)).when(segment2)
                    .process(modifications.getModifications(), mockStorage);

            // Then expect a PreprocessingException to be thrown
            try {
                chain.processDescriptionModifications(modifications);
                Assertions.fail("segment2 did not throw an exception");
            } catch (PreprocessingException e) {
                Assertions.assertEquals(expectedErrorMessage, e.getMessage());
                Assertions.assertEquals(segment2.toString(), e.getSegment());
            }

            // Then expect segment3 not to be processed
            verify(segment3, times(1)) // still one interaction only
                    .process(modifications.getModifications(), mockStorage);
        }
    }

    @Test
    void processModificationsAddedDuringProcessing() throws Exception {
        // Given a preprocessing chain with 3 segments, where the 2nd segment adds a 2nd modification
        final MdibStorage mockStorage = mock(MdibStorage.class);
        final DescriptionPreprocessingSegment realSegment = new DescriptionPreprocessingSegment() {
            private boolean isFirstCall = true;

            @Override
            public List<MdibDescriptionModification> process(List<MdibDescriptionModification> allModifications,
                                                        MdibStorage storage) throws Exception {
                if (isFirstCall) {
                    isFirstCall = false;
                    allModifications = new ArrayList<>(allModifications);
                    allModifications.add(new MdibDescriptionModification(
                        MdibDescriptionModification.Type.INSERT,
                        MockModelFactory.createDescriptor("handle", VmdDescriptor.builder()).build(),
                        null
                    ));
                }
                return allModifications;
            }
        };
        final DescriptionPreprocessingSegment segment1 = mock(DescriptionPreprocessingSegment.class);
        final DescriptionPreprocessingSegment segment2 = spy(realSegment);
        final DescriptionPreprocessingSegment segment3 = mock(DescriptionPreprocessingSegment.class);

        when(segment1.beforeFirstModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment3.beforeFirstModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));

        when(segment1.process(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment3.process(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));

        when(segment1.afterLastModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));
        when(segment3.afterLastModification(any(), any(MdibStorage.class))).thenAnswer(invocation -> invocation.getArgument(0, List.class));

        final List<DescriptionPreprocessingSegment> segments = Arrays.asList(segment1, segment2, segment3);
        final MdibStoragePreprocessingChain chain = chainFactory.createMdibStoragePreprocessingChain(
                mockStorage,
                segments,
                mock(List.class));

        final String expectedHandle = "foobarHandle";
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(expectedHandle, MdsDescriptor.builder()).build());

        // When there is a regular call to process description modifications
        chain.processDescriptionModifications(modifications);

        // Then expect every segment to be processed once;
        for (DescriptionPreprocessingSegment segment : segments) {
            ArgumentCaptor<List<MdibDescriptionModification>> captor = ArgumentCaptor.forClass(List.class);
            verify(segment, times(1))
                    .process(captor.capture(), any());
            if (segment == segment1) {
                assertEquals(1, captor.getValue().size());
            } else if (segment == segment2) {
                assertEquals(1, captor.getValue().size());
            } else if (segment == segment3) {
                // last segment has received the added modification
                assertEquals(2, captor.getValue().size());
            }
        }
    }

    @Test
    void processStateModifications() throws Exception {
        // Given a preprocessing chain with 3 segments
        final MdibStorage mockStorage = mock(MdibStorage.class);
        final StatePreprocessingSegment segment1 = mock(StatePreprocessingSegment.class);
        final StatePreprocessingSegment segment2 = mock(StatePreprocessingSegment.class);
        final StatePreprocessingSegment segment3 = mock(StatePreprocessingSegment.class);
        final List<StatePreprocessingSegment> segments = Arrays.asList(segment1, segment2, segment3);
        final MdibStoragePreprocessingChain chain = chainFactory.createMdibStoragePreprocessingChain(
                mockStorage,
                mock(List.class),
                segments);

        final String expectedHandle = "foobarHandle";
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.METRIC)
                .add(MockModelFactory.createState(expectedHandle, NumericMetricState.builder()).build());

        {
            // When there is a regular call to process state modifications
            chain.processStateModifications(modifications);

            // Then expect every segment to be processed once
            for (StatePreprocessingSegment segment : segments) {
                verify(segment, times(1))
                        .process(modifications, mockStorage);
            }
        }

        {
            // When there is a call that causes an exception during processing of segment2
            final String expectedErrorMessage = "foobarMessage";
            doThrow(new Exception(expectedErrorMessage)).when(segment2)
                    .process(modifications, mockStorage);

            // Then expect a PreprocessingException to be thrown
            try {
                chain.processStateModifications(modifications);
                Assertions.fail("segment2 did not throw an exception");
            } catch (PreprocessingException e) {
                Assertions.assertEquals(expectedErrorMessage, e.getMessage());
                Assertions.assertEquals(segment2.toString(), e.getSegment());
            }

            // Then expect segment3 not to be processed
            verify(segment3, times(1)) // still one interaction only
                    .process(modifications, mockStorage);
        }
    }

    @Test
    void beforeAndAfterModificationOnDescriptionModifications() throws PreprocessingException {
        // Given a preprocessing chain with 2 segments
        final MdibStorage mockStorage = mock(MdibStorage.class);
        final DescriptionPreprocessingSegment segment1 = mock(DescriptionPreprocessingSegment.class);
        final DescriptionPreprocessingSegment segment2 = mock(DescriptionPreprocessingSegment.class);

        List<DescriptionPreprocessingSegment> segments = Arrays.asList(segment1, segment2);
        final MdibStoragePreprocessingChain chain = chainFactory.createMdibStoragePreprocessingChain(
                mockStorage,
                segments,
                mock(List.class));

        // When there is a regular call to process state modifications
        MdibDescriptionModifications expectedModifications = mock(MdibDescriptionModifications.class);
        chain.processDescriptionModifications(expectedModifications);

        // Then expect before first and after last modification callbacks to be triggered for each segment
        segments.forEach(segment -> {
            verify(segment, times(1)).beforeFirstModification(expectedModifications.getModifications(), mockStorage);
            verify(segment, times(1)).afterLastModification(expectedModifications.getModifications(), mockStorage);
        });
    }

    @Test
    void beforeAndAfterModificationOnStateModifications() throws PreprocessingException {
        // Given a preprocessing chain with 2 segments
        final MdibStorage mockStorage = mock(MdibStorage.class);
        final StatePreprocessingSegment segment1 = mock(StatePreprocessingSegment.class);
        final StatePreprocessingSegment segment2 = mock(StatePreprocessingSegment.class);

        List<StatePreprocessingSegment> segments = Arrays.asList(segment1, segment2);
        final MdibStoragePreprocessingChain chain = chainFactory.createMdibStoragePreprocessingChain(
                mockStorage,
                mock(List.class),
                segments);

        // When there is a regular call to process state modifications
        MdibStateModifications modifications = mock(MdibStateModifications.class);
        chain.processStateModifications(modifications);

        // Then expect before first and after last modification callbacks to be triggered for each segment
        segments.forEach(segment -> {
            verify(segment, times(1)).beforeFirstModification(modifications, mockStorage);
            verify(segment, times(1)).afterLastModification(modifications,mockStorage);
        });
    }
}