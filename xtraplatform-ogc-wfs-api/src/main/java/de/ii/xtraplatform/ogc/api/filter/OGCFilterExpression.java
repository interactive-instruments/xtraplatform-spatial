/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.api.filter;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.xml.domain.XMLDocument;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import java.util.Map;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public abstract class OGCFilterExpression {
    
    protected XMLNamespaceNormalizer NSstore;

    public abstract void toXML(FES.VERSION version, Element e, XMLDocument doc);

    public Map<String, String> toKVP(FES.VERSION version) {
        return ImmutableMap.of();
    }
}
