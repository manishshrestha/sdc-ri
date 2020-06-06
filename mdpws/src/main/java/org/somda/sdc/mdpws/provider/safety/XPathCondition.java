package org.somda.sdc.mdpws.provider.safety;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class to create XPath conditions attached to {@linkplain XPathStep} objects.
 * <p>
 * Conditions are added in brackets.
 * In order to ensure MDPWS compatibility, this XPath condition creator only allows
 * <ul>
 * <li>element indices
 * <li>attributes matching a specific literal value or number
 * </ul>
 */
public class XPathCondition {
    private final String nonColonizedAttributeName;
    private final String literal;
    private final BigDecimal decimal;
    private final Integer integer;
    private final QName qualifiedAttributeName;

    /**
     * Adds a condition based on an element index.
     * <p>
     * <em>Note that XPath element indices start at 1</em>
     *
     * @param index the index to add
     * @return {@linkplain XPathCondition} instance.
     */
    public static XPathCondition createFromElementIndex(Integer index) {
        return new XPathCondition(null, null, null, null, index);
    }

    /**
     * Adds a condition based on an unqualified attribute name and a string literal.
     * <p>
     * Quotes are escaped automatically.
     *
     * @param attributeName the attribute name.
     * @param literal       the string value to be matched.
     * @return {@linkplain XPathCondition} instance.
     */
    public static XPathCondition createFromAttributeName(String attributeName, String literal) {
        return new XPathCondition(attributeName, null, literal, null, null);
    }

    /**
     * Adds a condition based on an unqualified attribute name and a decimal number.
     * <p>
     * Quotes are escaped automatically.
     *
     * @param attributeName the attribute name.
     * @param number        the decimal number to be matched.
     * @return {@linkplain XPathCondition} instance.
     */
    public static XPathCondition createFromAttributeName(String attributeName, BigDecimal number) {
        return new XPathCondition(attributeName, null, null, number, null);
    }

    /**
     * Adds a condition based on an unqualified attribute name and an integer.
     * <p>
     * Quotes are escaped automatically.
     *
     * @param attributeName the attribute name.
     * @param number        the integer to be matched.
     * @return {@linkplain XPathCondition} instance.
     */
    public static XPathCondition createFromAttributeName(String attributeName, Integer number) {
        return new XPathCondition(attributeName, null, null, null, number);
    }

    /**
     * Adds a condition based on a qualified attribute name and a string literal.
     * <p>
     * Quotes are escaped automatically.
     *
     * @param attributeName the attribute name.
     * @param literal       the string value to be matched.
     * @return {@linkplain XPathCondition} instance.
     */
    public static XPathCondition createFromAttributeName(QName attributeName, String literal) {
        return new XPathCondition(null, attributeName, literal, null, null);
    }

    /**
     * Adds a condition based on a qualified attribute name and a decimal number.
     * <p>
     * Quotes are escaped automatically.
     *
     * @param attributeName the attribute name.
     * @param number        the decimal number to be matched.
     * @return {@linkplain XPathCondition} instance.
     */
    public static XPathCondition createFromAttributeName(QName attributeName, BigDecimal number) {
        return new XPathCondition(null, attributeName, null, number, null);
    }

    /**
     * Adds a condition based on a qualified attribute name and an integer.
     * <p>
     * Quotes are escaped automatically.
     *
     * @param attributeName the attribute name.
     * @param number        the integer to be matched.
     * @return {@linkplain XPathCondition} instance.
     */
    public static XPathCondition createFromAttributeName(QName attributeName, Integer number) {
        return new XPathCondition(null, attributeName, null, null, number);
    }

    private XPathCondition(@Nullable String nonColonizedAttributeName,
                           @Nullable QName qualifiedAttributeName,
                           @Nullable String literal,
                           @Nullable BigDecimal decimal,
                           @Nullable Integer integer) {
        this.nonColonizedAttributeName = nonColonizedAttributeName;
        this.qualifiedAttributeName = qualifiedAttributeName;
        this.literal = literal;
        this.decimal = decimal;
        this.integer = integer;
    }

    /**
     * Creates the serialized XPath string, supposed to be used by {@linkplain XPathStep} only.
     *
     * @param namespacesToPrefix the namespace/prefix mapping to be used for qualified names.
     * @return an XPath string matching this condition enclosed in brackets.
     */
    public String createXPathPart(Map<String, String> namespacesToPrefix) {
        StringBuilder xPathPart = new StringBuilder();
        xPathPart.append('[');
        appendIndex(xPathPart);
        appendLiteral(xPathPart, namespacesToPrefix);
        appendDecimal(xPathPart, namespacesToPrefix);
        appendInteger(xPathPart, namespacesToPrefix);
        xPathPart.append(']');
        var str = xPathPart.toString();
        if (str.length() == 2) {
            return "";
        }
        return str;
    }

