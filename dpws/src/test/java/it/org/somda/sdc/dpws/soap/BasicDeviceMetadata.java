package it.org.somda.sdc.dpws.soap;

import javax.xml.namespace.QName;
import java.net.URI;

public class BasicDeviceMetadata {
    public static final URI SCOPE_1 = URI.create("http://integration-test-scope1");
    public static final URI SCOPE_2 = URI.create("http://integration-test-scope2");

    public static final QName QNAME_1 = new QName("http://type-ns", "integration-test-type1");
    public static final QName QNAME_2 = new QName("http://type-ns", "integration-test-type2");
}
