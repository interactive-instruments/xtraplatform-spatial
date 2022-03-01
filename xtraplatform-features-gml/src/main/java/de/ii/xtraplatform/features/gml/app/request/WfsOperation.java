/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.app.request;

import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.xml.domain.XMLDocument;
import de.ii.xtraplatform.xml.domain.XMLDocumentFactory;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Map;

/**
 * @author zahnen
 */
public interface WfsOperation {
    WFS.OPERATION getOperation();

    default String getOperationName(WFS.VERSION version) {
        return WFS.getWord(version, this.getOperation());
    }

    XMLDocument asXml(XMLDocumentFactory documentFactory, Versions versions) throws TransformerException, IOException, SAXException;

    Map<String,String> asKvp(XMLDocumentFactory documentFactory, Versions versions) throws TransformerException, IOException, SAXException;
}
