package org.somda.sdc.dpws;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.apache.commons.io.output.TeeOutputStream;

import javax.inject.Named;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Default implementation of {@linkplain CommunicationLog}.
 */
public class CommunicationLogImpl implements CommunicationLog {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogImpl.class);

    private File udpSubDirectory;
    private File httpSubDirectory;

    private static final String SEPARATOR = "_";
    private static final String SUFFIX = ".xml";

    @Inject
    CommunicationLogImpl(@Named(DpwsConfig.COMMUNICATION_LOG_DIRECTORY) File logDirectory) {

	File udpSubDirectory = new File(logDirectory, "udp");
	File httpSubDirectory = new File(logDirectory, "http");

	if ((!udpSubDirectory.exists() && !udpSubDirectory.mkdirs())
		|| (!httpSubDirectory.exists() && !httpSubDirectory.mkdirs())) {
	    this.udpSubDirectory = null;
	    this.httpSubDirectory = null;

	    LOG.warn("Could not create communication log directories '{}'", logDirectory.getAbsolutePath());
	} else {
	    this.udpSubDirectory = udpSubDirectory;
	    this.httpSubDirectory = httpSubDirectory;
	}
    }

    @Override
    public TeeOutputStream logHttpMessage(HttpDirection direction, String address, Integer port,
	    OutputStream httpMessage) {

	OutputStream log_file = getFileOutStream(this.httpSubDirectory, makeName(direction.toString(), address, port));

	return new TeeOutputStream(httpMessage, log_file);

    }

    @Override
    public InputStream logHttpMessage(HttpDirection direction, String address, Integer port, InputStream httpMessage) {
	return writeLogFile(this.httpSubDirectory, makeName(direction.toString(), address, port), httpMessage);
    }

    @Override
    public void logUdpMessage(UdpDirection direction, String destinationAddress, Integer destinationPort,
	    UdpMessage udpMessage) {
	writeLogFile(this.udpSubDirectory, makeName(direction.toString(), destinationAddress, destinationPort),
		new ByteArrayInputStream(udpMessage.getData(), 0, udpMessage.getLength()));
    }

    private InputStream writeLogFile(File subDir, String filename, InputStream inputStream) {

	try {
	    final byte[] bytes = ByteStreams.toByteArray(inputStream);

	    new ByteArrayInputStream(bytes).transferTo(getFileOutStream(subDir, filename));

	    return new ByteArrayInputStream(bytes);

	} catch (IOException e) {
	    LOG.warn("Could not write to communication log file", e);
	}

	return inputStream;
    }

    private OutputStream getFileOutStream(File subDir, String filename) {

	if (subDir != null) {
	    try {
		return new FileOutputStream(subDir.getAbsolutePath() + File.separator + filename);

	    } catch (FileNotFoundException e) {
		LOG.warn("Could not open communication log file", e);
	    }
	}

	return OutputStream.nullOutputStream();

    }

    private String makeName(String direction, String destinationAddress, Integer destinationPort) {
	LocalTime date = LocalTime.now();
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH-mm-ss-SSS");
	return System.nanoTime() + SEPARATOR + date.format(formatter) + SEPARATOR + direction + SEPARATOR
		+ destinationAddress + SEPARATOR + destinationPort + SUFFIX;
    }

    /**
     * UDP direction enumeration.
     */
    public enum UdpDirection {
	INBOUND("ibound-udp"), OUTBOUND("obound-udp");

	private final String stringRepresentation;

	UdpDirection(String stringRepresentation) {
	    this.stringRepresentation = stringRepresentation;
	}

	@Override
	public String toString() {
	    return stringRepresentation;
	}
    }

    /**
     * HTTP direction enumeration.
     */
    public enum HttpDirection {
	INBOUND_REQUEST("ibound-http-request"), INBOUND_RESPONSE("ibound-http-response"),
	OUTBOUND_REQUEST("obound-http-request"), OUTBOUND_RESPONSE("obound-http-response");

	private final String stringRepresentation;

	HttpDirection(String stringRepresentation) {
	    this.stringRepresentation = stringRepresentation;
	}

	@Override
	public String toString() {
	    return stringRepresentation;
	}
    }
}
