/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.util.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.io.Writer;

/**
 *
 * @author fischer
 */
public class XMLDocument {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLDocument.class);
    private Document doc;
    private XMLNamespaceNormalizer nsn;

    public XMLDocument(XMLNamespaceNormalizer nsn) {
        this.nsn = nsn;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setNamespaceAware(true);
            factory.setValidating(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();

        } catch (Exception ex) {
            LOGGER.error("Error creating XMLDocument", ex);
        }
    }

    public void appendChild(Element element) {
        this.doc.appendChild(element);
    }

    public Element createElementNS(String namespace, String prefix, String localName) {

        this.nsn.addNamespace(prefix, namespace);
               
        return this.doc.createElementNS(namespace, this.nsn.getQualifiedName(namespace, localName));
    }

    // returns the String representation of the XML Document
    public String toString(boolean pretty) {
        try {
            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            if (pretty) {
                tf.setOutputProperty(OutputKeys.INDENT, "yes");
            }
            Writer out = new StringWriter();
            tf.transform(new DOMSource(doc), new StreamResult(out));
            return out.toString();
        } catch (Exception ex) {
            LOGGER.error("Error serializing XMLDocument", ex);
        }
        return null;
    }

    public XMLNamespaceNormalizer getNamespaceNormalizer() {
        return nsn;
    }
}
