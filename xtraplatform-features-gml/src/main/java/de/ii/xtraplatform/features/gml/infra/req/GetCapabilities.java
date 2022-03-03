/**
 * Copyright 2017 European Union, interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * bla
 */
/**
 * bla
 */
package de.ii.xtraplatform.features.gml.infra.req;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.xml.domain.XMLDocument;
import de.ii.xtraplatform.xml.domain.XMLDocumentFactory;
import org.w3c.dom.Element;

import java.util.Map;

/**
 *
 * @author fischer
 */
public class GetCapabilities implements WfsOperation {

    private final WFS.VERSION m_version;

    public GetCapabilities() {
        m_version = null;
    }

    public GetCapabilities(WFS.VERSION version) {
        m_version = version;
    }

    @Override
    public WFS.OPERATION getOperation() {
        return WFS.OPERATION.GET_CAPABILITES;
    }

    @Override
    public XMLDocument asXml(XMLDocumentFactory documentFactory, Versions versions) {
        if (versions.getWfsVersion() == null) {
            versions.setWfsVersion(WFS.VERSION._1_1_0);
        }

        XMLDocument doc = documentFactory.newDocument();
        doc.addNamespace(WFS.getNS(versions.getWfsVersion()), WFS.getPR(versions.getWfsVersion()));
        Element oper = doc.createElementNS(WFS.getNS(versions.getWfsVersion()), getOperationName(versions.getWfsVersion()));
        doc.appendChild(oper);

        if (m_version != null) {
            oper.setAttribute(WFS.getWord(m_version, WFS.VOCABULARY.VERSION), m_version.toString());
        }

        oper.setAttribute("service", "WFS");

        return doc;
    }

    @Override
    public Map<String, String> asKvp(XMLDocumentFactory documentFactory, Versions versions) {
        final ImmutableMap.Builder<String,String> builder = ImmutableMap.builder();

        builder.put("REQUEST", this.getOperation()
                                  .toString());
        builder.put("SERVICE", "WFS");

        if (m_version != null) {
            builder.put("VERSION", m_version.toString());
        } else if (versions.getWfsVersion() != null) {
            builder.put("VERSION", versions.getWfsVersion()
                                          .toString());
        }

        return builder.build();
    }
}
