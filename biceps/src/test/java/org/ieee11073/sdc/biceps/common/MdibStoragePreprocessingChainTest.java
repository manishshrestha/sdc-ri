package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.UnitTestUtil;
import org.ieee11073.sdc.biceps.common.factory.MdibStoragePreprocessingChainFactory;
import org.ieee11073.sdc.biceps.model.participant.MdsDescriptor;
import org.ieee11073.sdc.biceps.model.participant.NumericMetricState;
import org.ieee11073.sdc.biceps.model.participant.VmdDescriptor;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

class MdibStoragePreprocessingChainTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private MdibStoragePreprocessingChainFactory chainFactory;

    @BeforeEach
    void beforeAll() {
        chainFactory = UT.getInjector().getInstance(MdibStoragePreprocessingChainFactory.class);
    }

    @Test
    void processDescriptionModifications() throws Exception {
        // Given a preprocessing chain with 3 segments
        final MdibStorage mockStorage = mock(MdibStorage.class);
        final DescriptionPreprocessingSegment segment1 = mock(DescriptionPreprocessingSegment.class);
        final DescriptionPreprocessingSegment segment2 = mock(DescriptionPreprocessingSegment.class);
        final DescriptionPreprocessingSegment segment3 = mock(DescriptionPreprocessingSegment.class);
        final MdibStoragePreprocessingChain chain = chainFactory.createMdibStoragePreprocessingChain(
                mockStorage,
                Arrays.asList(segment1, segment2, segment3),
                mock(List.class));

        final String expectedHandle = "foobarHandle";
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(expectedHandle, MdsDescriptor.class));

        {
            // When there is a regular call to process description modifications
            chain.processDescriptionModifications(modifications);

            // Then expect every segment to be processed once
            verify(segment1, times(1))
                    .process(modifications, modifications.getModifications().get(0), mockStorage);

            verify(segment2, times(1))
                    .process(modifications, modifications.getModifications().get(0), mockStorage);

            verify(segment3, times(1))
                    .process(modifications, modifications.getModifications().get(0), mockStorage);
        }

        {
            // When there is a call that causes an exception during processing of segment2
            final String expectedErrorMessage = "foobarMessage";
            doThrow(new Exception(expectedErrorMessage)).when(segment2)
                    .process(modifications, modifications.getModifications().get(0), mockStorage);

            // Then expect a PreprocessingException to be thrown
            try {
                chain.processDescriptionModifications(modifications);
                Assertions.fail("segment2 did not throw an exception");
            } catch (PreprocessingException e) {
                Assertions.assertEquals(expectedErrorMessage, e.getMessage());
                Assertions.assertEquals(expectedHandle, e.getHandle());
                Assertions.assertEquals(segment2.toString(), e.getSegment());
            }

            // Then expect segment3 not to be processed
            verify(segment3, times(1)) // still one interaction only
                    .process(modifications, modifications.getModifications().get(0), mockStorage);
        }
    }

    @Test
    void processModificationsAddedDuringProcessing() throws Exception {
        // Given a preprocessing chain with 3 segments, where the 2nd segment adds a 2nd modification
        final MdibStorage mockStorage = mock(MdibStorage.class);
        final DescriptionPreprocessingSegment realSegment = new DescriptionPreprocessingSegment() {
            private boolean isFirstCall = true;

            @Override
            public void process(MdibDescriptionModifications allModifications,
                                MdibDescriptionModification currentModification,
                                MdibStorage storage) throws Exception {
                if (isFirstCall) {
                    isFirstCall = false;
                    allModifications.insert(MockModelFactory.createDescriptor("handle", VmdDescriptor.class));
                }
            }
        };
        final DescriptionPreprocessingSegment segment1 = mock(DescriptionPreprocessingSegment.class);
        final DescriptionPreprocessingSegment segment2 = spy(realSegment);
        final DescriptionPreprocessingSegment segment3 = mock(DescriptionPreprocessingSegment.class);

        final MdibStoragePreprocessingChain chain = chainFactory.createMdibStoragePreprocessingChain(
                mockStorage,
                Arrays.asList(segment1, segment2, segment3),
                mock(List.class));

        final String expectedHandle = "foobarHandle";
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(expectedHandle, MdsDescriptor.class));

        // When there is a regular call to process description modifications
        chain.processDescriptionModifications(modifications);

        // Then expect every segment to be processed twice; 1x on 1st, 1x on 2nd modification
        verify(segment1, times(1))
                .process(modifications, modifications.getModifications().get(0), mockStorage);

        verify(segment2, times(1))
                .process(modifications, modifications.getModifications().get(0), mockStorage);

        verify(segment3, times(1))
                .process(modifications, modifications.getModifications().get(0), mockStorage);

        verify(segment1, times(1))
                .process(modifications, modifications.getModifications().get(1), mockStorage);

        verify(segment2, times(1))
                .process(modifications, modifications.getModifications().get(1), mockStorage);

        verify(segment3, times(1))
                .process(modifications, modifications.getModifications().get(1), mockStorage);
    }

    @Test
    void processStateModifications() throws Exception {
        // Given a preprocessing chain with 3 segments
        final MdibStorage mockStorage = mock(MdibStorage.class);
        final StatePreprocessingSegment segment1 = mock(StatePreprocessingSegment.class);
        final StatePreprocessingSegment segment2 = mock(StatePreprocessingSegment.class);
        final StatePreprocessingSegment segment3 = mock(StatePreprocessingSegment.class);
        final MdibStoragePreprocessingChain chain = chainFactory.createMdibStoragePreprocessingChain(
                mockStorage,
                mock(List.class),
                Arrays.asList(segment1, segment2, segment3));

        final String expectedHandle = "foobarHandle";
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.METRIC)
                .add(MockModelFactory.createState(expectedHandle, NumericMetricState.class));

        {
            // When there is a regular call to process state modifications
            chain.processStateModifications(modifications);

            // Then expect every segment to be processed once
            verify(segment1, times(1))
                    .process(modifications.getStates().get(0), mockStorage);

            verify(segment2, times(1))
                    .process(modifications.getStates().get(0), mockStorage);

            verify(segment3, times(1))
                    .process(modifications.getStates().get(0), mockStorage);
        }

        {
            // When there is a call that causes an exception during processing of segment2
            final String expectedErrorMessage = "foobarMessage";
            doThrow(new Exception(expectedErrorMessage)).when(segment2)
                    .process(modifications.getStates().get(0), mockStorage);

            // Then expect a PreprocessingException to be thrown
            try {
                chain.processStateModifications(modifications);
                Assertions.fail("segment2 did not throw an exception");
            } catch (PreprocessingException e) {
                Assertions.assertEquals(expectedErrorMessage, e.getMessage());
                Assertions.assertEquals(expectedHandle, e.getHandle());
                Assertions.assertEquals(segment2.toString(), e.getSegment());
            }

            // Then expect segment3 not to be processed
            verify(segment3, times(1)) // still one interaction only
                    .process(modifications.getStates().get(0), mockStorage);
        }
    }
}