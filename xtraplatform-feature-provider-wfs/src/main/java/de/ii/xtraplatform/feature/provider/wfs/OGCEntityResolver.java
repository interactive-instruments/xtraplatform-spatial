/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.akka.http.HttpClient;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author fischer
 */
public class OGCEntityResolver implements EntityResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(OGCEntityResolver.class);
  private static final Map<String, HttpClient> HTTP_CLIENTS = new HashMap<>();

  private final Map<String, String> uris = new HashMap<>();
  private final Map<String, String> cache = new HashMap<>();
  private final Http http;

  public OGCEntityResolver(Http http, URI defaultUri, HttpClient defaultClient) {
    this.http = http;
    HTTP_CLIENTS.putIfAbsent(getHostKey(defaultUri), defaultClient);
  }

  private String getHostKey(URI uri) {
    return String.format("%s://%s:%d", uri.getScheme(), uri.getHost(), uri.getPort());
  }

  private HttpClient getClientForUri(URI uri) {
    String hostKey = getHostKey(uri);

    HTTP_CLIENTS.computeIfAbsent(hostKey, key -> http.getHostClient(uri, 2));

    return HTTP_CLIENTS.get(hostKey);
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId)
      throws SAXException, IOException {

    String actualSystemId = systemId;

    if (Objects.nonNull(systemId)) {
      // workarounds for unresolvable mirrors of public schemas, needed for pegelonline
      if (Objects.equals(publicId, "http://www.opengis.net/gml")) {
        if (isNotResolvable(systemId)) {
          actualSystemId = "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd";
        }
      }
      if (Objects.equals(publicId, "http://www.opengis.net/gml/3.2")) {
        if (isNotResolvable(systemId)) {
          actualSystemId = "http://schemas.opengis.net/gml/3.2.1/gml.xsd";
        }
      }
      if (Objects.equals(publicId, "http://www.w3.org/1999/xlink")) {
        if (isNotResolvable(systemId)) {
          actualSystemId = "http://www.w3.org/1999/xlink.xsd";
        }
      }

      // workaround for A4I DescribeFeatureType (seen in 10.2.1)
      // also occurs with native XtraServer, moved to general workarounds
      if (Objects.isNull(publicId) && systemId.contains("&REQUEST=DescribeFeatureType&TYPENAMES=ns:AbstractFeature")) {
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
          LOGGER.debug("original systemId: {}", systemId);
          LOGGER.debug("changed systemId: {}", url);
          actualSystemId = url;
        }
      }
    }

    // ignore multiple imports into the same namespace
    if (Objects.nonNull(publicId)) {
      uris.putIfAbsent(publicId, actualSystemId);

      if (!Objects.equals(actualSystemId, uris.get(publicId))) {
        return createFakeSchema(publicId);
      }
    }

    return resolveUrl(systemId);
  }

  private InputSource resolveUrl(String url) {
    String response;

    if (cache.containsKey(url)) {
      response = cache.get(url);
    } else {
      try {
        URI uri = URI.create(url);

        response = getClientForUri(uri).getAsString(url);

        cache.put(url, response);
      } catch (Throwable e) {
        throw new IllegalStateException("Error parsing application schema: " + e.getMessage(), e);
      }
    }

    InputSource is = new InputSource(new StringReader(response));
    is.setSystemId(url);

    return is;
  }

  private boolean isNotResolvable(String systemId) {
    try {
      URL url = new URL(systemId);
      URLConnection connection = url.openConnection();

      if (connection instanceof HttpURLConnection) {
        HttpURLConnection httpConnection = (HttpURLConnection) connection;

        int code = httpConnection.getResponseCode();

        if (code == 200) {
          return false;
        }
      }
    } catch (Throwable e) {
      // continue
    }

    return true;
  }

  private InputSource createFakeSchema(String ns) {
    return new InputSource(new StringReader("<schema targetNamespace=\"" + ns
        + "\" xmlns=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\"></schema>"));
  }
}
