package de.ii.xtraplatform.feature.provider.wfs.infra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderDataWfsFromMetadata;
import de.ii.xtraplatform.feature.provider.wfs.GMLSchemaParser;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.DescribeFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.WfsRequestEncoder;
import de.ii.xtraplatform.scheduler.api.TaskProgress;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WfsSchemaCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WfsSchemaCrawler.class);
    private final ConnectionInfoWfsHttp connectionInfo;
    private final WfsRequestEncoder wfsRequestEncoder;

    public WfsSchemaCrawler(ConnectionInfoWfsHttp connectionInfo) {
        this.connectionInfo = connectionInfo;

        this.wfsRequestEncoder = new WfsRequestEncoder();
        wfsRequestEncoder.setVersion(connectionInfo.getVersion());
        wfsRequestEncoder.setGmlVersion(connectionInfo.getGmlVersion());
        wfsRequestEncoder.setUrls(ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()), WFS.METHOD.POST, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()))));
        wfsRequestEncoder.setNsStore(new XMLNamespaceNormalizer(connectionInfo.getNamespaces()));
    }

    public List<FeatureType> parseSchema(Map<String, QName> featureTypes) {

        SchemaConsumer schemaConsumer = new SchemaConsumer();

        analyzeFeatureTypes(schemaConsumer, featureTypes, new TaskProgressNoop());

        return schemaConsumer.getFeatureTypes();
    }

    private void analyzeFeatureTypes(FeatureProviderSchemaConsumer schemaConsumer, Map<String, QName> featureTypes,
                                     TaskProgress taskProgress) {
        Map<String, List<String>> featureTypesByNamespace = getSupportedFeatureTypesPerNamespace(featureTypes);

        if (!featureTypesByNamespace.isEmpty()) {
            analyzeFeatureTypesWithDescribeFeatureType(schemaConsumer, featureTypesByNamespace, taskProgress);
        }
    }

    private void analyzeFeatureTypesWithDescribeFeatureType(FeatureProviderSchemaConsumer schemaConsumer,
                                                            Map<String, List<String>> featureTypesByNamespace,
                                                            TaskProgress taskProgress) {

        HttpClient httpClient = new DefaultHttpClient();
        URI baseUri = connectionInfo.getUri();
        String requestUrl = wfsRequestEncoder.getAsUrl(new DescribeFeatureType());

        try {
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            InputStream inputStream = response.getEntity()
                                              .getContent();

            GMLSchemaParser gmlSchemaParser = new GMLSchemaParser(ImmutableList.of(schemaConsumer), baseUri);
            gmlSchemaParser.parse(inputStream, featureTypesByNamespace, taskProgress);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, List<String>> getSupportedFeatureTypesPerNamespace(Map<String, QName> featureTypes) {
        Map<String, List<String>> featureTypesPerNamespace = new HashMap<>();

        for (QName featureType : featureTypes.values()) {
            if (!featureTypesPerNamespace.containsKey(featureType.getNamespaceURI())) {
                featureTypesPerNamespace.put(featureType.getNamespaceURI(), new ArrayList<>());
            }
            featureTypesPerNamespace.get(featureType.getNamespaceURI())
                                    .add(featureType.getLocalPart());
        }

        return featureTypesPerNamespace;
    }

    //

    /**
     * TODO: implement FeatureType generation, see {@link de.ii.xtraplatform.feature.provider.wfs.GmlFeatureTypeAnalyzer}
     */
    static class SchemaConsumer implements FeatureProviderSchemaConsumer {

        private final List<FeatureType> featureTypes;

        SchemaConsumer() {
            this.featureTypes = new ArrayList<>();
        }

        public List<FeatureType> getFeatureTypes() {
            return featureTypes;
        }

        @Override
        public void analyzeFeatureType(String nsUri, String localName) {

        }

        @Override
        public void analyzeAttribute(String nsUri, String localName, String type, boolean required) {

        }

        @Override
        public void analyzeProperty(String nsUri, String localName, String type, long minOccurs, long maxOccurs,
                                    int depth, boolean isParentMultiple, boolean isComplex, boolean isObject) {

        }

        @Override
        public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
            return false;
        }

        @Override
        public void analyzeFailure(Throwable e) {

        }

        @Override
        public void analyzeSuccess() {

        }
    }

    static class TaskProgressNoop implements TaskProgress {

        @Override
        public void setStatusMessage(String statusMessage) {

        }

        @Override
        public void setCompleteness(double completeness) {

        }
    }
}
