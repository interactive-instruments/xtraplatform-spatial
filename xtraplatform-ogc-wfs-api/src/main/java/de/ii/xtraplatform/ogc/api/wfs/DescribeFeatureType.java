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
package de.ii.xtraplatform.ogc.api.wfs;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLDocumentFactory;
import org.w3c.dom.Element;

import java.util.Map;

/**
 *
 * @author fischer
 */
public class DescribeFeatureType implements WfsOperation {

    public DescribeFeatureType() {
    }

    @Override
    public WFS.OPERATION getOperation() {
        return WFS.OPERATION.DESCRIBE_FEATURE_TYPE;
    }

    @Override
    public XMLDocument asXml(XMLDocumentFactory documentFactory, Versions versions) {
        final XMLDocument doc = documentFactory.newDocument();
        doc.addNamespace(WFS.getNS(versions.getWfsVersion()), WFS.getPR(versions.getWfsVersion()));

        Element operation = doc.createElementNS(WFS.getNS(versions.getWfsVersion()), getOperationName(versions.getWfsVersion()));
        operation.setAttribute("service", "WFS");
        doc.appendChild(operation);

        if (versions.getGmlVersion() != null && versions.getWfsVersion() != null) {
            operation.setAttribute(GML.getWord(versions.getWfsVersion(), WFS.VOCABULARY.OUTPUT_FORMAT), GML.getWord(versions.getGmlVersion(), GML.VOCABULARY.OUTPUTFORMAT_VALUE));
            operation.setAttribute(WFS.getWord(versions.getWfsVersion(), WFS.VOCABULARY.VERSION), versions.getWfsVersion()
                                                                                                          .toString());
        }

        return doc;
    }

    @Override
    public Map<String, String> asKvp(XMLDocumentFactory documentFactory, Versions versions) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put("REQUEST", this.getOperation()
                                   .toString());
        builder.put("SERVICE", "WFS");

        if (versions.getWfsVersion() != null) {
            builder.put("VERSION", versions.getWfsVersion()
                                           .toString());

            if (versions.getGmlVersion() != null && !versions.getGmlVersion()
                                                             .isGreaterOrEqual(versions.getWfsVersion()
                                                                                       .getGmlVersion())) {
                builder.put("OUTPUTFORMAT", GML.getWord(versions.getGmlVersion(), GML.VOCABULARY.OUTPUTFORMAT_VALUE));
            }
        }

        return builder.build();
    }
}
