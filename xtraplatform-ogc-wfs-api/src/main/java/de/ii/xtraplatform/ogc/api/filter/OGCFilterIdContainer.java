/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.api.filter;

import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.xml.domain.XMLDocument;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public class OGCFilterIdContainer extends OGCFilterExpression {

    List<OGCFilterExpression> operands;

    public OGCFilterIdContainer() {
        this.operands = new ArrayList();
    }

    public void addOperand(OGCFilterExpression oper) {
        this.operands.add(oper);
    }

    @Override
    public void toXML(FES.VERSION version, Element e, XMLDocument doc) {
        if (operands.size() == 1) {
            operands.get(0).toXML(version, e, doc);

        } else if (operands.size() > 1) {
            if (version.compareTo(FES.VERSION._1_1_0) > 0) {
                doc.addNamespace(FES.getNS(version),FES.getPR(version));
                Element ex = doc.createElementNS(FES.getNS(version), FES.getWord(version, FES.VOCABULARY.OR));
                e.appendChild(ex);

                for (OGCFilterExpression expr : operands) {
                    expr.toXML(version, ex, doc);
                }
            } else {
                for (OGCFilterExpression expr : operands) {
                    expr.toXML(version, e, doc);
                }
            }
        }
    }
}
