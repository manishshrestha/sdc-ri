package mdpws.provider.safety;
 
import org.junit.jupiter.api.Test;
import org.somda.sdc.mdpws.provider.safety.XPathCondition;
import org.somda.sdc.mdpws.provider.safety.XPathStep;

import javax.xml.namespace.QName; 
import java.util.Map; 
 
import static org.junit.jupiter.api.Assertions.assertEquals; 
import static org.junit.jupiter.api.Assertions.assertThrows; 
 
class XPathStepTest { 
    private static final String EXPECTED_ELEMENT_NAME = "Elem"; 
    private static final Map<String, String> DEFAULT_NS_MAP = Map.of( 
            "http://ns1", "ns1", 
            "http://ns2", "ns2", 
            "http://ns3", "ns3" 
    ); 
 
    @Test 
    void createWithNonColonizedName() { 
        var expectedName = "Test"; 
        { 
            assertEquals(String.format("/%s", expectedName), 
                    org.somda.sdc.mdpws.provider.safety.XPathStep.create(expectedName).createXPathPart(DEFAULT_NS_MAP));
        } 
        { 
            var expectedIndex = 10; 
            var condition = XPathCondition.createFromElementIndex(expectedIndex);
            assertEquals(String.format("/%s[%s]", expectedName, expectedIndex), 
                    org.somda.sdc.mdpws.provider.safety.XPathStep.create(expectedName, condition).createXPathPart(DEFAULT_NS_MAP));
        } 
    } 
 
    @Test 
    void createWithColonizedName() { 
        var expectedPrefix = "ns1"; 
        var expectedName = new QName("http://ns1", EXPECTED_ELEMENT_NAME); 
        { 
            assertEquals(String.format("/%s:%s", expectedPrefix, EXPECTED_ELEMENT_NAME), 
                    org.somda.sdc.mdpws.provider.safety.XPathStep.create(expectedName).createXPathPart(DEFAULT_NS_MAP));
        } 
        { 
            var expectedIndex = 10; 
            var condition = XPathCondition.createFromElementIndex(expectedIndex); 
            assertEquals(String.format("/%s:%s[%s]", expectedPrefix, EXPECTED_ELEMENT_NAME, expectedIndex), 
                    org.somda.sdc.mdpws.provider.safety.XPathStep.create(expectedName, condition).createXPathPart(DEFAULT_NS_MAP));
        } 
    } 
 
    @Test 
    void namespaceMappings() { 
        { 
            QName unknownNamespace = new QName(EXPECTED_ELEMENT_NAME, "http://unknown"); 
            assertThrows(RuntimeException.class, () -> org.somda.sdc.mdpws.provider.safety.XPathStep.create(unknownNamespace).createXPathPart(DEFAULT_NS_MAP));
        } 
        { 
            QName knownNamespace = new QName("http://ns1", EXPECTED_ELEMENT_NAME); 
            assertEquals(String.format("/%s:%s", "ns1", EXPECTED_ELEMENT_NAME), 
                    XPathStep.create(knownNamespace).createXPathPart(DEFAULT_NS_MAP));
        } 
    } 
}