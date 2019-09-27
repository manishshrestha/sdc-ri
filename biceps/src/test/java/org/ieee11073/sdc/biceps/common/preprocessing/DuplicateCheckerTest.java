package org.ieee11073.sdc.biceps.common.preprocessing;

import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.model.participant.MdsDescriptor;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

class DuplicateCheckerTest {
    @Test
    void process() throws Exception {
        // Given a duplicate detector
        final DuplicateChecker duplicateChecker = new DuplicateChecker();

        final String expectedNonExistingHandle = "nonExistingHandle";
        final String expectedExistingHandle = "existingHandle";
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(expectedNonExistingHandle, MdsDescriptor.class))
                .insert(MockModelFactory.createDescriptor(expectedExistingHandle, MdsDescriptor.class))
                .update(MockModelFactory.createDescriptor(expectedExistingHandle, MdsDescriptor.class));

        final MdibStorage mdibStorage = Mockito.mock(MdibStorage.class);
        Mockito.when(mdibStorage.getEntity(expectedNonExistingHandle)).thenReturn(Optional.empty());
        Mockito.when(mdibStorage.getEntity(expectedExistingHandle)).thenReturn(Optional.of(Mockito.mock(MdibEntity.class)));

        // When there is no duplication detected
        // Then expect the detector to continue
        assertDoesNotThrow(() ->
                duplicateChecker.process(modifications, modifications.getModifications().get(0), mdibStorage));

        // When there is a duplication detected
        // Then expect the detector to throw an exception
        assertThrows(HandleDuplicatedException.class, () ->
                duplicateChecker.process(modifications, modifications.getModifications().get(1), mdibStorage));

        // When there is a potential duplicate that is not going to be inserted (updated, deleted)
        // Then expect the detector to continue
        assertDoesNotThrow(() ->
                duplicateChecker.process(modifications, modifications.getModifications().get(2), mdibStorage));
    }
}