    private void appendIndex(StringBuilder xPathPart) {
        if (!hasAttributeName() && integer != null) {
            if (integer > 0) {
                xPathPart.append(integer);
            }
        }
    }

    private boolean appendAttributeName(StringBuilder xPathPart, Map<String, String> namespacesToPrefix) {
        if (nonColonizedAttributeName != null) {
            xPathPart.append(nonColonizedAttributeName);
            xPathPart.append('=');
            return true;
        }
        if (qualifiedAttributeName != null) {
            var prefix = namespacesToPrefix.get(qualifiedAttributeName.getNamespaceURI());
            if (prefix != null) {
                xPathPart.append(prefix);
                xPathPart.append(':');
                xPathPart.append(qualifiedAttributeName.getLocalPart());
                xPathPart.append('=');
                return true;
            }
        }

        return false;
    }

    private void appendLiteral(StringBuilder xPathPart, Map<String, String> namespacesToPrefix) {
        if (!hasAttributeName() || literal == null) {
            return;
        }
        if (!appendAttributeName(xPathPart, namespacesToPrefix)) {
            return;
        }

        // As there are no escape characters in XPath, split up string literals 

        // check literal for single and double quotes 
        var singleQuote = '\'';
        var doubleQuote = '"';
        var foundSingleQuote = false;
        var foundDoubleQuote = false;
        for (int i = 0; i < literal.length(); ++i) {
            if (literal.charAt(i) == singleQuote) {
                foundSingleQuote = true;
            }
            if (literal.charAt(i) == doubleQuote) {
                foundDoubleQuote = true;
            }
        }

        // If none or only one of both was found, continue with one literal, enclosed by a suitable quote 
        if (!(foundSingleQuote && foundDoubleQuote)) {
            var quoteType = doubleQuote;
            if (foundDoubleQuote) {
                quoteType = singleQuote;
            }
            xPathPart.append(quoteType);
            xPathPart.append(literal);
            xPathPart.append(quoteType);
            return;
        }

        // There are single and double quotes in the text - split up and concat appropriately 

        xPathPart.append("concat(");
        var firstLiteral = true;
        var progress = 0;
        Optional<Character> currentQuote = Optional.empty();
        for (int i = 0; i < literal.length(); ++i) {
            var currentChar = literal.charAt(i);
            if (currentChar != singleQuote && currentChar != doubleQuote) {
                continue;
            }

            if (currentQuote.isEmpty()) {
                currentQuote = Optional.of(currentChar);
                continue;
            }

            if (currentQuote.get().equals(singleQuote) && currentChar == singleQuote) {
                continue;
            }
            if (currentQuote.get().equals(doubleQuote) && currentChar == doubleQuote) {
                continue;
            }

            if (!firstLiteral) {
                xPathPart.append(',');
            } else {
                firstLiteral = false;
            }

            var quoteToUse = singleQuote;
            if (currentQuote.get().equals(singleQuote)) {
                quoteToUse = doubleQuote;
            }

            xPathPart.append(quoteToUse);
            xPathPart.append(literal, progress, i);
            xPathPart.append(quoteToUse);

            currentQuote = Optional.of(currentChar);
            progress = i;
        }

        xPathPart.append(',');
        var quoteToUse = singleQuote;
        if (currentQuote.get().equals(singleQuote)) {
            quoteToUse = doubleQuote;
        }
        xPathPart.append(quoteToUse);
        xPathPart.append(literal, progress, literal.length());
        xPathPart.append(quoteToUse);

        xPathPart.append(')');
    }

    private void appendDecimal(StringBuilder xPathPart, Map<String, String> namespacesToPrefix) {
        if (!hasAttributeName() || decimal == null) {
            return;
        }
        if (!appendAttributeName(xPathPart, namespacesToPrefix)) {
            return;
        }

        xPathPart.append(decimal.toPlainString());
    }

    private void appendInteger(StringBuilder xPathPart, Map<String, String> namespacesToPrefix) {
        if (!hasAttributeName() || integer == null) {
            return;
        }
        if (!appendAttributeName(xPathPart, namespacesToPrefix)) {
            return;
        }

        xPathPart.append(integer);
    }

    private boolean hasAttributeName() {
        return nonColonizedAttributeName != null || qualifiedAttributeName != null;
    }
} 
