package org.somda.sdc.mdpws.consumer;

import com.google.common.base.Splitter;
import org.somda.sdc.mdpws.consumer.exception.XPathParseException;

import java.util.Map;

/**
 * Expands namespace prefixes from XPath expressions compatible with the limited XPath subset from MDPWS.
 * <p>
 * <em>Important note: this expander imitates a limited XPath tokenizer, hence any invalid XPath expression could
 * silently be accepted even if the result is not meaningful.</em>
 */
public class NamespacePrefixExpander {
    private final Map<String, String> namespacePrefixMapping;
    private final String defaultNamespace;
    private final String xPath;

    /**
     * Creates an expander based on the given namespace prefix mapping and XPath expression.
     *
     * @param namespacePrefixMapping the mapping the shall include every prefix that can potentially come up during
     *                               parsing.
     *                               The default namespace, if defined, shall be available from the empty key ("").
     * @param xPath                  the XPath expression in which prefixes are supposed to be expanded.
     */
    public NamespacePrefixExpander(Map<String, String> namespacePrefixMapping, String xPath) {
        this.namespacePrefixMapping = namespacePrefixMapping;
        this.defaultNamespace = namespacePrefixMapping.get("");
        this.xPath = xPath;
    }

    /**
     * Performs the actual prefix expansion.
     *
     * @return an XPath string with expanded prefixes in the form of <code>{NAMESPACE}LOCAL-NAME</code>.
     * @throws XPathParseException if the XPath expression could not be parsed correctly.
     */
    public String get() throws XPathParseException {
        if (xPath.isEmpty()) {
            return "";
        }

        var expandedXPath = new StringBuilder();
        var index = 0;
        try {
            do {
                index = expandStep(expandedXPath, index);
            } while (index < xPath.length());
        } catch (IndexOutOfBoundsException e) {
            throw new XPathParseException(
                    String.format("Expected a next token, but run into end of expression. Expression: %s", xPath));
        }
        return expandedXPath.toString();
    }

    private int expandStep(StringBuilder expandedXPath,
                           int index) throws XPathParseException, IndexOutOfBoundsException {
        // Exist recursion if start position exceeds xPath length
        if (index >= xPath.length()) {
            return index;
        }

        index = ignoreWhiteSpaces(expandedXPath, index);

        // Each step needs to start with a slash
        if (xPath.charAt(index++) != '/') {
            throw new XPathParseException(String.format(
                    "XPath expression does not start with a slash. First character is a %s at position %s." +
                            "Expression: %s",
                    xPath.charAt(index - 1),
                    (index - 1),
                    xPath));
        }

        // Start expansion by adding required slash
        expandedXPath.append('/');
        index = ignoreWhiteSpaces(expandedXPath, index);

        /*
            Identify step token which can be
            - an unqualified attribute
            - a qualified attribute
            - an unqualified element name
            - a qualified element name
            - a text() reference
         */

        // In case of an attribute ignore the at-sign for parsing, but append it to the expanded step
        if (xPath.charAt(index) == '@') {
            index++;
            expandedXPath.append('@');
            index = ignoreWhiteSpaces(expandedXPath, index);
        }

        // Cut out the name which is terminated by next step ('/') or condition ('[')
        int nameOffset = index++;
        while (index < xPath.length() && xPath.charAt(index) != '/' && xPath.charAt(index) != '[') {
            index++;
        }

        // There must be at least one character cut out
        int characterCount = index - nameOffset;
        if (characterCount == 0) {
            throw new XPathParseException(
                    String.format("XPath expression misses an expected name at position %s, but none found. " +
                            "Expression: %s", index, xPath));
        }
        var stepName = xPath.substring(nameOffset, index);

        // Check if 'text()' selector occurs somewhere between whitespaces and quit recursion here if true
        if (stepName.contains("text()")) {
            expandedXPath.append(stepName);
            return index;
        }

        expandName(expandedXPath, stepName);

        return expandCondition(expandedXPath, index);
    }

