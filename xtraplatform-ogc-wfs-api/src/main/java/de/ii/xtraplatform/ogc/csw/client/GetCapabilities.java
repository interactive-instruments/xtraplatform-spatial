/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.csw.client;

import java.util.HashMap;
import java.util.Map;

import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public class GetCapabilities extends WFSOperationGetCapabilities {

    private final WFS.VERSION m_version;

    public GetCapabilities() {
        m_version = null;
    }

    public GetCapabilities(WFS.VERSION version) {
        m_version = version;
    }

    @Override
    protected void initialize(XMLNamespaceNormalizer nsStore) {
    }

    @Override
    public String getPOSTXML(XMLNamespaceNormalizer nsStore, Versions vs) {
        this.initialize(nsStore);

        // TODO
        if (vs.getWfsVersion() == null) {
            vs.setWfsVersion(WFS.VERSION._1_1_0);
        }

        XMLDocument doc = new XMLDocument(nsStore);
        Element oper = doc.createElementNS(WFS.getNS(vs.getWfsVersion()), WFS.getPR(vs.getWfsVersion()), getOperationName(vs.getWfsVersion()));
        doc.appendChild(oper);

        if (m_version != null) {
            oper.setAttribute(WFS.getWord(m_version, WFS.VOCABULARY.VERSION), m_version.toString());
        }

        // TODO
        oper.setAttribute("service", "WFS");

        String out = doc.toString(true);

        return out;
    }

    @Override
    public Map<String, String> getGETParameters(XMLNamespaceNormalizer nsStore, Versions vs) {

        Map<String, String> params = new HashMap<>();

        params.put("REQUEST", this.getOperation().toString());
        params.put("SERVICE", "WFS");

        if (m_version != null) {
            params.put("VERSION", m_version.toString());
        } else if( vs.getWfsVersion() != null) {
            params.put("VERSION", vs.getWfsVersion().toString());
        }

        return params;
    }
}
