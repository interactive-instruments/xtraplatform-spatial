/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.api.wfs;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.xml.domain.XMLDocument;
import org.geotools.filter.FidFilterImpl;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class WfsQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsQuery.class);

    private static final Map<WFS.VERSION, FilterEncoder> FILTER_ENCODERS = ImmutableMap.of(
            WFS.VERSION._2_0_0, new FilterEncoder(WFS.VERSION._2_0_0),
            WFS.VERSION._1_1_0, new FilterEncoder(WFS.VERSION._1_1_0),
            WFS.VERSION._1_0_0, new FilterEncoder(WFS.VERSION._1_0_0)
    );

    private final List<String> typeNames;
    private final List<Filter> filter;
    private final EpsgCrs crs;

    WfsQuery(List<String> typeNames, List<Filter> filter, EpsgCrs crs) {
        this.typeNames = typeNames;
        this.filter = filter;
        this.crs = crs;
    }

    public Element asXml(XMLDocument document, Versions versions) throws IOException, TransformerException, SAXException {
        document.addNamespace(WFS.getNS(versions.getWfsVersion()), WFS.getPR(versions.getWfsVersion()));

        final Element query = document.createElementNS(WFS.getNS(versions.getWfsVersion()), WFS.getWord(versions.getWfsVersion(), WFS.VOCABULARY.QUERY));

        if (!filter.isEmpty()) {
            final Node node = document.adoptDocument(FILTER_ENCODERS.get(versions.getWfsVersion()).encode(filter.get(0)));
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
                final Node node = document.adoptDocument(FILTER_ENCODERS.get(versions.getWfsVersion()).encode(filter.get(0)));
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
            return crs.toSimpleString();
        }
        return null;
    }


}
