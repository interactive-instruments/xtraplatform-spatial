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
package de.ii.xtraplatform.features.gml.app.request;

import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.xml.domain.XMLDocumentFactory;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author zahnen
 */
public class WfsRequestEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsRequestEncoder.class);

    private Map<String, Map<WFS.METHOD, URI>> urls;
    private Versions versions;
    private XMLNamespaceNormalizer nsStore;

    public WfsRequestEncoder(String version, Optional<String> gmlVersion, Map<String,String> namespaces, Map<String, Map<WFS.METHOD, URI>> urls) {
        this.urls = urls;
        this.nsStore = new XMLNamespaceNormalizer(namespaces);
        this.versions = new Versions();

        versions.setWfsVersion(WFS.VERSION.fromString(version));
        if (gmlVersion.isPresent()) {
            versions.setGmlVersion(GML.VERSION.fromString(gmlVersion.get()));
        }
    }

    public String getAsUrl(WfsOperation operation) {
        try {
            URIBuilder uri = new URIBuilder(findUrl(operation.getOperation(), WFS.METHOD.GET));

            Map<String, String> params = operation.asKvp(new XMLDocumentFactory(nsStore), versions);

            for (Map.Entry<String, String> param : params.entrySet()) {
                uri.addParameter(param.getKey(), param.getValue());
            }

            return uri.build()
                      .toString();
        } catch (URISyntaxException | ParserConfigurationException | TransformerException | IOException | SAXException e) {
            return "";
        }
    }

    public Tuple<String,String> getAsUrlAndBody(WfsOperation operation) {
        try {
            URI uri = findUrl(operation.getOperation(), WFS.METHOD.POST);
            String xml = operation.asXml(new XMLDocumentFactory(nsStore), versions).toString(false);

            return Tuple.of(uri.toString(), xml);

        } catch (TransformerException | ParserConfigurationException | SAXException | IOException e) {
            return null;
        }
    }

    public URI findUrl(WFS.OPERATION operation, WFS.METHOD method) {

        URI uri = this.urls.containsKey(operation.toString()) ? this.urls.get(operation.toString())
                                                                         .get(method) : this.urls.get("default")
                                                                                                 .get(method);

        if (uri == null && method.equals(WFS.METHOD.GET)) {
            return this.urls.get("default")
                            .get(method);
        }

        return uri;
    }
}
