package org.somda.sdc.proto.addressing;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.proto.model.addressing.AddressingTypes;

import javax.annotation.Nullable;

public class AddressingValidator {
    private final MessageDuplicateDetection messageDuplicateDetection;

    @Inject
    AddressingValidator(@Assisted MessageDuplicateDetection messageDuplicateDetection) {
        this.messageDuplicateDetection = messageDuplicateDetection;
    }

    public Validator validate(AddressingTypes.Addressing addressing) {
        return new Validator() {
            @Override
            public Validator validateAction(@Nullable String action) throws ValidationException {
                if (action == null) {
                    if (addressing.getAction() != null) {
                        throw new ValidationException("No action found, but required");
                    } else {
                        return this;
                    }
                }
                if (!action.equals(addressing.getAction())) {
                    throw new ValidationException(String.format("Expected action differs from actual action: %s != %s",
                            addressing.getAction(),
                            action));
                }
                return this;
            }

            @Override
            public Validator validateMessageId() throws ValidationException {
                var messageId = addressing.getMessageId();
                if (messageId == null) {
                    throw new ValidationException("No message ID found, but required");
                }
                if (messageDuplicateDetection.isDuplicate(messageId)) {
                    throw new ValidationException(String.format("Message ID %s already seen", messageId));
                }
                return this;
            }
        };
    }

    public interface Validator {
        Validator validateAction(@Nullable String action) throws ValidationException;

        Validator validateMessageId() throws ValidationException;
    }
}
