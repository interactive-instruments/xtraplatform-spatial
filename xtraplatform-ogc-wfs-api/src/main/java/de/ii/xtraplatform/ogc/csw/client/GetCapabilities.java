/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
