package org.somda.sdc.dpws;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;

import javax.inject.Named;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Default implementation of {@linkplain CommunicationLogSink}.
 */
public class CommunicationLogSinkImpl implements CommunicationLogSink {

    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogSinkImpl.class);

    private static final String SEPARATOR = "_";
    private static final String SUFFIX = ".xml";
    private static final String HEADER_SUFFIX = SEPARATOR + "HEADER.txt";

    private EnumMap<CommunicationLog.TransportType, File> dirMapping;

    @Inject
    CommunicationLogSinkImpl(@Named(DpwsConfig.COMMUNICATION_LOG_SINK_DIRECTORY) File logDirectory) {

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
    public OutputStream getTargetStream(CommunicationLog.TransportType transportType, CommunicationLog.Direction direction, CommunicationContext communicationContext) {
        return createTargetStream(transportType, direction, communicationContext);
    }

    public OutputStream createTargetStream(CommunicationLog.TransportType transportType, CommunicationLog.Direction direction, CommunicationContext communicationContext) {

        File dir = dirMapping.get(transportType);

        var destAddr = communicationContext.getTransportInfo().getRemoteAddress().orElse("UNKNOWN_ADDRESS");
        var destPort = communicationContext.getTransportInfo().getRemotePort().orElse(-1);

        if (dir != null) {
            try {
                var basePath = dir.getAbsolutePath() + File.separator + makeName(direction.toString(), destAddr, destPort);

                // if message is http, we can store header info too
                if (communicationContext.getApplicationInfo() instanceof HttpApplicationInfo) {
                    var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
                    try (OutputStream headerFile = new FileOutputStream(basePath + HEADER_SUFFIX)) {
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
                        LOG.error("Could not write headers to header file {}", basePath + HEADER_SUFFIX);
                    }
                }

                return new FileOutputStream(basePath + SUFFIX);

            } catch (FileNotFoundException e) {
                LOG.warn("Could not open communication log file", e);
            }
        }

        LOG.warn("The directory for the given transport type was not configured.");

        return OutputStream.nullOutputStream();

    }

    private String makeName(String direction, String destinationAddress, Integer destinationPort) {
        LocalTime date = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH-mm-ss-SSS");
        return System.nanoTime() + SEPARATOR + date.format(formatter) + SEPARATOR + direction + SEPARATOR
                + destinationAddress + SEPARATOR + destinationPort;
    }
}
