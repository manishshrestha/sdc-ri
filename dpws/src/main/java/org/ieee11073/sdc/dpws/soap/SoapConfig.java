package org.ieee11073.sdc.dpws.soap;

import javax.xml.bind.JAXBContext;

/**
 * Configuration of SOAP package.
 */
public class SoapConfig {

    /**
     * Set context path for JAXB marshalling and unmarshalling.
     *
     * Internal context path elements will be added automatically.
     * This configuration item uses the same String format as used in {@link JAXBContext#newInstance(String)}.
     *
     * - Data type: {@linkplain String}
     * - Use: optional
     */
    public static final String JAXB_CONTEXT_PATH = "SoapConfig.ContextPaths";
}
