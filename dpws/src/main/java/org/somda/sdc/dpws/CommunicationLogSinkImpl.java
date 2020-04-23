package org.somda.sdc.dpws;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.helper.CommunicationLogFileName;
import org.somda.sdc.dpws.helper.CommunicationLogFileOutputStream;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;

import javax.inject.Named;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Default implementation of {@linkplain CommunicationLogSink}.
 */
public class CommunicationLogSinkImpl implements CommunicationLogSink {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogSinkImpl.class);
    private final Boolean createHttpHeaders;

    private EnumMap<CommunicationLog.TransportType, File> dirMapping;

    @Inject
    CommunicationLogSinkImpl(@Named(DpwsConfig.COMMUNICATION_LOG_SINK_DIRECTORY) File logDirectory,
                             @Named(DpwsConfig.COMMUNICATION_LOG_WITH_HTTP_HEADERS) Boolean createHttpHeaders) {
        this.createHttpHeaders = createHttpHeaders;

        this.dirMapping = new EnumMap<>(CommunicationLog.TransportType.class);

        for (CommunicationLog.TransportType transportType : CommunicationLog.TransportType.values()) {
            File subDirFile = new File(logDirectory, transportType.toString());

            if (!subDirFile.exists() && !subDirFile.mkdirs()) {
                this.dirMapping.put(transportType, null);

                LOG.warn("Could not create the communication log directory '{}{}{}'", logDirectory.getAbsolutePath(),
                        File.separator, subDirFile.getName());
            } else {
                this.dirMapping.put(transportType, subDirFile);
            }
        }

    }

    @Deprecated
    public OutputStream getTargetStream(CommunicationLog.TransportType transportType,
                                        CommunicationLog.Direction direction,
                                        CommunicationContext communicationContext) {
        return createTargetStream(transportType, direction, communicationContext);
    }

    @Override
    public OutputStream createTargetStream(CommunicationLog.TransportType transportType,
                                           CommunicationLog.Direction direction,
                                           CommunicationContext communicationContext) {
        var outputStream = OutputStream.nullOutputStream();

        var dir = dirMapping.get(transportType);
        if (dir == null) {
            LOG.warn("The directory for the given transport type was not configured.");
            return outputStream;
        }

        var fileNamePrefix = CommunicationLogFileName.create(direction.toString(), communicationContext);

        if (createHttpHeaders) {
            var headerPath = CommunicationLogFileName.appendHttpHeaderSuffix(dir.getAbsolutePath() +
                    File.separator + fileNamePrefix);

            // if message is http, we can store header info too
            if (communicationContext.getApplicationInfo() instanceof HttpApplicationInfo) {
                var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
                try (OutputStream headerFile = new FileOutputStream(headerPath)) {
                    for (Map.Entry<String, String> entry : appInfo.getHeaders().entries()) {
                        String targetString;
                        if (entry.getValue() == null) {
                            targetString = String.format("%s\n", entry.getKey());
                        } else {
                            targetString = String.format("%s = %s\n", entry.getKey(), entry.getValue());
                        }
                        headerFile.write(targetString.getBytes());
                    }
                } catch (IOException e) {
                    LOG.error("Could not write headers to header file {}",
                            CommunicationLogFileName.appendHttpHeaderSuffix(headerPath));
                }
            }
        }

        return new CommunicationLogFileOutputStream(dir, fileNamePrefix);
    }
}
