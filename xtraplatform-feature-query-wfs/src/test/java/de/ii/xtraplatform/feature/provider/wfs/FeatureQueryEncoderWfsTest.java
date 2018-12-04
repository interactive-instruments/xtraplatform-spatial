/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfigurationOld;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.client.FilterEncoder;
import de.ii.xtraplatform.ogc.api.wfs.client.GetFeature;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLDocumentFactory;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public class FeatureQueryEncoderWfsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryEncoderWfsTest.class);

    private static final Versions VERSIONS = new Versions(WFS.VERSION._2_0_0, GML.VERSION._3_2_1);
    private static final XMLNamespaceNormalizer NAMESPACES = new XMLNamespaceNormalizer(ImmutableMap.of("au", "urn:inspire:au"));

    private static final Map<String, FeatureTypeConfigurationOld> FEATURETYPES = ImmutableMap.of("urn:inspire:au:AdministrativeUnit",
            new FeatureTypeConfigurationOld("AdministrativeUnit", "urn:inspire:au", "au:AdministrativeUnit",
                    ImmutableFeatureTypeMapping.builder()
                                               .mappings(ImmutableMap.of(
                                                       "urn:inspire:au:nested/urn:inspire:au:name", ImmutableSourcePathMapping.builder()
                                                                                                                              .mappings(ImmutableMap.of(TargetMapping.BASE_TYPE, new MockMapping("name")))
                                                                                                                              .build(),
                                                       "urn:inspire:au:pos", ImmutableSourcePathMapping.builder()
                                                                                                       .mappings(ImmutableMap.of(TargetMapping.BASE_TYPE, new MockMapping("pos")))
                                                                                                       .build()))
                                               .build()));

    private static final FeatureQuery QUERY = ImmutableFeatureQuery.builder().type("AdministrativeUnit")
                                                                   .limit(10)
                                                                   .offset(10)
                                                                   .filter("name = 'bla' AND name LIKE 'bl*' AND BBOX(pos, 50,7,51,8,'EPSG:4258')")
                                                                   .build();

    private static final String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
            "<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" xmlns:au=\"urn:inspire:au\" xmlns:fes=\"http://www.opengis.net/fes/2.0\" xmlns:gml=\"http://www.opengis.net/gml/3.2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" count=\"10\" outputFormat=\"application/gml+xml; version=3.2\" service=\"WFS\" startIndex=\"10\" version=\"2.0.0\">\n" +
            "<wfs:Query typeNames=\"au:AdministrativeUnit\">\n" +
            "<fes:Filter>\n" +
            "<fes:And>\n" +
            "<fes:BBOX>\n" +
            "<fes:ValueReference>au:pos</fes:ValueReference>\n" +
            "<gml:Envelope srsName=\"http://www.opengis.net/def/crs/EPSG/0/4258\">\n" +
            "<gml:lowerCorner>50 7</gml:lowerCorner>\n" +
            "<gml:upperCorner>51 8</gml:upperCorner>\n" +
            "</gml:Envelope>\n" +
            "</fes:BBOX>\n" +
            "<fes:And>\n" +
            "<fes:PropertyIsEqualTo>\n" +
            "<fes:ValueReference>au:nested/au:name</fes:ValueReference>\n" +
            "<fes:Literal>bla</fes:Literal>\n" +
            "</fes:PropertyIsEqualTo>\n" +
            "<fes:PropertyIsLike escapeChar=\"\\\" singleChar=\"#\" wildCard=\"*\">\n" +
            "<fes:ValueReference>au:nested/au:name</fes:ValueReference>\n" +
            "<fes:Literal>bl*</fes:Literal>\n" +
            "</fes:PropertyIsLike>\n" +
            "</fes:And>\n" +
            "</fes:And>\n" +
            "</fes:Filter>\n" +
            "</wfs:Query>\n" +
            "</wfs:GetFeature>\n";
    private static final String EXPECTED_KVP = "{SERVICE=WFS, REQUEST=GetFeature, OUTPUTFORMAT=application/gml+xml; version=3.2, VERSION=2.0.0, COUNT=10, STARTINDEX=10, TYPENAMES=au:AdministrativeUnit, FILTER=<fes:Filter xmlns:fes=\"http://www.opengis.net/fes/2.0\" xmlns:au=\"urn:inspire:au\" xmlns:gml=\"http://www.opengis.net/gml/3.2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><fes:And><fes:BBOX><fes:ValueReference>au:pos</fes:ValueReference><gml:Envelope srsName=\"http://www.opengis.net/def/crs/EPSG/0/4258\"><gml:lowerCorner>50 7</gml:lowerCorner><gml:upperCorner>51 8</gml:upperCorner></gml:Envelope></fes:BBOX><fes:And><fes:PropertyIsEqualTo><fes:ValueReference>au:nested/au:name</fes:ValueReference><fes:Literal>bla</fes:Literal></fes:PropertyIsEqualTo><fes:PropertyIsLike escapeChar=\"\\\" singleChar=\"#\" wildCard=\"*\"><fes:ValueReference>au:nested/au:name</fes:ValueReference><fes:Literal>bl*</fes:Literal></fes:PropertyIsLike></fes:And></fes:And></fes:Filter>, NAMESPACES=xmlns(au,urn:inspire:au),xmlns(xs,http://www.w3.org/2001/XMLSchema),xmlns(gml,http://www.opengis.net/gml/3.2),xmlns(fes,http://www.opengis.net/fes/2.0)}";

    @Test(groups = {"default"})
    public void testAsXml() throws ParserConfigurationException, IOException, SAXException, CQLException, TransformerException {
        //String xml = new FeatureQueryEncoderWfs(featureTypes, namespaceNormalizer).asXml(QUERY, NAMESPACES, VERSIONS);
        Date from = new Date();
        final Optional<GetFeature> getFeature = new FeatureQueryEncoderWfs(ImmutableMap.of("au", new QName(FEATURETYPES.values().iterator().next().getNamespace(), FEATURETYPES.values().iterator().next().getName())), ImmutableMap.of("au", FEATURETYPES.entrySet().iterator().next().getValue().getMappings()), NAMESPACES).encode(QUERY, );

        LOGGER.debug("TOOK {}", new Date().getTime() - from.getTime());
        Assert.assertTrue(getFeature.isPresent());
        final XMLDocumentFactory documentFactory = new XMLDocumentFactory(NAMESPACES);
        final XMLDocument document = getFeature.get()
                                               .asXml(documentFactory, VERSIONS);
        LOGGER.debug("TOOK {}", new Date().getTime() - from.getTime());
        String xml = document.toString(true);
        LOGGER.debug("TOOK {}", new Date().getTime() - from.getTime());
        LOGGER.info(xml);

        Assert.assertEquals(xml, EXPECTED_XML);
    }

    @Test(groups = {"default"})
    public void testAsKvp() throws ParserConfigurationException, IOException, SAXException, CQLException, TransformerException {
        //Map<String, String> kvp = new FeatureQueryEncoderWfs(featureTypes, namespaceNormalizer).asKvp(QUERY, NAMESPACES, VERSIONS);
        final Optional<GetFeature> getFeature = new FeatureQueryEncoderWfs(ImmutableMap.of("au", new QName(FEATURETYPES.values().iterator().next().getNamespace(), FEATURETYPES.values().iterator().next().getName())), ImmutableMap.of("au", FEATURETYPES.entrySet().iterator().next().getValue().getMappings()), NAMESPACES).encode(QUERY, );

        Assert.assertTrue(getFeature.isPresent());

        final XMLDocumentFactory documentFactory = new XMLDocumentFactory(NAMESPACES);
        Map<String, String> kvp = getFeature.get()
                                            .asKvp(documentFactory, VERSIONS);
        LOGGER.info("{}", kvp);

        Assert.assertEquals(kvp.toString(), EXPECTED_KVP);
    }

    @Test(groups = {"default"})
    public void testFilterEncoding() throws ParserConfigurationException, SAXException, IOException {
        String xml = "<fes:Filter\n" +
                "   xmlns:fes='http://www.opengis.net/fes/2.0'\n" +
                "   xmlns:gml='http://www.opengis.net/gml/3.2'\n" +
                "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
                "   xsi:schemaLocation='http://www.opengis.net/fes/2.0 http://schemas.opengis.net/filter/2.0/filterAll.xsd\n" +
                " http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd'>\n" +
                "   <fes:During>\n" +
                "  <fes:ValueReference>timeInstanceAttribute</fes:ValueReference>\n" +
                "   <gml:TimePeriod gml:id='TP1'>\n" +
                "  <gml:begin>\n" +
                "    <gml:TimeInstant gml:id='TI1'>\n" +
                "      <gml:timePosition>2005-05-17T08:00:00Z</gml:timePosition>\n" +
                "    </gml:TimeInstant>\n" +
                "  </gml:begin>\n" +
                "  <gml:end>\n" +
                "    <gml:TimeInstant gml:id='TI2'>\n" +
                "      <gml:timePosition>2005-05-23T11:00:00Z</gml:timePosition>\n" +
                "    </gml:TimeInstant>\n" +
                "  </gml:end>\n" +
                "</gml:TimePeriod>\n" +
                "   </fes:During>\n" +
                "</fes:Filter>";

        Configuration configuration = new org.geotools.filter.v2_0.FESConfiguration();
        Parser parser = new Parser(configuration);

        Filter filter = (Filter) parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        String xml2 = new FilterEncoder(WFS.VERSION._2_0_0).encodeAsString(filter);
        LOGGER.info(xml2);
        boolean stop = true;
    }

    enum bla {BLA}
    static class MockMapping implements TargetMapping<bla> {

        private final String name;

        MockMapping(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public bla getType() {
            return null;
        }

        @Override
        public Boolean getEnabled() {
            return null;
        }

        @Nullable
        @Override
        public Integer getSortPriority() {
            return null;
        }

        @Nullable
        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
            return null;
        }

        @Override
        public boolean isSpatial() {
            return false;
        }
    }
}