package org.somda.sdc.glue.consumer;

import org.somda.sdc.biceps.model.extension.ExtensionType;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to seek extensions.
 */
public class ExtensionUtil {
    private static final String METHOD_NAME = "getExtension";

    /**
     * Takes an element and searches extensions for the given type.
     *
     * @param enclosingExtensionElement the element that contains an ext:Extension element.
     * @param qName                     the expected qualified name to match.
     * @param <T>                       any element type that contains an ext:Extension element.
     * @return a list of DOM nodes of the given qualified name.
     * @throws IllegalArgumentException if the given enclosingExtensionElement does not contain exactly one
     *                                  accessor function to retrieve the extension.
     */
    public static <T> List<Node> getExtensionsOfQName(T enclosingExtensionElement, QName qName) {
        var extensionContainer = findExtensionElement(enclosingExtensionElement);
        if (extensionContainer == null) {
            return Collections.emptyList();
        }
        return filterNodes(extensionContainer)
                .filter(n -> n.getLocalName() != null && n.getLocalName().equals(qName.getLocalPart()))
                .filter(n -> n.getNamespaceURI() != null && n.getNamespaceURI().equals(qName.getNamespaceURI()))
                .collect(Collectors.toList());
    }

    /**
     * Takes an element and searches extensions for the given type.
     *
     * @param enclosingExtensionElement the element that contains an ext:Extension element.
     * @param extensionType             the expected extension type (needs to be known by JAXB)
     * @param <T>                       any element type that contains an ext:Extension element.
     * @param <V>                       the expected extension type.
     * @return a list of instances of the given extension type.
     * @throws IllegalArgumentException if the given enclosingExtensionElement does not contain exactly one
     *                                  accessor function to retrieve the extension.
     */
    @SuppressWarnings("unchecked")
    public static <T, V> List<V> getExtensionsOfType(T enclosingExtensionElement, Class<V> extensionType) {
        var extensionContainer = findExtensionElement(enclosingExtensionElement);
        if (extensionContainer == null) {
            return Collections.emptyList();
        }
        return extensionContainer.getAny().stream()
                .filter(extensionType::isInstance)
                .map(extensionType::cast)
                .collect(Collectors.toList());
    }

    private static ExtensionType findExtensionElement(Object extensionElement) {
        try {

            var getExtensionMethod = extensionElement.getClass().getMethod(METHOD_NAME);
            if (!getExtensionMethod.getReturnType().equals(ExtensionType.class)) {
                throw new NoSuchMethodException(String.format(
                        "Given enclosing extension element %s has no extension accessor function named %s that" +
                                " returns the extension element",
                        extensionElement,
                        METHOD_NAME));
            }

            return (ExtensionType) getExtensionMethod.invoke(extensionElement);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Stream<Node> filterNodes(ExtensionType extension) {
        return extension.getAny().stream()
                .filter(Node.class::isInstance)
                .map(Node.class::cast);
    }
}
