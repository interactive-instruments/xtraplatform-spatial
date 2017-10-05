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
package de.ii.xtraplatform.ogc.api.wfs.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.io.CharStreams;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.exceptions.ReadError;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
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
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class WFSAdapter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WFSAdapter.class);
    private static final String DEFAULT_OPERATION = "default";

    private Map<String, Map<WFS.METHOD, URI>> urls;
    //private WFS.VERSION version;
    //private GML.VERSION gmlVersion;
    private Versions versions;
    private HttpClient httpClient;
    private HttpClient untrustedSslHttpClient;
    private XMLNamespaceNormalizer nsStore;
    private boolean alphaNumericId;
    private EpsgCrs defaultCrs;
    private Set<EpsgCrs> otherCrs;
    private boolean ignoreTimeouts ;
    private WFS.METHOD httpMethod;
    private boolean useBasicAuth;
    private String user;
    private String password;

    public WFSAdapter() {
        this.urls = new HashMap<>();
        this.nsStore = new XMLNamespaceNormalizer();
        this.alphaNumericId = false;
        this.versions = new Versions();
        this.defaultCrs = null;
        this.otherCrs = new HashSet<>();
        this.ignoreTimeouts = true;
        this.httpMethod = WFS.METHOD.GET;
        this.useBasicAuth = false;
    }

    public WFSAdapter(String url) {
        this();

        try {
            // TODO: temporary basic auth hack
            // extract and remove credentials from url if existing
            //URI noAuthUri = this.extractBasicAuthCredentials(new URI(url));
            URI noAuthUri = parseAndCleanWfsUrl(url);
            Map<WFS.METHOD, URI> urls = new ImmutableMap.Builder<WFS.METHOD, URI>()
                .put(WFS.METHOD.GET, noAuthUri)
                .put(WFS.METHOD.POST, noAuthUri)
                .build();

            this.urls.put(DEFAULT_OPERATION, urls);
        } catch (URISyntaxException ex) {
            LOGGER.error(FrameworkMessages.INVALID_WFS_URL, url);
            throw new WebApplicationException();
        }
    }

    public void initialize(HttpClient httpClient, HttpClient untrustedSslhttpClient) {
        this.httpClient = httpClient;
        this.untrustedSslHttpClient = untrustedSslhttpClient;
    }

    public EpsgCrs getDefaultCrs() {
        return defaultCrs;
    }

    public void setDefaultCrs(EpsgCrs defaultCrs) {
        if (this.defaultCrs == null) {
            this.defaultCrs = defaultCrs;
            LOGGER.getLogger().debug("added default CRS {} {}", defaultCrs.getAsUrn(), defaultCrs.isLongitudeFirst() ? "LonLat" : "LatLon");
            this.otherCrs.add(defaultCrs);
        }
    }

    public List<EpsgCrs> getOtherCrs() {
        return Lists.newArrayList(otherCrs);
    }

    public void addOtherCrs(EpsgCrs otherCrs) {
        if (this.otherCrs.add(otherCrs)) {
            LOGGER.debug(FrameworkMessages.ADDED_SRS, otherCrs.getAsUrn());
            if (this.defaultCrs == null) {
                this.setDefaultCrs(otherCrs);
            }
        }
    }

    public boolean supportsCrs(EpsgCrs crs) {
        return otherCrs.contains(crs);

        // TODO: equals works? defaultCrs in otherCrs?
        /*if (other.getCode() == getDefaultCrs().getCode()) {
            return true;
        }

        for (EpsgCrs sr : this.otherCrs) {
            if (sr.getCode() == other.getCode()) {
                return true;
            }
        }
        return false;*/
    }

    public void setOtherCrs(List<EpsgCrs> otherCrs) {
        this.otherCrs.addAll(otherCrs);
    }

    public Map<String, Map<WFS.METHOD, URI>> getUrls() {
        return this.urls;
    }

    public void setUrls(Map<String, Map<WFS.METHOD, URI>> urls) {
        this.urls = urls;
    }

    public void addUrl(URI url, WFS.OPERATION op, WFS.METHOD method) {
        // TODO: remove toString
        if (!this.urls.containsKey(op.toString())) {
            this.urls.put(op.toString(), new HashMap<WFS.METHOD, URI>());
        }
            this.urls.get(op.toString()).put(method, url);
    }

    public boolean isAlphaNumericId() {
        return alphaNumericId;
    }

    public void setAlphaNumericId(boolean alphaNumericId) {
        this.alphaNumericId = alphaNumericId;
    }

    public String getVersion() {
        return versions != null && versions.getWfsVersion() != null ? versions.getWfsVersion().toString() : null;
    }

    public void setVersion(String version) {
        WFS.VERSION v = WFS.VERSION.fromString(version);
        if (v != null) {
            if (this.versions.getWfsVersion() == null || v.isGreater(this.versions.getWfsVersion())) {
                this.versions.setWfsVersion(v);
                LOGGER.debug(FrameworkMessages.VERSION_SET_TO, version);
            }
        }
    }

    public void capabilitiesAnalyzed() {
        if (versions.getGmlVersion() == null && versions.getWfsVersion() != null) {
            versions.setGmlVersion(versions.getWfsVersion().getGmlVersion());
        }
    }

    public String getGmlVersion() {
        if (versions.getGmlVersion() == null) {
            return null;
        }
        return versions.getGmlVersion().toString();
    }

    public void setGmlVersion(String gmlversion) {
        GML.VERSION v = GML.VERSION.fromString(gmlversion);
        this.versions.setGmlVersion(v);
    }

    public void setGmlVersionFromOutputFormat(String outputFormatString) {
        GML.VERSION v = GML.VERSION.fromOutputFormatString(outputFormatString);
        if (v != null) {
            if (this.versions.getGmlVersion() == null || v.isGreater(this.versions.getGmlVersion())) {
                this.versions.setGmlVersion(v);
                LOGGER.debug(FrameworkMessages.VERSION_SET_TO, " GML: " + this.versions.getGmlVersion().toString());
            }
        } else { // Parsing of gml version was not successful, set the default version
            this.versions.setGmlVersion(this.versions.getWfsVersion().getGmlVersion());
        }
    }

    public HttpEntity request(WFSOperation operation) {

        // TODO: POST or GET
        // temporal workaround for beta
        //URIBuilder uriGET = new URIBuilder(findUrl(operation.getOperation(), WFS.METHOD.GET));
        URI uriGET = findUrl(operation.getOperation(), WFS.METHOD.GET);
        URI uriPOST = findUrl(operation.getOperation(), WFS.METHOD.POST);

        if (httpMethod == WFS.METHOD.GET || uriGET.toString().contains("wsinspire.geoportail.lu") || uriPOST.toString().contains("wsinspire.geoportail.lu")) {
            return requestGET(operation).getEntity();
        }

        //URIBuilder uriPOST = new URIBuilder(findUrl(operation.getOperation(), WFS.METHOD.POST));
        HttpResponse r = requestPOST(operation);
        operation.setResponseHeaders(r.getAllHeaders());
        return r.getEntity();
    }

    private HttpResponse requestPOST(WFSOperation operation) {

        URI url = findUrl(operation.getOperation(), WFS.METHOD.POST);

        HttpClient httpClient = url.getScheme().equals("https") ? this.untrustedSslHttpClient : this.httpClient;

        URIBuilder uri = new URIBuilder(url);

        String xml = operation.getPOSTXML(nsStore, versions);
        
        LOGGER.getLogger().debug("{}\n{}", uri, xml);
        
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
                LOGGER.warn(FrameworkMessages.POST_REQUEST_TIMED_OUT_AFTER_MS_URL_REQUEST, HttpConnectionParams.getConnectionTimeout(httpClient.getParams()), uri.toString(), xml);
            }
            response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
            response.setEntity(new StringEntity("", ContentType.TEXT_XML));
        } catch (IOException ex) {
            //LOGGER.error(ERROR_IN_POST_REQUEST_TO_URL_REQUEST, uri.toString(), xml, ex);
            //LOGGER.debug("Error requesting URL: {}", uri.toString());

            try {
                if (!isDefaultUrl(uri.build(), WFS.METHOD.POST)) {

                    LOGGER.info(FrameworkMessages.REMOVING_URL, uri.toString());
                    this.urls.remove(operation.getOperation().toString());

                    LOGGER.info(FrameworkMessages.RETRY_WITH_DEFAULT_URL, this.urls.get("default"));
                    return requestPOST(operation);
                }
            } catch (URISyntaxException ex0) {
            }

            LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new ReadError(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
        } catch (URISyntaxException ex) {
            LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new ReadError(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
        } catch (ReadError ex) {
            LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw ex;
        }
        LOGGER.debug(FrameworkMessages.WFS_REQUEST_SUBMITTED);
        return response;
    }

    public String getRequestUrl(WFSOperation operation) {
        URIBuilder uri = new URIBuilder(findUrl(operation.getOperation(), WFS.METHOD.GET));

        Map<String, String> params = operation.getGETParameters(nsStore, versions);

        for (Map.Entry<String, String> param : params.entrySet()) {
            uri.addParameter(param.getKey(), param.getValue());
        }

        try {
            return uri.build().toString();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private HttpResponse requestGET(WFSOperation operation) {
        URI url = findUrl(operation.getOperation(), WFS.METHOD.GET);

        HttpClient httpClient = url.getScheme().equals("https") ? this.untrustedSslHttpClient : this.httpClient;

        URIBuilder uri = new URIBuilder(url);

        Map<String, String> params = operation.getGETParameters(nsStore, versions);

        for (Map.Entry<String, String> param : params.entrySet()) {
            uri.addParameter(param.getKey(), param.getValue());
        }
        LOGGER.debug(FrameworkMessages.GET_REQUEST_OPERATION_URL, operation.toString(), uri.toString());

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
                LOGGER.warn(FrameworkMessages.GET_REQUEST_TIMED_OUT_AFTER_MS_URL_REQUEST, HttpConnectionParams.getConnectionTimeout(httpClient.getParams()), uri.toString());
            }
            response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
            response.setEntity(new StringEntity("", ContentType.TEXT_XML));
        } catch (IOException ex) {
            try {
                if (!isDefaultUrl(uri.build(), WFS.METHOD.GET)) {

                    LOGGER.info(FrameworkMessages.REMOVING_URL, uri.toString());
                    this.urls.remove(operation.getOperation().toString());

                    LOGGER.info(FrameworkMessages.RETRY_WITH_DEFAULT_URL, this.urls.get("default"));
                    return requestGET(operation);
                }
            } catch (URISyntaxException ex0) {
            }
            LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new ReadError(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
        } catch (URISyntaxException ex) {
            LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            throw new ReadError(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
        }
        LOGGER.debug(FrameworkMessages.WFS_REQUEST_SUBMITTED);
        return response;
    }

    private void checkResponseStatus(int status, URIBuilder uri) {
        if (status >= 400) {
            String reason = "No reason available";
            try {
                reason = status + " " + Response.Status.fromStatusCode(status).getReasonPhrase();
            } catch (Exception e) {
            }
            LOGGER.error(FrameworkMessages.FAILED_REQUESTING_URL_REASON, uri.toString(), reason);
            ReadError re = new ReadError(FrameworkMessages.FAILED_REQUESTING_URL, uri.toString());
            re.addDetail("Reason: " + reason);
            throw re;
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

    public URI findUrl(WFS.OPERATION operation, WFS.METHOD method) {

        URI uri = this.urls.containsKey(operation.toString()) ? this.urls.get(operation.toString()).get(method) : this.urls.get("default").get(method);

        if (uri == null && method.equals(WFS.METHOD.GET)) {
            return this.urls.get("default").get(method);
        }

        return uri;
    }

    private boolean isDefaultUrl(URI uri, WFS.METHOD method) {
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

    /*public void checkHttpMethodSupport() {

        URI url = findUrl(WFS.OPERATION.DESCRIBE_FEATURE_TYPE, WFS.METHOD.POST);
        if (url == null) {
            LOGGER.warn(FrameworkMessages.WFS_DOES_NOT_SUPPORT_HTTP_METHOD_POST_USING_GET_INSTEAD);
            return;
        }

        HttpEntity dft = null;
        try {
            dft = requestPOST(new DescribeFeatureType()).getEntity();

            String response = CharStreams.toString(new InputStreamReader(dft.getContent()));

            if (response.contains("schema>")) {
                httpMethod = WFS.METHOD.POST;
            } else {
                LOGGER.warn(FrameworkMessages.WFS_DOES_NOT_SUPPORT_HTTP_METHOD_POST_USING_GET_INSTEAD);
            }
        } catch (IOException | ReadError ex) {
            LOGGER.warn(FrameworkMessages.WFS_DOES_NOT_SUPPORT_HTTP_METHOD_POST_USING_GET_INSTEAD);
        } finally {
            EntityUtils.consumeQuietly(dft);
        }
    }*/

    public String getHttpMethod() {
        return httpMethod.toString();
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = WFS.METHOD.fromString(httpMethod);
    }

    public static URI parseAndCleanWfsUrl(String url) throws URISyntaxException {
        URI inUri = new URI(url.trim());
        URIBuilder outUri = new URIBuilder(inUri).removeQuery();

        if (inUri.getQuery() != null && !inUri.getQuery().isEmpty()) {
            for (String inParam : inUri.getQuery().split("&")) {
                String[] param = inParam.split("=");
                if (!WFS.hasKVPKey(param[0].toUpperCase())) {
                    outUri.addParameter(param[0], param[1]);
                }
            }
        }

        return outUri.build();
    }
}
