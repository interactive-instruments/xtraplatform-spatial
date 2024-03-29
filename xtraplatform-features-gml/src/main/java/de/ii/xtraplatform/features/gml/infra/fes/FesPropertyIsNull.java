/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.infra.fes;

import de.ii.xtraplatform.features.gml.infra.req.FES;
import de.ii.xtraplatform.features.gml.infra.xml.XMLDocument;
import org.w3c.dom.Element;

public class FesPropertyIsNull extends FesExpression {

  private final FesLiteral arg;

  public FesPropertyIsNull(FesLiteral arg) {
    this.arg = arg;
  }

  @Override
  public void toXML(FES.VERSION version, Element e, XMLDocument doc) {
    doc.addNamespace(FES.getNS(version), FES.getPR(version));
    Element ex =
        doc.createElementNS(FES.getNS(version), FES.getWord(version, FES.VOCABULARY.ISNULL));
    e.appendChild(ex);
    arg.toXML(version, ex, doc);
  }
}
