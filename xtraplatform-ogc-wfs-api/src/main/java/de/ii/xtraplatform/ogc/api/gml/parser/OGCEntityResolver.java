/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.api.gml.parser;

import com.google.common.io.CharStreams;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.exceptions.SchemaParseException;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author fischer
 */
public class OGCEntityResolver implements EntityResolver {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(OGCEntityResolver.class);
    private Map<String, String> uris = new HashMap();
    private HttpClient untrustedSslHttpClient;
    private boolean useBasicAuth = false;
    private String user;
    private String password;

    public OGCEntityResolver(HttpClient untrustedSslHttpClient, String user, String password) {
        this.untrustedSslHttpClient = untrustedSslHttpClient;
        this.user = user;
        this.password = password;
        this.useBasicAuth = true;
    }

    public OGCEntityResolver() {
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {

        // TODO: temporary basic auth hack
        // protected schema files
        if (systemId != null && systemId.startsWith("https://") && useBasicAuth) {
            HttpResponse response = null;
            LOGGER.getLogger().debug("resolving protected schema: {}", systemId);
            try {
                HttpGet httpGet = new HttpGet(systemId);

                String basic_auth = new String(Base64.encodeBase64((user + ":" + password).getBytes()));
                httpGet.addHeader("Authorization", "Basic " + basic_auth);

                response = untrustedSslHttpClient.execute(httpGet, new BasicHttpContext());
                String stringFromStream = CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                InputSource is = new InputSource(new StringReader(stringFromStream));
                is.setSystemId(systemId);

                return is;

            } catch (IOException ex) {
                ex.printStackTrace();
                LOGGER.error(FrameworkMessages.ERROR_PARSING_APPLICATION_SCHEMA, ex);
                throw new SchemaParseException(FrameworkMessages.ERROR_PARSING_APPLICATION_SCHEMA, ex.getMessage());
            } finally {
                if (response != null) {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        }

        //LOGGER.info(" --- {} --- {} ", systemId, publicId);
        if (publicId != null && publicId.equals("http://www.opengis.net/gml")) {
            if (!isAvailable(systemId)) {
                return new InputSource("http://schemas.opengis.net/gml/3.1.1/base/gml.xsd");
            }
        }
        if (publicId != null && publicId.equals("http://www.opengis.net/gml/3.2")) {
            if (!isAvailable(systemId)) {
                return new InputSource("http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            }
        }
        if (publicId != null && publicId.equals("http://www.w3.org/1999/xlink")) {
            if (!isAvailable(systemId)) {
                return new InputSource("http://www.w3.org/1999/xlink.xsd");
            }
        }

        if (publicId != null && publicId.equals("http://www.aixm.aero/schema/5.1")) {
            if (!isAvailable(systemId)) {
                return new InputSource("http://www.aixm.aero/gallery/content/public/schema/5.1/AIXM_Features.xsd");
            }
        }

        if (systemId != null) {
            // Workaround for broken Schema in dwd-WFS
            if (systemId.endsWith("gml.xsd") && systemId.contains("kunden.dwd.de")) {
                return new InputSource("http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            }

            /*if (systemId.endsWith("basicTypes.xsd") && redirect.contains("http://www.opengis.net/gml/3.2")) {
             return new InputSource("http://schemas.opengis.net/gml/3.2.1/basicTypes.xsd");
             }

             if (systemId.endsWith("xlinks.xsd") && redirect.contains("http://www.w3.org/1999/xlink")) {
             return new InputSource("http://www.w3.org/1999/xlink.xsd");
             }*/
            // workaround for A4I DescribeFeatureType (seen in 10.2.1)
            // also occurs with native XtraServer, moved to general workarounds
            if (publicId == null && systemId.contains("&REQUEST=DescribeFeatureType&TYPENAMES=ns:AbstractFeature")) {
                return createFakeSchema("http://www.opengis.net/gml/3.2");
            }

            // A4I workarounds
            if (systemId.contains("/exts/InspireFeatureDownload/service")) {
                String url = systemId;
                // workaround for A4I 10.1 SP1 (Patch1) blank encoding bug in GET parameters
                if (url.contains("OUTPUT_FORMAT=")) {
                    int start = url.indexOf("OUTPUT_FORMAT=") + 13;
                    int end = url.indexOf("&", start);
                    String out = url.substring(start, end).replaceAll("%20", "");
                    url = url.substring(0, start) + out + url.substring(end);
                }

                if (!url.equals(systemId)) {
                    LOGGER.getLogger().debug("original systemId: {}", systemId);
                    LOGGER.getLogger().debug("changed systemId: {}", url);
                    return new InputSource(url);
                }
            }
        }

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

    private InputSource createFakeSchema(String ns) {
        return new InputSource(new StringReader("<schema targetNamespace=\"" + ns + "\" xmlns=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\"></schema>"));
    }
}
