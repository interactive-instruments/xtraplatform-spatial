/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.xml.domain;

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
