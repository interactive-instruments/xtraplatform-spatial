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
package de.ii.ogc.wfs.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xsf.core.api.AbstractService;
import de.ii.xsf.core.api.Resource;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import de.ii.xtraplatform.ogc.api.exceptions.SchemaParseException;
import de.ii.xtraplatform.ogc.api.exceptions.WFSException;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLSchemaAnalyzer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLSchemaParser;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import de.ii.xtraplatform.ogc.api.wfs.client.DescribeFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.codehaus.staxmate.SMInputFactory;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

/**
 * @author zahnen
 */
public abstract class AbstractWfsProxyService extends AbstractService implements Resource, WfsProxyService {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AbstractWfsProxyService.class);

    private WFSAdapter wfsAdapter;
    private WfsProxyCrsTransformations crsTransformations;
    private WFSProxyServiceProperties serviceProperties;
    private final Map<String, WfsProxyFeatureType> featureTypes;
    protected final List<GMLSchemaAnalyzer> schemaAnalyzers;

    protected HttpClient httpClient;
    protected HttpClient sslHttpClient;
    // TODO
    @JsonIgnore
    public SMInputFactory staxFactory;
    // TODO
    @JsonIgnore
    public ObjectMapper jsonMapper;
    protected JsonFactory jsonFactory;

    public AbstractWfsProxyService() {
        super();
        this.featureTypes = new HashMap<>();
        this.schemaAnalyzers = new ArrayList<>();
        this.serviceProperties = new WFSProxyServiceProperties();
    }

    public AbstractWfsProxyService(String id, String type, File configDirectory, WFSAdapter wfsAdapter) {
        super(id, type, configDirectory);
        this.wfsAdapter = wfsAdapter;
        this.featureTypes = new HashMap<>();
        this.schemaAnalyzers = new ArrayList<>();
        this.serviceProperties = new WFSProxyServiceProperties();
    }

    @Override
    public WFSAdapter getWfsAdapter() {
        return wfsAdapter;
    }

    public void setWfsAdapter(WFSAdapter wfsAdapter) {
        this.wfsAdapter = wfsAdapter;
    }

    @Override
    public WFSProxyServiceProperties getServiceProperties() {
        return serviceProperties;
    }

    public void setServiceProperties(WFSProxyServiceProperties serviceProperties) {
        this.serviceProperties = serviceProperties;
    }

    @Override
    public Map<String, WfsProxyFeatureType> getFeatureTypes() {
        return featureTypes;
    }

    public void setFeatureTypes(Map<String, WfsProxyFeatureType> featureTypes) {
        this.featureTypes.putAll(featureTypes);
    }

    @Override
    @JsonIgnore
    public Optional<WfsProxyFeatureType> getFeatureTypeByName(String name) {
        return featureTypes.values().stream().filter(ft -> ft.getName().toLowerCase().equals(name.toLowerCase())).findFirst();
    }

    @JsonIgnore
    public WfsProxyCrsTransformations getCrsTransformations() {
        return crsTransformations;
    }

    public final void initialize(String[] path, HttpClient httpClient, HttpClient sslHttpClient, SMInputFactory staxFactory, ObjectMapper jsonMapper, CrsTransformation crsTransformation) {
        this.httpClient = httpClient;
        this.sslHttpClient = sslHttpClient;
        this.staxFactory = staxFactory;
        this.jsonMapper = jsonMapper;
        this.jsonFactory = new JsonFactory();

        if (this.wfsAdapter != null) {
            this.wfsAdapter.initialize(this.httpClient, this.sslHttpClient);
        }

        this.crsTransformations = new WfsProxyCrsTransformations(crsTransformation, wfsAdapter != null ? wfsAdapter.getDefaultCrs() : null, new EpsgCrs(4326, true));

    }

    // TODO: move somewhere else
    public JsonGenerator createJsonGenerator(OutputStream output) throws IOException {
        JsonGenerator json = jsonFactory.createGenerator(output);
        json.setCodec(jsonMapper);
        //if (useFormattedJsonOutput) {
        json.useDefaultPrettyPrinter();
        //}
        // Zum JSON debuggen hier einschalten.
        //JsonGenerator jsond = new JsonGeneratorDebug(json);
        return json;
    }

    private void analyzeCapabilities() {
        try {
            analyzeCapabilities(null);
        } catch (WFSException ex) {
            for (WFS.VERSION version : WFS.VERSION.values()) {
                try {
                    analyzeCapabilities(version);
                    return;
                } catch (WFSException ex2) {
                    // ignore
                }
            }

            ParseError pe = new ParseError("Parsing of GetCapabilities response failed.");
            pe.addDetail(ex.getMsg());
            for (String det : ex.getDetails()) {
                pe.addDetail(det);
            }
            throw pe;
        }
    }

    private void analyzeCapabilities(WFS.VERSION version) throws ParseError {

        HttpEntity capabilities;
        GetCapabilities getCapabilities;

        if (version == null) {
            LOGGER.debug(FrameworkMessages.ANALYZING_CAPABILITIES);
            getCapabilities = new GetCapabilities();
        } else {
            LOGGER.debug(FrameworkMessages.ANALYZING_CAPABILITIES_VERSION, version.toString());
            getCapabilities = new GetCapabilities(version);
        }
        capabilities = wfsAdapter.request(getCapabilities);

        WFSCapabilitiesAnalyzer analyzer = new WfsProxyCapabilitiesAnalyzer(this, wfsAdapter.getRequestUrl(getCapabilities));
        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, staxFactory);

        wfsParser.parse(capabilities);

        // TODO
        // tell the WFSadapter we are done with the capabilities.
        this.wfsAdapter.capabilitiesAnalyzed();
    }

    private Map<String, List<String>> retrieveSupportedFeatureTypesPerNamespace() {
        Map<String, List<String>> featureTypesPerNamespace = new HashMap<>();

        for (WfsProxyFeatureType featureType : featureTypes.values()) {
            if (!featureTypesPerNamespace.containsKey(featureType.getNamespace())) {
                featureTypesPerNamespace.put(featureType.getNamespace(), new ArrayList<String>());
            }
            featureTypesPerNamespace.get(featureType.getNamespace()).add(featureType.getName());
        }

        return featureTypesPerNamespace;
    }

    public void analyzeFeatureTypes() {
        WfsProxyMappingStatus mappingStatus = serviceProperties.getMappingStatus();
        //mappingStatus.setEnabled(!disableMapping);
        //mappingStatus.setLoading(!disableMapping);

        // TODO: if loading, run analysis in background queue
        // separate (or this?) function, start from store
        if (mappingStatus.isEnabled() && mappingStatus.isLoading()) {
            //mappingStatus.setEnabled(true);
            //mappingStatus.setLoading(true);

            HttpEntity dft = wfsAdapter.request(new DescribeFeatureType());
            // TODO: ???
            URI baseuri = wfsAdapter.findUrl(WFS.OPERATION.DESCRIBE_FEATURE_TYPE, WFS.METHOD.GET);

            Map<String, List<String>> fts = retrieveSupportedFeatureTypesPerNamespace();

            if (!featureTypes.isEmpty()) {
                // create mappings
                GMLSchemaParser gmlSchemaParser;
                // TODO: temporary basic auth hack
                //if (wfs.usesBasicAuth()) {
                //    gmlParser = new GMLSchemaParser(analyzers, baseuri, new OGCEntityResolver(sslHttpClient, wfs.getUser(), wfs.getPassword()));
                //} else {
                gmlSchemaParser = new GMLSchemaParser(schemaAnalyzers, baseuri);
                //}
                try {
                    gmlSchemaParser.parse(dft, fts);

                    mappingStatus.setLoading(false);
                    mappingStatus.setSupported(true);
                } catch (Exception ex) {
                    mappingStatus.setLoading(false);
                    mappingStatus.setSupported(false);
                    mappingStatus.setErrorMessage(ex.getMessage());
                    if (ex.getClass() == SchemaParseException.class) {
                        mappingStatus.setErrorMessageDetails(((SchemaParseException)ex).getDetails());
                    }
                }
            }

            // only log warnings about timeouts in the analysis phase
            //wfsAdapter.setIgnoreTimeouts(true);
        }
    }

    public void analyzeWFS() {
        LOGGER.debug(FrameworkMessages.ANALYZING_WFS);
        analyzeCapabilities();

        if (wfsAdapter.getDefaultCrs() == null) {
            ParseError pe = new ParseError(FrameworkMessages.NO_VALID_SRS_FOUND_IN_GETCAPABILITIES_RESPONSE);
            throw pe;
        }

        crsTransformations.setWfsDefaultCrs(wfsAdapter.getDefaultCrs());

        // TODO: background tests, check method, check urls
        //wfsAdapter.checkHttpMethodSupport();

        // TODO: analyze functionality like paging in background queue


    }

    @Override
    public String getResourceId() {
        return getId();
    }

    @Override
    public void setResourceId(String id) {
        setId(id);
    }

    @Override
    public String getBrowseUrl() {
        return getId() + "/";
    }

    @Override
    protected void internalStart() {

    }

    @Override
    protected void internalStop() {

    }


    // TODO
    @Override
    public void update(String s) {
    }

    // TODO
    @Override
    public void load() throws IOException {
    }

    // TODO
    @Override
    public void save() {
    }

    @Override
    public void delete() {
        // TODO: iterate layers
    }

    @Override
    public void invalidate() {
        // TODO: iterate layers
    }
}
