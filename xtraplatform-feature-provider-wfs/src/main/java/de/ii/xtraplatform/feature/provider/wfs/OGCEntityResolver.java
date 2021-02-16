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
import java.util.HashMap;
import java.util.Map;
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

    if (systemId != null) {
      // workaround for A4I DescribeFeatureType (seen in 10.2.1)
      // also occurs with native XtraServer, moved to general workarounds
      if (publicId == null && systemId
          .contains("&REQUEST=DescribeFeatureType&TYPENAMES=ns:AbstractFeature")) {
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
          return resolveUrl(url);
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

    return resolveUrl(systemId);
  }

  private InputSource resolveUrl(String url) {
    try {
      URI uri = URI.create(url);

      String response = getClientForUri(uri).getAsString(url);

      InputSource is = new InputSource(new StringReader(response));
      is.setSystemId(url);

      return is;
    } catch (Throwable e) {
      throw new IllegalStateException("Error parsing application schema: " + e.getMessage(), e);
    }
  }

  private InputSource createFakeSchema(String ns) {
    return new InputSource(new StringReader("<schema targetNamespace=\"" + ns
        + "\" xmlns=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\"></schema>"));
  }
}
