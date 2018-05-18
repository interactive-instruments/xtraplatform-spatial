package de.ii.xtraplatform.util.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author zahnen
 */
public class XMLDocumentFactory {

    private final XMLNamespaceNormalizer nsn;
    private final DocumentBuilder builder;

    public XMLDocumentFactory(XMLNamespaceNormalizer nsn) throws ParserConfigurationException {
        this.nsn = nsn;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);

        this.builder = factory.newDocumentBuilder();
    }

    public XMLDocument newDocument() {
        return new XMLDocument(nsn, builder.newDocument());
    }
}
