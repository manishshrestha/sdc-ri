package org.ieee11073.sdc.biceps.common.access;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.biceps.common.CommonConfig;
import org.ieee11073.sdc.common.helper.ObjectUtil;

public class CopyManager {
    private final ObjectUtil objectUtil;
    private final Boolean copyInput;
    private final Boolean copyOutput;

    @Inject
    CopyManager(ObjectUtil objectUtil,
                @Named(CommonConfig.COPY_MDIB_INPUT) Boolean copyInput,
                @Named(CommonConfig.COPY_MDIB_OUTPUT) Boolean copyOutput) {
        this.objectUtil = objectUtil;
        this.copyInput = copyInput;
        this.copyOutput = copyOutput;
    }

    public <T> T processInput(T input) {
        return doDeepCopyIfConfigured(copyInput, input);
    }

    public <T> T processOutput(T output) {
        return doDeepCopyIfConfigured(copyOutput, output);
    }

    private <T> T doDeepCopyIfConfigured(boolean doCopy, T data) {
        if (doCopy) {
            return objectUtil.deepCopy(data);
        } else{
            return data;
        }
    }
}
