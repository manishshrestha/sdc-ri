package org.somda.sdc.proto.addressing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.proto.addressing.AddressingValidator;
import org.somda.sdc.proto.addressing.MessageDuplicateDetection;

public interface AddressingValidatorFactory {
    AddressingValidator create(@Assisted MessageDuplicateDetection messageDuplicateDetection);
}
