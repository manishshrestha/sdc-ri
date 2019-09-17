package org.ieee11073.sdc.biceps.provider.preprocessing;

import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.model.participant.MdsDescriptor;
import org.ieee11073.sdc.biceps.model.participant.NumericMetricState;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuplicateDetectorTest {
    @Test
    void process() throws Exception {
        // Given a duplicate detector
        final DuplicateDetector duplicateDetector = new DuplicateDetector();

        final String expectedNonExistingHandle = "nonExistingHandle";
        final String expectedExistingHandle = "existingHandle";
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(expectedNonExistingHandle, MdsDescriptor.class))
                .insert(MockModelFactory.createDescriptor(expectedExistingHandle, MdsDescriptor.class))
                .update(MockModelFactory.createDescriptor(expectedExistingHandle, MdsDescriptor.class));

        final MdibStorage mdibStorage = mock(MdibStorage.class);
        when(mdibStorage.getEntity(expectedNonExistingHandle)).thenReturn(Optional.empty());
        when(mdibStorage.getEntity(expectedExistingHandle)).thenReturn(Optional.of(mock(MdibEntity.class)));

        // When there is no duplication detected
        duplicateDetector.process(modifications, modifications.getModifications().get(0), mdibStorage);

        // Then expect the detector to continue

        // When there is a duplication detected
        try {
            duplicateDetector.process(modifications, modifications.getModifications().get(1), mdibStorage);
            Assertions.fail("duplicated handle not detected");
        } catch (HandleDuplicatedException e) {
            // Then expect the detector to throw an exception
        }

        // When there is a potential duplicate that is not going to be inserted (updated, deleted)
        duplicateDetector.process(modifications, modifications.getModifications().get(2), mdibStorage);

        // Then expect the detector to continue
    }
}