    private int expandCondition(StringBuilder expandedXPath,
                                int index) throws XPathParseException, IndexOutOfBoundsException {
        // Exit condition parsing if xPath length is exceeded
        index = ignoreWhiteSpaces(expandedXPath, index);
        if (index == xPath.length()) {
            return index;
        }

        // Exit condition parsing if there is no xPath start token at startPosition
        if (xPath.charAt(index) != '[') {
            return index;
        }

        expandedXPath.append('[');
        index = ignoreWhiteSpaces(expandedXPath, ++index);

        // In case of no attribute this is a element index - continuing to copy until condition is terminated by ']'
        if (xPath.charAt(index) != '@') {
            while (xPath.charAt(index) != ']') {
                //index++;
                expandedXPath.append(xPath.charAt(index++));
            }
            expandedXPath.append(']');
            return ++index;
        }

        expandedXPath.append('@');
        index = ignoreWhiteSpaces(expandedXPath, ++index);

        // Found comparison of literal value
        // Cut out the name which is terminated by next equal character '=' and expand
        int nameOffset = index;
        while (xPath.charAt(index) != '=' && !isWhiteSpace(xPath.charAt(index))) {
            index++;
        }
        expandName(expandedXPath, xPath.substring(nameOffset, index));

        // Continue until condition ends with a closing bracket
        // Make sure the closing bracket does not appear in a string literal
        boolean withinLiteral = false;
        boolean escaped = false;
        while (true) {
            var charAtIndex = xPath.charAt(index);
            expandedXPath.append(charAtIndex);

            boolean isQuote = charAtIndex == '\'' || charAtIndex == '"';
            if (isQuote && !escaped) {
                withinLiteral = !withinLiteral;
            }
            if (escaped) {
                escaped = false;
            } else {
                if (charAtIndex == '\\') {
                    escaped = true;
                }
            }

            index++;

            // Only check on closing bracket if there is no literal string opened
            if (charAtIndex == ']' && !withinLiteral) {
                return index;
            }
        }
    }

    private void expandName(StringBuilder expandedXPath,
                            String fullName) throws XPathParseException {
        // Split full name at ':' to separate into prefix and local name
        var split = Splitter.on(':').split(fullName);

        // Expect at least one element
        var splitIterator = split.iterator();
        if (!splitIterator.hasNext()) {
            throw new XPathParseException(
                    String.format("XPath expression lacks a prefix or qualified name. Expression: %s", xPath));
        }

        // Set namespace to default one (null is explicitly allowed; it indicates no specific namespace)
        var namespace = defaultNamespace;
        var prefix = splitIterator.next();
        var localName = "";
        if (splitIterator.hasNext()) {
            if (prefix.isEmpty()) {
                throw new XPathParseException(String.format("A namespace prefix was empty in expression: %s", xPath));
            }

            // If there is a second item, expect it to be the local name
            localName = splitIterator.next();
            if (localName.isEmpty()) {
                throw new XPathParseException(String.format("A local name was empty in expression: %s", xPath));
            }

            // Expect the first item to be the prefix and expand it accordingly
            namespace = namespacePrefixMapping.get(prefix);

            // If the namespace is still null, point this out as an error (namespace context insufficient)
            if (namespace == null) {
                throw new XPathParseException(String.format("Namespace prefix mapping misses namespace for prefix: %s",
                        prefix));
            }
        } else {
            // If there is no second item, expect the prefix to be the local name
            localName = prefix;
        }

        if (namespace == null) {
            // With no default namespace available, just append local name
            expandedXPath.append(localName);
        } else {
            // Instead of adding the prefix, append the expanded namespace URI
            expandedXPath.append('{').append(namespace).append('}').append(localName);
        }
    }

    // whitespace definition in accordance with https://www.w3.org/TR/REC-xml/#sec-common-syn
    private boolean isWhiteSpace(char c) {
        switch (c) {
            case 0x20:
            case 0x9:
            case 0xD:
            case 0xA:
                return true;
        }
        return false;
    }

    // appends all whitespaces and returns the index of the first non-whitespace
    private int ignoreWhiteSpaces(StringBuilder expandedXPath, int index) {
        while (true) {
            if (index == xPath.length()) {
                return index;
            }
            char c = xPath.charAt(index++);
            if (isWhiteSpace(c)) {
                expandedXPath.append(c);
            } else {
                return index - 1;
            }
        }
    }
}
