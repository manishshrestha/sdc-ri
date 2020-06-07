package mdpws.consumer;

import org.somda.sdc.mdpws.consumer.NamespacePrefixExpander;
import org.somda.sdc.mdpws.consumer.exception.XPathParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NamespacePrefixExpanderTest {
    private Map<String, String> namespacePrefixMappingWithDefault;
    private Map<String, String> namespacePrefixMappingWithoutDefault;

    @BeforeEach
    void beforeEach() {
        namespacePrefixMappingWithDefault = Map.of(
                "p1", "http://p1",
                "p2", "http://p2",
                "p3", "http://p3",
                "", "http://default");

        namespacePrefixMappingWithoutDefault = Map.of(
                "p1", "http://p1",
                "p2", "http://p2",
                "p3", "http://p3");
    }

    @Test
    @DisplayName("Valid XPath combinations with default namespace mapping")
    void validXPathWithDefaultNamespace() throws XPathParseException {
        var xPathExpressions = new ArrayList<String>();
        xPathExpressions.addAll(List.of(
                "/text()",
                "/a/b/c/text()",
                "/p1:a/p2:b/p3:c/d/text()",
                "/p1:a[2]/@b",
                "/p1:a[@p2:b=3]/@p3:c",
                "/p1:a[@p2:b=3.43]/@c",
                "/p1:a[@p2:b=\"foo]\\\"bar]\"]/@c"
        ));
        xPathExpressions.addAll(List.of(
                "  /\t text() \t ",
                "\t / a / \tb / \tc / text()\t",
                "/\tp1:a\t/p2:b/p3:c/\td  /\ttext()",
                "  /  p1:a  [  2  ]  /  @  b  ",
                " / p1:a [ @ p2:b = 3 ] / @ p3:c ",
                "\t/\tp1:a\t[\t@\tp2:b\t=\t3.43\t]\t/\t@\tc\t",
                " / p1:a [ @ p2:b = \"foo]\\\"bar ] \" ] / @ c "
        ));
        xPathExpressions.addAll(List.of(
                "/p1:a[@p2:b=concat(\"foo]bar]\",'[test]',\"[]\"]/text()"
        ));

        var expectedExpansions = new ArrayList<String>();
        expectedExpansions.addAll(List.of(
                "/text()",
                "/{http://default}a/{http://default}b/{http://default}c/text()",
                "/{http://p1}a/{http://p2}b/{http://p3}c/{http://default}d/text()",
                "/{http://p1}a[2]/@{http://default}b",
                "/{http://p1}a[@{http://p2}b=3]/@{http://p3}c",
                "/{http://p1}a[@{http://p2}b=3.43]/@{http://default}c",
                "/{http://p1}a[@{http://p2}b=\"foo]\\\"bar]\"]/@{http://default}c"
        ));
        expectedExpansions.addAll(List.of(
                "  /\t text() \t ",
                "\t / {http://default}a / \t{http://default}b / \t{http://default}c / text()\t",
                "/\t{http://p1}a\t/{http://p2}b/{http://p3}c/\t{http://default}d  /\ttext()",
                "  /  {http://p1}a  [  2  ]  /  @  {http://default}b  ",
                " / {http://p1}a [ @ {http://p2}b = 3 ] / @ {http://p3}c ",
                "\t/\t{http://p1}a\t[\t@\t{http://p2}b\t=\t3.43\t]\t/\t@\t{http://default}c\t",
                " / {http://p1}a [ @ {http://p2}b = \"foo]\\\"bar ] \" ] / @ {http://default}c "
        ));
        expectedExpansions.addAll(List.of(
                "/{http://p1}a[@{http://p2}b=concat(\"foo]bar]\",'[test]',\"[]\"]/text()"
        ));

        for (int i = 0; i < xPathExpressions.size(); ++i) {
            var expander = new NamespacePrefixExpander(namespacePrefixMappingWithDefault, xPathExpressions.get(i));
            final int index = i;
            assertDoesNotThrow(() -> expectedExpansions.get(index), () -> "Exception on test input " + index);
            assertEquals(expectedExpansions.get(index), expander.get(), () -> "Assertion on test input " + index);
        }
    }

    @Test
    @DisplayName("Valid XPath combinations without default namespace mapping")
    void validXPathWithoutDefaultNamespace() throws XPathParseException {
        var xPathExpressions = new ArrayList<String>();
        xPathExpressions.addAll(List.of(
                "/text()",
                "/a/b/c/text()",
                "/p1:a/p2:b/p3:c/d/text()",
                "/p1:a[2]/@b",
                "/p1:a[@p2:b=3]/@p3:c",
                "/p1:a[@p2:b=3.43]/@c",
                "/p1:a[@p2:b=\"foo]\\\"bar]\"]/@c"
        ));
        xPathExpressions.addAll(List.of(
                "  /\t text() \t ",
                "\t / a / \tb / \tc / text()\t",
                "/\tp1:a\t/p2:b/p3:c/\td  /\ttext()",
                "  /  p1:a  [  2  ]  /  @  b  ",
                " / p1:a [ @ p2:b = 3 ] / @ p3:c ",
                "\t/\tp1:a\t[\t@\tp2:b\t=\t3.43\t]\t/\t@\tc\t",
                " / p1:a [ @ p2:b = \"foo]\\\"bar ] \" ] / @ c "
        ));
        xPathExpressions.addAll(List.of(
                "/p1:a[@p2:b=concat(\"foo]bar]\",'[test]',\"[]\"]/text()"
        ));

        var expectedExpansions = new ArrayList<String>();
        expectedExpansions.addAll(List.of(
                "/text()",
                "/a/b/c/text()",
                "/{http://p1}a/{http://p2}b/{http://p3}c/d/text()",
                "/{http://p1}a[2]/@b",
                "/{http://p1}a[@{http://p2}b=3]/@{http://p3}c",
                "/{http://p1}a[@{http://p2}b=3.43]/@c",
                "/{http://p1}a[@{http://p2}b=\"foo]\\\"bar]\"]/@c"
        ));
        expectedExpansions.addAll(List.of(
                "  /\t text() \t ",
                "\t / a / \tb / \tc / text()\t",
                "/\t{http://p1}a\t/{http://p2}b/{http://p3}c/\td  /\ttext()",
                "  /  {http://p1}a  [  2  ]  /  @  b  ",
                " / {http://p1}a [ @ {http://p2}b = 3 ] / @ {http://p3}c ",
                "\t/\t{http://p1}a\t[\t@\t{http://p2}b\t=\t3.43\t]\t/\t@\tc\t",
                " / {http://p1}a [ @ {http://p2}b = \"foo]\\\"bar ] \" ] / @ c "
        ));
        expectedExpansions.addAll(List.of(
                "/{http://p1}a[@{http://p2}b=concat(\"foo]bar]\",'[test]',\"[]\"]/text()"
        ));

        for (int i = 0; i < xPathExpressions.size(); ++i) {
            var expander = new NamespacePrefixExpander(namespacePrefixMappingWithoutDefault, xPathExpressions.get(i));
            final int index = i;
            assertDoesNotThrow(expander::get, () -> "Exception on test input " + index);
            assertEquals(expectedExpansions.get(index), expander.get(), () -> "Assertion on test input " + index);
        }
    }

    @Test
    @DisplayName("Invalid XPath combinations")
    void invalidXPath() {
        var xPathExpressions = List.of(
                "  ?",
                "/test[23",
                "/p1:[1]/text()",
                "/",
                "/p4:a",
                "p1:a",
                "/:a/@b"
        );

        for (int i = 0; i < xPathExpressions.size(); ++i) {
            var expander = new NamespacePrefixExpander(namespacePrefixMappingWithoutDefault, xPathExpressions.get(i));
            final int index = i;
            assertThrows(XPathParseException.class,
                    expander::get,
                    () -> "No expected exception on test input " + index);
        }
    }
}