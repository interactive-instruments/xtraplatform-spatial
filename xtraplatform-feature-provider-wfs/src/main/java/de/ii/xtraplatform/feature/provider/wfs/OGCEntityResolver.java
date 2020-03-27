/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import com.google.common.io.CharStreams;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.ogc.api.exceptions.SchemaParseException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author fischer
 */
public class OGCEntityResolver implements EntityResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OGCEntityResolver.class);
    private Map<String, String> uris = new HashMap<>();
    private HttpClient httpClient;
    private boolean useBasicAuth = false;
    private String user;
    private String password;

    public OGCEntityResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public OGCEntityResolver(HttpClient httpClient, String user, String password) {
        this.httpClient = httpClient;
        this.user = user;
        this.password = password;
        this.useBasicAuth = true;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {

        Optional<String> fakeResolved = applyContentWorkarounds(publicId, systemId);

        if (fakeResolved.isPresent()) {
            return new InputSource(new StringReader(fakeResolved.get()));
        }

        String normalizedSystemId = applyUrlWorkarounds(publicId, systemId);

        Map<String, String> headers = new HashMap<>();

        if (useBasicAuth) {
            String basic_auth = new String(Base64.encodeBase64((user + ":" + password).getBytes()));
            headers.put("Authorization", "Basic " + basic_auth);
        }

        try {
            InputStream response = httpClient.getAsInputStream(normalizedSystemId, headers);

            String stringFromStream = CharStreams.toString(new InputStreamReader(response, "UTF-8"));
            InputSource is = new InputSource(new StringReader(stringFromStream));
            is.setSystemId(normalizedSystemId);

            return is;

        } catch (IOException ex) {
            LOGGER.error("Error parsing application schema. {}", ex.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception:", ex);
            }
            throw new SchemaParseException("Error parsing application schema. {}", ex.getMessage());
        }
    }

    private Optional<String> applyContentWorkarounds(String publicId, String systemId) {
        return Optional.ofNullable(
                Optional.ofNullable(applyContentWorkaroundsGeneral(publicId, systemId))
                        .orElse(applyContentWorkaroundsXtraServer(publicId, systemId))
        );
    }

    private String applyContentWorkaroundsGeneral(String publicId, String systemId) {
        // ignore multiple imports into the same namespace
        if (publicId != null) {
            if (!uris.containsKey(publicId)) {
                uris.put(publicId, systemId);
            }
            if (systemId != null && !systemId.equals(uris.get(publicId))) {
                return createFakeSchema(publicId);
            }
        }

        return null;
    }

    private String applyContentWorkaroundsXtraServer(String publicId, String systemId) {
        if (Objects.isNull(publicId) && systemId.contains("&REQUEST=DescribeFeatureType&TYPENAMES=ns:AbstractFeature")) {
            return createFakeSchema("http://www.opengis.net/gml/3.2");
        }

        return null;
    }

    private String applyUrlWorkarounds(String publicId, String systemId) {
        String normalizedSystemId;

        normalizedSystemId = applyUrlWorkaroundsGeneral(publicId, systemId);
        normalizedSystemId = applyUrlWorkaroundsA4I(publicId, systemId);

        return normalizedSystemId;
    }

    private String applyUrlWorkaroundsGeneral(String publicId, String systemId) {
        if (Objects.nonNull(publicId) && !isAvailable(systemId)) {
            switch (publicId) {
                case "http://www.opengis.net/gml":
                    return "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd";
                case "http://www.opengis.net/gml/3.2":
                    return "http://schemas.opengis.net/gml/3.2.1/gml.xsd";
                case "http://www.w3.org/1999/xlink":
                    return "http://www.w3.org/1999/xlink.xsd";
                case "http://www.aixm.aero/schema/5.1":
                    return "http://www.aixm.aero/gallery/content/public/schema/5.1/AIXM_Features.xsd";
            }
        }

        return systemId;
    }

    private String applyUrlWorkaroundsA4I(String publicId, String systemId) {
        if (systemId.contains("/exts/InspireFeatureDownload/service")) {
            String url = systemId;
            // workaround for A4I 10.1 SP1 (Patch1) blank encoding bug in GET parameters
            if (url.contains("OUTPUT_FORMAT=")) {
                int start = url.indexOf("OUTPUT_FORMAT=") + 13;
                int end = url.indexOf("&", start);
                String out = url.substring(start, end)
                                .replaceAll("%20", "");
                url = url.substring(0, start) + out + url.substring(end);
            }

            if (!url.equals(systemId)) {
                return url;
            }
        }

        return systemId;
    }

    private String applyUrlWorkaroundsDwd(String publicId, String systemId) {
        if (Objects.nonNull(systemId)) {
            if (systemId.endsWith("gml.xsd") && systemId.contains("kunden.dwd.de")) {
                return "http://schemas.opengis.net/gml/3.2.1/gml.xsd";
            }
        }

        return systemId;
    }

    private boolean isAvailable(String systemId) {

        try {
            URL url = new URL(systemId);
            URLConnection connection = url.openConnection();

            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;

                int code = httpConnection.getResponseCode();

                if (code == 200) {
                    return true;
                }
            }

        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private String createFakeSchema(String targetNamespace) {
        return "<schema targetNamespace=\"" + targetNamespace + "\" xmlns=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\"></schema>";
    }
}
