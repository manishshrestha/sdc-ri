package org.ieee11073.sdc.dpws;

import org.ieee11073.sdc.dpws.model.LocalizedStringType;
import org.ieee11073.sdc.dpws.model.ObjectFactory;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Convenient class to build localized string lists.
 */
public class LocalizedStringsBuilder {
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    private static final String XML_LANG = "lang";

    private final ObjectFactory objectFactory;
    private final List<LocalizedStringType> localizedStringTypes;

    /**
     * Create instance with empty localized string list.
     */
    public LocalizedStringsBuilder() {
        objectFactory = new ObjectFactory();
        localizedStringTypes = new ArrayList<>(3);
    }

    /**
     * Create instance with one text in default language.
     *
     * @param text Text of the first element in the localized string list.
     */
    public LocalizedStringsBuilder(String text) {
        this(null, text);
    }

    /**
     * Create instance with one text in specified language.
     *
     * @param locale Locale identifier in accordance to XML xml:lang specification. Omitted if null.
     * @param text   Text of the first element in the localized string list.
     */
    public LocalizedStringsBuilder(@Nullable String locale, String text) {
        this();
        add(locale, text);
    }

    /**
     * Add text in default language.
     *
     * @param text The text to add.
     * @return The object where the text was added to chain multiple calls.
     */
    public LocalizedStringsBuilder add(String text) {
        return add(null, text);
    }

    /**
     * Add text with specified language.
     *
     * @param lang Locale identifier in accordance to XML xml:lang specification. Omitted if null.
     * @param text The text to add.
     * @return The object where the text was added to chain multiple calls.
     */
    public LocalizedStringsBuilder add(@Nullable String lang, String text) {
        LocalizedStringType localizedStringType = objectFactory.createLocalizedStringType();
        Optional.ofNullable(lang).ifPresent(s ->
                localizedStringType.getOtherAttributes().put(new QName(XML_NAMESPACE, XML_LANG), s));
        localizedStringType.setValue(text);
        localizedStringTypes.add(localizedStringType);
        return this;
    }

    /**
     * @return copy of the current localized string list.
     */
    public List<LocalizedStringType> get() {
        return new ArrayList<>(localizedStringTypes);
    }

    /**
     * Reset current localized string buffer.
     *
     * @return current localized string buffer
     */
    public LocalizedStringsBuilder clear() {
        localizedStringTypes.clear();
        return this;
    }
}
