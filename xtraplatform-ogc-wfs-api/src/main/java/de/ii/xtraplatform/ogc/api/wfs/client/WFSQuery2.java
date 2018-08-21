/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.api.wfs.client;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Envelope;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.util.xml.XMLDocument;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDParticle;
import org.geotools.filter.FidFilterImpl;
import org.geotools.filter.v2_0.bindings.BBOXTypeBinding;
import org.geotools.filter.v2_0.bindings.BinaryComparisonOpTypeBinding;
import org.geotools.geometry.jts.LiteCoordinateSequence;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.gml3.bindings.GML3EncodingUtils;
import org.geotools.gml3.v3_2.GML;
import org.geotools.gml3.v3_2.bindings.EnvelopeTypeBinding;
import org.geotools.referencing.CRS;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class WFSQuery2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(WFSQuery2.class);

    private final List<String> typeNames;
    private final List<Filter> filter;
    private final EpsgCrs crs;

    WFSQuery2(List<String> typeNames, List<Filter> filter, EpsgCrs crs) {
        this.typeNames = typeNames;
        this.filter = filter;
        this.crs = crs;
    }

    public Element asXml(XMLDocument document, Versions versions) throws IOException, TransformerException, SAXException {
        document.addNamespace(WFS.getNS(versions.getWfsVersion()), WFS.getPR(versions.getWfsVersion()));

        final Element query = document.createElementNS(WFS.getNS(versions.getWfsVersion()), WFS.getWord(versions.getWfsVersion(), WFS.VOCABULARY.QUERY));

        if (!filter.isEmpty()) {
            final Node node = document.adoptDocument(new FilterEncoder(versions.getWfsVersion()).encode(filter.get(0)));
            query.appendChild(node);
        }

        query.setAttribute(WFS.getWord(versions.getWfsVersion(), WFS.VOCABULARY.TYPENAMES), getTypeNames());

        if (this.crs != null) {
            query.setAttribute(WFS.getWord(versions.getWfsVersion(), WFS.VOCABULARY.SRSNAME), getCrs(versions.getWfsVersion()));
        }

        return query;
    }

    public Map<String, String> asKvp(XMLDocument document, Versions versions) throws IOException, TransformerException, SAXException {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put(WFS.getWord(versions.getWfsVersion(), WFS.VOCABULARY.TYPENAMES)
                       .toUpperCase(), getTypeNames());

        if (getCrs(versions.getWfsVersion()) != null) {
            builder.put(WFS.getWord(versions.getWfsVersion(), WFS.VOCABULARY.SRSNAME)
                           .toUpperCase(), getCrs(versions.getWfsVersion()));
        }

        // check if the first level expression is BBOX
        // check if the first level expression is ResourceId

        if (!filter.isEmpty()) {
            if (filter.get(0) instanceof FidFilterImpl) {
                builder.put(WFS.getWord(versions.getWfsVersion()
                                                .getFilterVersion(), FES.VOCABULARY.RESOURCEID)
                               .toUpperCase(), Joiner.on(',').join(((FidFilterImpl) filter.get(0)).getFidsSet()));
            } else {
                final Node node = document.adoptDocument(new FilterEncoder(versions.getWfsVersion()).encode(filter.get(0)));
                document.appendChild(node);
                document.done();
                final String filterString = document.toString(false, true);
                LOGGER.debug("FILTER {}", filterString);
                builder.put(WFS.getWord(versions.getWfsVersion()
                                                .getFilterVersion(), FES.VOCABULARY.FILTER)
                               .toUpperCase(), filterString);
            }
        }


        return builder.build();
    }

    private String getTypeNames() {
        return Joiner.on(',')
                     .skipNulls()
                     .join(typeNames);
    }

    private String getCrs(WFS.VERSION version) {
        if (this.crs != null) {
            return crs.getAsSimple();
        }
        return null;
    }


}
