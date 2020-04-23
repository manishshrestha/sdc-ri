package org.somda.sdc.dpws.helper;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@linkplain OutputStream} that buffers written bytes and performs post-processing.
 * <p>
 * This stream implementation buffers written output to a byte array and writes a file once the stream is closed.
 * During the post-processing step the {@linkplain CommunicationLogFileOutputStream} does the following:
 * <ul>
 * <li>Tries to extract the SOAP action and writes the last part of the action to the file name in order to facilitate
 * users to get an idea which contents are enclosed by a communication log file.
 * <li>Pretty-prints the XML data.
 * </ul>
 */
public class CommunicationLogFileOutputStream extends OutputStream {
    private final static CommunicationLogSoapXmlUtils SOAP_UTILS = new CommunicationLogSoapXmlUtils();

    private final File targetDirectory;
    private final String fileNamePrefix;
    private ByteArrayOutputStream outputStream;
    private AtomicBoolean closed = new AtomicBoolean(false);

    public CommunicationLogFileOutputStream(File targetDirectory,
                                            String fileNamePrefix) {
        this.fileNamePrefix = fileNamePrefix;
        this.targetDirectory = targetDirectory;
        this.outputStream = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) {
        outputStream.write(b);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        // Only close once and avoid to accidentally write the commlog file multiple times
        if (closed.getAndSet(true)) {
            return;
        }

        outputStream.close();

        var xmlDoc = SOAP_UTILS.prettyPrint(outputStream.toByteArray());
        var name = SOAP_UTILS.makeNameElement(xmlDoc);

        try (var fileOutputStream = new FileOutputStream(targetDirectory.getAbsolutePath() + File.separator +
                CommunicationLogFileName.appendSoapSuffix(CommunicationLogFileName.append(fileNamePrefix, name)))) {
            fileOutputStream.write(xmlDoc);
        }
    }
}
