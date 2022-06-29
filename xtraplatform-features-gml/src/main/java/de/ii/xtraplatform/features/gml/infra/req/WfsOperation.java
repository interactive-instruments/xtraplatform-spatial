/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.infra.req;

import de.ii.xtraplatform.features.gml.infra.xml.XMLDocument;
import de.ii.xtraplatform.features.gml.infra.xml.XMLDocumentFactory;
import java.io.IOException;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

/**
 * @author zahnen
 */
public interface WfsOperation {
  WFS.OPERATION getOperation();

  default String getOperationName(WFS.VERSION version) {
    return WFS.getWord(version, this.getOperation());
  }

  XMLDocument asXml(XMLDocumentFactory documentFactory, Versions versions)
      throws TransformerException, IOException, SAXException;

  Map<String, String> asKvp(XMLDocumentFactory documentFactory, Versions versions)
      throws TransformerException, IOException, SAXException;
}
