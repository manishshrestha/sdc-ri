package mdpws.provider.safety;
 
import org.junit.jupiter.api.Test;
import org.somda.sdc.mdpws.provider.safety.XPathBuilder;
import org.somda.sdc.mdpws.provider.safety.XPathCondition;
import org.somda.sdc.mdpws.provider.safety.XPathStep;

import javax.xml.namespace.QName; 
import java.util.Map; 
 
import static org.junit.jupiter.api.Assertions.assertEquals; 
import static org.junit.jupiter.api.Assertions.assertThrows; 
 
class XPathBuilderTest { 
    private static final Map<String, String> DEFAULT_NS_MAP = Map.of( 
            "http://ns1", "ns1", 
            "http://ns2", "ns2", 
            "http://ns3", "ns3" 
    ); 
 
    @Test 
    void buildXPathVariations() { 
        var builder = XPathBuilder.create(DEFAULT_NS_MAP);
        assertEquals("/@Foo", builder.getAttribute("Foo")); 
        assertEquals("/@ns2:Foo", builder.getAttribute(new QName("http://ns2", "Foo"))); 
        assertThrows(RuntimeException.class, () -> 
                builder.getAttribute(new QName("http://unknown", "Foo"))); 
 
        assertEquals("/Foo/Bar/text()", 
                builder.add(org.somda.sdc.mdpws.provider.safety.XPathStep.create("Foo")).add(org.somda.sdc.mdpws.provider.safety.XPathStep.create("Bar")).getElementText());
 
        builder.clear(); 
        assertEquals("/Foo/ns3:Bar/text()", 
                builder.add(org.somda.sdc.mdpws.provider.safety.XPathStep.create("Foo")).add(org.somda.sdc.mdpws.provider.safety.XPathStep.create(new QName("http://ns3", "Bar"))).getElementText());
 
        builder.clear(); 
        assertEquals("/Foo[4]/ns3:Bar/text()", 
                builder.add(org.somda.sdc.mdpws.provider.safety.XPathStep.create("Foo", org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromElementIndex(4)))
                        .add(org.somda.sdc.mdpws.provider.safety.XPathStep.create(new QName("http://ns3", "Bar"))).getElementText());
 
        builder.clear(); 
        assertEquals("/Foo[ns1:Attr='Te\"xt']/ns3:Bar/text()", 
                builder.add(org.somda.sdc.mdpws.provider.safety.XPathStep.create("Foo", org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(new QName("http://ns1", "Attr"), "Te\"xt")))
                        .add(org.somda.sdc.mdpws.provider.safety.XPathStep.create(new QName("http://ns3", "Bar"))).getElementText());
 
        builder.clear(); 
        assertEquals("/Foo[ns1:Attr=\"Text\"]/ns3:Bar/@Foo", 
                builder.add(org.somda.sdc.mdpws.provider.safety.XPathStep.create("Foo", org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(new QName("http://ns1", "Attr"), "Text")))
                        .add(org.somda.sdc.mdpws.provider.safety.XPathStep.create(new QName("http://ns3", "Bar"))).getAttribute("Foo"));
 
        builder.clear(); 
        assertEquals("/Foo[ns1:Attr=\"Te'xt\"]/ns3:Bar/@ns2:Foo", 
                builder.add(org.somda.sdc.mdpws.provider.safety.XPathStep.create("Foo", XPathCondition.createFromAttributeName(new QName("http://ns1", "Attr"), "Te'xt")))
                        .add(XPathStep.create(new QName("http://ns3", "Bar"))).getAttribute(new QName("http://ns2", "Foo")));
    } 
}