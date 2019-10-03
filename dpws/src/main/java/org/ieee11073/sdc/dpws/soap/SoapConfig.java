package org.ieee11073.sdc.dpws.soap;

import javax.xml.bind.JAXBContext;

/**
 * Configuration of the SOAP package.
 */
public class SoapConfig {

    /**
     * Sets the context path for JAXB marshalling and unmarshalling.
     * <p>
     * Internal context path elements will be added automatically.
     * This configuration item uses the same String format as used in {@link JAXBContext#newInstance(String)}.
     * <ul>
     * <li>Data type: {@linkplain String}
     * <li>Use: optional
     * </ul>
     */
    public static final String JAXB_CONTEXT_PATH = "SoapConfig.ContextPaths";
}
