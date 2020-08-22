/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.csw.client;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.ogc.api.exceptions.ReadError;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public class CSWAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSWAdapter.class);
    private static final String DEFAULT_OPERATION = "default";

    private Map<String, Map<CSW.METHOD, URI>> urls;
    private CSW.VERSION version;
    private HttpClient httpClient;
    private HttpClient untrustedSslHttpClient;
    private XMLNamespaceNormalizer nsStore;
    private boolean ignoreTimeouts = false;
    private CSW.METHOD httpMethod;
    private boolean useBasicAuth = false;
    private String user;
    private String password;

    public CSWAdapter() {
        this.urls = new HashMap<>();
        this.nsStore = new XMLNamespaceNormalizer();
        this.httpMethod = CSW.METHOD.GET;
    }

    public CSWAdapter(String url) {
        this();
        try {
            // TODO: temporary basic auth hack
            // extract and remove credentials from url if existing
            //URI noAuthUri = this.extractBasicAuthCredentials(new URI(url));
            URI noAuthUri = parseAndCleanWfsUrl(url);
            Map<CSW.METHOD, URI> urls = new ImmutableMap.Builder<CSW.METHOD, URI>()
                .put(CSW.METHOD.GET, noAuthUri)
                .put(CSW.METHOD.POST, noAuthUri)
                .build();

            this.urls.put(DEFAULT_OPERATION, urls);
        } catch (URISyntaxException ex) {
            //LOGGER.error(FrameworkMessages.INVALID_WFS_URL, url);
            throw new IllegalStateException(ex);
        }
    }

    public void initialize(HttpClient httpClient, HttpClient untrustedSslhttpClient) {
        this.httpClient = httpClient;
        this.untrustedSslHttpClient = untrustedSslhttpClient;
    }

    public Map<String, Map<CSW.METHOD, URI>> getUrls() {
        return this.urls;
    }

    public void setUrls(Map<String, Map<CSW.METHOD, URI>> urls) {
        this.urls = urls;
    }

    public void addUrl(URI url, CSW.OPERATION op, CSW.METHOD method) {
        // TODO: remove toString
        if (!this.urls.containsKey(op.toString())) {
            Map<CSW.METHOD, URI> urls = new ImmutableMap.Builder<CSW.METHOD, URI>()
                    .put(method, url)
                    .build();
            this.urls.put(op.toString(), urls);
        }
    }

    public String getVersion() {
        return version != null ? version.toString() : null;
    }

    public void setVersion(String version) {
        CSW.VERSION v = CSW.VERSION.fromString(version);
        if (v != null) {
            if (this.version == null || v.isGreater(this.version)) {
                this.version = v;
                //LOGGER.debug(FrameworkMessages.VERSION_SET_TO, version);
            }
        }
    }

    public HttpEntity request(CSWOperation operation) {

        try {
            // TODO: POST or GET
            HttpResponse r;
            if (httpMethod == CSW.METHOD.POST)
                r = requestPOST(operation);
            else
                r = requestGET(operation);
            operation.setResponseHeaders(r.getAllHeaders());
            return r.getEntity();
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

    private HttpResponse requestPOST(CSWOperation operation) throws ParserConfigurationException {

        URI url = findUrl(operation.getOperation(), CSW.METHOD.POST);

        HttpClient httpClient = url.getScheme().equals("https") ? this.untrustedSslHttpClient : this.httpClient;

        URIBuilder uri = new URIBuilder(url);

        String xml = operation.toXml(nsStore, version);
        
        LOGGER.debug("{}\n{}", uri, xml);
        
        HttpPost httpPost;
        HttpResponse response = null;

        try {
            httpPost = new HttpPost(uri.build());
            
            for( String key : operation.getRequestHeaders().keySet()) {
                httpPost.setHeader(key, operation.getRequestHeaders().get(key));
            }

            // TODO: temporary basic auth hack
            if (useBasicAuth) {
                String basic_auth = new String(Base64.encodeBase64((user + ":" + password).getBytes()));
                httpPost.addHeader("Authorization", "Basic " + basic_auth);
            }

            StringEntity xmlEntity = new StringEntity(xml,
                    ContentType.create("text/plain", "UTF-8"));
            httpPost.setEntity(xmlEntity);
            
            response = httpClient.execute(httpPost, new BasicHttpContext());

            // check http status
            checkResponseStatus(response.getStatusLine().getStatusCode(), uri);

        } catch (SocketTimeoutException ex) {
            if (ignoreTimeouts) {
                //LOGGER.warn(FrameworkMessages.POST_REQUEST_TIMED_OUT_AFTER_MS_URL_REQUEST, HttpConnectionParams.getConnectionTimeout(httpClient.getParams()), uri.toString(), xml);
            }
            response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
            response.setEntity(new StringEntity("", ContentType.TEXT_XML));
        } catch (IOException ex) {
            //LOGGER.error(ERROR_IN_POST_REQUEST_TO_URL_REQUEST, uri.toString(), xml, ex);
            //LOGGER.debug("Error requesting URL: {}", uri.toString());

            try {
                if (!isDefaultUrl(uri.build(), CSW.METHOD.POST)) {

                    //LOGGER.info(FrameworkMessages.REMOVING_URL, uri.toString());
                    this.urls.remove(operation.getOperation().toString());

                    //LOGGER.info(FrameworkMessages.RETRY_WITH_DEFAULT_URL, this.urls.get("default"));
                    return requestPOST(operation);
                }
            } catch (URISyntaxException ex0) {
            }

            //LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new RuntimeException(ex.getMessage(), ex);
        } catch (URISyntaxException ex) {
            //LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new RuntimeException(ex.getMessage(), ex);
        } catch (ReadError ex) {
            //LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new RuntimeException(ex.getMessage(), ex);
        }
        //LOGGER.debug(FrameworkMessages.WFS_REQUEST_SUBMITTED);
        return response;
    }

    public String getRequestUrl(CSWOperation operation) {
        try {
        URIBuilder uri = new URIBuilder(findUrl(operation.getOperation(), CSW.METHOD.GET));

        Map<String, String> params = operation.toKvp(nsStore, version);

        for (Map.Entry<String, String> param : params.entrySet()) {
            uri.addParameter(param.getKey(), param.getValue());
        }


            return uri.build().toString();
        } catch (URISyntaxException | ParserConfigurationException e) {
            return "";
        }
    }

    private HttpResponse requestGET(CSWOperation operation) throws ParserConfigurationException {
        URI url = findUrl(operation.getOperation(), CSW.METHOD.GET);

        HttpClient httpClient = url.getScheme().equals("https") ? this.untrustedSslHttpClient : this.httpClient;

        URIBuilder uri = new URIBuilder(url);

        Map<String, String> params = operation.toKvp(nsStore, version);

        for (Map.Entry<String, String> param : params.entrySet()) {
            uri.addParameter(param.getKey(), param.getValue());
        }
        //LOGGER.debug(FrameworkMessages.GET_REQUEST_OPERATION_URL, operation.toString(), uri.toString());

        boolean retried = false;
        HttpGet httpGet;
        HttpResponse response;

        try {

            // replace the + with %20
            String uristring = uri.build().toString();
            uristring = uristring.replaceAll("\\+", "%20");
            httpGet = new HttpGet(uristring);

            // TODO: temporary basic auth hack
            if (useBasicAuth) {
                String basic_auth = new String(Base64.encodeBase64((user + ":" + password).getBytes()));
                httpGet.addHeader("Authorization", "Basic " + basic_auth);
            }

            response = httpClient.execute(httpGet, new BasicHttpContext());

            // check http status
            checkResponseStatus(response.getStatusLine().getStatusCode(), uri);

        } catch (SocketTimeoutException ex) {
            if (ignoreTimeouts) {
                //LOGGER.warn(FrameworkMessages.GET_REQUEST_TIMED_OUT_AFTER_MS_URL_REQUEST, HttpConnectionParams.getConnectionTimeout(httpClient.getParams()), uri.toString());
            }
            response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
            response.setEntity(new StringEntity("", ContentType.TEXT_XML));
        } catch (IOException ex) {
            try {
                if (!isDefaultUrl(uri.build(), CSW.METHOD.GET)) {

                    //LOGGER.info(FrameworkMessages.REMOVING_URL, uri.toString());
                    this.urls.remove(operation.getOperation().toString());

                    //LOGGER.info(FrameworkMessages.RETRY_WITH_DEFAULT_URL, this.urls.get("default"));
                    return requestGET(operation);
                }
            } catch (URISyntaxException ex0) {
            }
            //LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new RuntimeException(ex.getMessage(), ex);
        } catch (URISyntaxException ex) {
            //LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new RuntimeException(ex.getMessage(), ex);
        }
        //LOGGER.debug(FrameworkMessages.WFS_REQUEST_SUBMITTED);
        return response;
    }

    private void checkResponseStatus(int status, URIBuilder uri) {
        if (status >= 400) {
            String reason = "No reason available";
            try {
                reason = status + " " + Response.Status.fromStatusCode(status).getReasonPhrase();
            } catch (Exception e) {
            }
            //LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL_REASON, uri.toString(), reason);
            throw new RuntimeException(String.format("Failed requesting URL: '%s'. Reason: %s", uri.toString(), reason));
        }
    }

    public XMLNamespaceNormalizer getNsStore() {
        return nsStore;
    }

    public void setNsStore(XMLNamespaceNormalizer nsStore) {
        this.nsStore = nsStore;
    }

    public void addNamespace(String namespaceURI) {
        nsStore.addNamespace(namespaceURI);
    }

    public void addNamespace(String prefix, String namespaceURI) {
        nsStore.addNamespace(prefix, namespaceURI);
    }

    public String retrieveNamespaceURI(String prefix) {
        return nsStore.getNamespaceURI(prefix);
    }

    public URI findUrl(CSW.OPERATION operation, CSW.METHOD method) {

        URI uri = this.urls.containsKey(operation.toString()) ? this.urls.get(operation.toString()).get(method) : this.urls.get("default").get(method);

        if (uri == null && method.equals(CSW.METHOD.GET)) {
            return this.urls.get("default").get(method);
        }

        return uri;
    }

    private boolean isDefaultUrl(URI uri, CSW.METHOD method) {
        URI defaultURI = this.urls.get("default").get(method);
        URI inputURI = uri;

        if (defaultURI.getHost().startsWith(inputURI.getHost())) {
            return true;
        }
        return false;
    }

    public void setIgnoreTimeouts(boolean ignoreTimeouts) {
        this.ignoreTimeouts = ignoreTimeouts;
    }


    public String getHttpMethod() {
        return httpMethod.toString();
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = CSW.METHOD.fromString(httpMethod);
    }

    public static URI parseAndCleanWfsUrl(String url) throws URISyntaxException {
        URI inUri = new URI(url.trim());
        URIBuilder outUri = new URIBuilder(inUri).removeQuery();

        if (inUri.getQuery() != null && !inUri.getQuery().isEmpty()) {
            for (String inParam : inUri.getQuery().split("&")) {
                String[] param = inParam.split("=");
                if (!CSW.hasKVPKey(param[0].toUpperCase())) {
                    outUri.addParameter(param[0], param[1]);
                }
            }
        }

        return outUri.build();
    }
}
