package org.somda.sdc.dpws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.EnumMap;

import javax.inject.Named;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@linkplain CommunicationLogSink}.
 */
public class CommunicationLogSinkImpl implements CommunicationLogSink {

    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogSinkImpl.class);

    private static final String SUFFIX = ".xml";

    private EnumMap<BranchPath, File> dirMapping;

    @Inject
    CommunicationLogSinkImpl(@Named(DpwsConfig.COMMUNICATION_LOG_SINK_DIRECTORY) File logDirectory) {
        
        this.dirMapping = new EnumMap<BranchPath, File>(BranchPath.class);

        for (BranchPath subDir : BranchPath.values()) {
            File subDirFile = new File(logDirectory, subDir.toString());

            if (!subDirFile.exists() && !subDirFile.mkdirs()) {
                this.dirMapping.put(subDir, null);

                LOG.warn("Could not create the communication log directory '{}{}{}'.", logDirectory.getAbsolutePath(),
                        File.separator, subDirFile.getName());
            } else {
                this.dirMapping.put(subDir, subDirFile);
            }
        }

    }

    public OutputStream createBranch(BranchPath path, String key) {

        File dir = dirMapping.get(path);

        if (dir != null) {
            try {
                return new FileOutputStream(dir.getAbsolutePath() + File.separator + key + SUFFIX);

            } catch (FileNotFoundException e) {
                LOG.warn("Could not open communication log file.", e);
            }
        }

        return OutputStream.nullOutputStream();

    }

}
