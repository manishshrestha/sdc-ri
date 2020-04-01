package mdpws.provider.safety;
 
import org.junit.jupiter.api.Test;
import org.somda.sdc.mdpws.provider.safety.XPathCondition;

import javax.xml.namespace.QName; 
import java.math.BigDecimal; 
import java.util.Map; 
 
import static org.junit.jupiter.api.Assertions.assertEquals; 
 
class XPathConditionTest { 
    private static final String EXPECTED_ATTRIBUTE_NAME = "Attr"; 
    private static final Map<String, String> DEFAULT_NS_MAP = Map.of( 
            "http://ns1", "ns1", 
            "http://ns2", "ns2", 
            "http://ns3", "ns3" 
    ); 
 
    @Test 
    void createFromElementIndex() { 
        int count = 100; 
        for (int i = 0; i >= count * -1; --i) { 
            var condition = org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromElementIndex(i);
            assertEquals("", condition.createXPathPart(DEFAULT_NS_MAP)); 
        } 
        for (int i = 1; i <= count; ++i) { 
            var condition = org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromElementIndex(i);
            assertEquals(String.format("[%s]", i), condition.createXPathPart(DEFAULT_NS_MAP)); 
        } 
    } 
 
    @Test 
    void createFromAttributeNameBasedOnInteger() { 
        assertEquals(String.format("[%s=%s]", EXPECTED_ATTRIBUTE_NAME, -100), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME, -100).createXPathPart(DEFAULT_NS_MAP));
        assertEquals(String.format("[%s=%s]", EXPECTED_ATTRIBUTE_NAME, 0), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME, 0).createXPathPart(DEFAULT_NS_MAP));
        assertEquals(String.format("[%s=%s]", EXPECTED_ATTRIBUTE_NAME, 100), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME, 100).createXPathPart(DEFAULT_NS_MAP));
    } 
 
    @Test 
    void createFromAttributeNameBasedOnDecimal() { 
        assertEquals(String.format("[%s=%s]", EXPECTED_ATTRIBUTE_NAME, -100.123), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME, BigDecimal.valueOf(-100.123)).createXPathPart(DEFAULT_NS_MAP));
        assertEquals(String.format("[%s=%s]", EXPECTED_ATTRIBUTE_NAME, 0), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME, BigDecimal.valueOf(0)).createXPathPart(DEFAULT_NS_MAP));
        assertEquals(String.format("[%s=%s]", EXPECTED_ATTRIBUTE_NAME, 100.234), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME, BigDecimal.valueOf(100.234)).createXPathPart(DEFAULT_NS_MAP));
    } 
 
    @Test 
    void createFromAttributeBasedOnLiteral() { 
        assertEquals(String.format("[%s=\"\"]", EXPECTED_ATTRIBUTE_NAME), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "").createXPathPart(DEFAULT_NS_MAP)); 
 
        assertEquals(String.format("[%s=\"%s\"]", EXPECTED_ATTRIBUTE_NAME, "Foobar"), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "Foobar").createXPathPart(DEFAULT_NS_MAP)); 
        assertEquals(String.format("[%s=\"%s\"]", EXPECTED_ATTRIBUTE_NAME, "Foo'bar"), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "Foo'bar").createXPathPart(DEFAULT_NS_MAP)); 
        assertEquals(String.format("[%s=\"%s\"]", EXPECTED_ATTRIBUTE_NAME, "'Foo'bar'Foo'bar'"), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "'Foo'bar'Foo'bar'").createXPathPart(DEFAULT_NS_MAP)); 
        assertEquals(String.format("[%s='%s']", EXPECTED_ATTRIBUTE_NAME, "Foo\"bar"), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "Foo\"bar").createXPathPart(DEFAULT_NS_MAP)); 
        assertEquals(String.format("[%s='%s']", EXPECTED_ATTRIBUTE_NAME, "\"Foo\"bar\"Foo\"bar\""), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "\"Foo\"bar\"Foo\"bar\"").createXPathPart(DEFAULT_NS_MAP)); 
 
        assertEquals(String.format("[%s=concat(%s)]", EXPECTED_ATTRIBUTE_NAME, "'Föö\"bär',\"\'F%&bèß\""), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "Föö\"bär\'F%&bèß").createXPathPart(DEFAULT_NS_MAP)); 
 
        assertEquals(String.format("[%s=concat(%s)]", EXPECTED_ATTRIBUTE_NAME, "'\"',\"'\",'\"',\"'\""), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "\"'\"'").createXPathPart(DEFAULT_NS_MAP)); 
 
        assertEquals(String.format("[%s=concat(%s)]", EXPECTED_ATTRIBUTE_NAME, "\"That's an \",'\"example\" text'"), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "That's an \"example\" text").createXPathPart(DEFAULT_NS_MAP)); 
 
        assertEquals(String.format("[%s=concat(%s)]", EXPECTED_ATTRIBUTE_NAME, "'\"\"\"\"',\"'\",'\"\"'"), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "\"\"\"\"'\"\"").createXPathPart(DEFAULT_NS_MAP)); 
 
        assertEquals(String.format("[%s=concat(%s)]", EXPECTED_ATTRIBUTE_NAME, "\"'''\",'\"',\"''''\""), 
                org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(EXPECTED_ATTRIBUTE_NAME,
                        "'''\"''''").createXPathPart(DEFAULT_NS_MAP)); 
    } 
 
    @Test 
    void namespaceMappings() { 
        { 
            QName unknownNamespace = new QName(EXPECTED_ATTRIBUTE_NAME, "http://unknown"); 
 
            assertEquals("", 
                    org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(unknownNamespace, "Foobar").createXPathPart(DEFAULT_NS_MAP));
        } 
 
        { 
            QName knownNamespace = new QName("http://ns1", EXPECTED_ATTRIBUTE_NAME); 
            assertEquals(String.format("[%s:%s=\"%s\"]", "ns1", EXPECTED_ATTRIBUTE_NAME, "Foobar"), 
                    org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(knownNamespace, "Foobar").createXPathPart(DEFAULT_NS_MAP));
        } 
 
        { 
            QName knownNamespace = new QName("http://ns1", EXPECTED_ATTRIBUTE_NAME); 
            assertEquals(String.format("[%s:%s=%s]", "ns1", EXPECTED_ATTRIBUTE_NAME, 100), 
                    org.somda.sdc.mdpws.provider.safety.XPathCondition.createFromAttributeName(knownNamespace, 100).createXPathPart(DEFAULT_NS_MAP));
        } 
 
        { 
            QName knownNamespace = new QName("http://ns1", EXPECTED_ATTRIBUTE_NAME); 
            assertEquals(String.format("[%s:%s=%s]", "ns1", EXPECTED_ATTRIBUTE_NAME, 10.0), 
                    XPathCondition.createFromAttributeName(knownNamespace, BigDecimal.valueOf(10.0)).createXPathPart(DEFAULT_NS_MAP));
        } 
    } 
}