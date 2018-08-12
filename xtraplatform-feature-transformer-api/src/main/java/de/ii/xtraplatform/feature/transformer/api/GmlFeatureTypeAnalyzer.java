/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zahnen
 */
public class GmlFeatureTypeAnalyzer {

    public enum GML_TYPE {
        ID("ID"),
        STRING("string"),
        DATE_TIME("dateTime"),
        DATE("date"),
        GEOMETRY("geometry"),
        DECIMAL("decimal"),
        DOUBLE("double"),
        FLOAT("float"),
        INT("int"),
        INTEGER("integer"),
        LONG("long"),
        SHORT("short"),
        BOOLEAN("boolean"),
        URI("anyURI"),
        NONE("");

        private String stringRepresentation;

        GML_TYPE(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GML_TYPE fromString(String type) {
            for (GML_TYPE v : GML_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return v;
                }
            }

            return NONE;
        }

        public static boolean contains(String type) {
            for (GML_TYPE v : GML_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }

    public enum GML_GEOMETRY_TYPE implements SimpleFeatureGeometryFrom {

        GEOMETRY("GeometryPropertyType"),
        ABSTRACT_GEOMETRY("GeometricPrimitivePropertyType"),
        POINT("PointPropertyType", "Point"),
        MULTI_POINT("MultiPointPropertyType", "MultiPoint"),
        LINE_STRING("LineStringPropertyType", "LineString"),
        MULTI_LINESTRING("MultiLineStringPropertyType", "MultiLineString"),
        CURVE("CurvePropertyType", "Curve"),
        MULTI_CURVE("MultiCurvePropertyType", "MultiCurve"),
        SURFACE("SurfacePropertyType", "Surface"),
        MULTI_SURFACE("MultiSurfacePropertyType", "MultiSurface"),
        POLYGON("PolygonPropertyType", "Polygon"),
        MULTI_POLYGON("MultiPolygonPropertyType", "MultiPolygon"),
        SOLID("SolidPropertyType"),
        NONE("");

        private String stringRepresentation;
        private String elementStringRepresentation;

        GML_GEOMETRY_TYPE(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }
        GML_GEOMETRY_TYPE(String stringRepresentation, String elementStringRepresentation) {
            this(stringRepresentation);
            this.elementStringRepresentation = elementStringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GML_GEOMETRY_TYPE fromString(String type) {
            for (GML_GEOMETRY_TYPE v : GML_GEOMETRY_TYPE.values()) {
                if (v.toString().equals(type) || (v.elementStringRepresentation != null && v.elementStringRepresentation.equals(type))) {
                    return v;
                }
            }
            return NONE;
        }

        public static boolean contains(String type) {
            for (GML_GEOMETRY_TYPE v : GML_GEOMETRY_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public SimpleFeatureGeometry toSimpleFeatureGeometry() {
            SimpleFeatureGeometry simpleFeatureGeometry = SimpleFeatureGeometry.NONE;

            switch (this) {

                case GEOMETRY:
                    break;
                case ABSTRACT_GEOMETRY:
                    break;
                case POINT:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POINT;
                    break;
                case MULTI_POINT:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POINT;
                    break;
                case LINE_STRING:
                    simpleFeatureGeometry = SimpleFeatureGeometry.LINE_STRING;
                    break;
                case MULTI_LINESTRING:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_LINE_STRING;
                    break;
                case CURVE:
                    simpleFeatureGeometry = SimpleFeatureGeometry.LINE_STRING;
                    break;
                case MULTI_CURVE:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_LINE_STRING;
                    break;
                case SURFACE:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POLYGON;
                    break;
                case MULTI_SURFACE:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POLYGON;
                    break;
                case POLYGON:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POLYGON;
                    break;
                case MULTI_POLYGON:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POLYGON;
                    break;
                case SOLID:
                    break;
            }

            return simpleFeatureGeometry;
        }

        @Override
        public boolean isValid() {
            return this != NONE;
        }
    }

    public static final String GML_NS_URI = GML.getNS(GML.VERSION._2_1_1);
    private static final Logger LOGGER = LoggerFactory.getLogger(GmlFeatureTypeAnalyzer.class);

    private FeatureTransformerService proxyService;
    // TODO: could it be more than one?
    private FeatureTypeConfigurationOld currentFeatureType;
    private ImmutableFeatureTypeMapping.Builder currentFeatureTypeMapping;
    private XMLPathTracker currentPath;
    //private XMLPathTracker currentPathWithoutObjects;
    private Set<String> mappedPaths;
    //private boolean geometryMapped;
    private int geometryCounter;
    private final List<TargetMappingProviderFromGml> mappingProviders;

    public GmlFeatureTypeAnalyzer(FeatureTransformerService proxyService, List<TargetMappingProviderFromGml> mappingProviders) {
        this.proxyService = proxyService;
        this.currentPath = new XMLPathTracker();
        //this.currentPathWithoutObjects = new XMLPathTracker();
        this.mappedPaths = new HashSet<>();
        //this.geometryMapped = false;
        this.geometryCounter = -1;
        this.mappingProviders = mappingProviders;
    }

    protected boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        boolean rewritten = false;

        String prefix = proxyService.getWfsAdapter().getNsStore().getNamespacePrefix(oldNamespace);
        if (prefix != null) {
            proxyService.getWfsAdapter().getNsStore().addNamespace(prefix, newNamespace, true);

            String fullName = oldNamespace + ":" + featureTypeName;
            FeatureTypeConfigurationOld wfsProxyFeatureType = proxyService.getFeatureTypes().get(fullName);
            if (wfsProxyFeatureType != null) {
                wfsProxyFeatureType.setNamespace(newNamespace);
                proxyService.getFeatureTypes().remove(fullName);
                fullName = newNamespace + ":" + featureTypeName;
                proxyService.getFeatureTypes().put(fullName, wfsProxyFeatureType);
                rewritten = true;
            }
        }

        return rewritten;
    }

    protected void analyzeFeatureType(String nsUri, String localName) {

        // finish former feature type
        // does the service exist yet or are we just building the data here?
        //featureProviderDataBuilder.putMappings(currentFeatureType.getName(), currentFeatureTypeMapping.build());


        if (nsUri.isEmpty()) {
            //LOGGER.error(FrameworkMessages.NSURI_IS_EMPTY);
        }

        String fullName = nsUri + ":" + localName;
        currentFeatureType = proxyService.getFeatureTypes().get(fullName);
        currentFeatureTypeMapping = ImmutableFeatureTypeMapping.builder();

        mappedPaths.clear();
        currentPath.clear();
        //currentPathWithoutObjects.clear();

        //geometryMapped = false;
        this.geometryCounter = -1;

        proxyService.getWfsAdapter().addNamespace(nsUri);


        for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

            TargetMapping targetMapping = mappingProvider.getTargetMappingForFeatureType(nsUri, localName);

            if (targetMapping != null) {
                currentFeatureTypeMapping.putMappings(fullName, ImmutableSourcePathMapping.builder().putMappings(mappingProvider.getTargetType(), targetMapping).build());
                //currentFeatureType.getMappings().addMapping(fullName, mappingProvider.getTargetType(), targetMapping);
            }
        }
    }

    protected void analyzeAttribute(String nsUri, String localName, String type) {

        // only first level gml:ids
        if (!currentPath.isEmpty()) {
            return;
        }

        proxyService.getWfsAdapter().addNamespace(nsUri);

        currentPath.track(nsUri, "@" + localName);

        // only gml:id of the feature for now
        // TODO: version
        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {
            String path = currentPath.toString();

            if (currentFeatureType != null && !isPathMapped(path)) {

                for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

                    TargetMapping targetMapping = mappingProvider.getTargetMappingForAttribute(currentPath.toFieldNameGml(), nsUri, localName, GML_TYPE.ID);

                    if (targetMapping != null) {
                        mappedPaths.add(path);

                        currentFeatureTypeMapping.putMappings(path, ImmutableSourcePathMapping.builder().putMappings(mappingProvider.getTargetType(), targetMapping).build());
                        //currentFeatureType.getMappings().addMapping(path, mappingProvider.getTargetType(), targetMapping);
                    }
                }
            }
        }
    }

    protected void analyzeProperty(String nsUri, String localName, String type, int depth, boolean isObject) {

        proxyService.getWfsAdapter().addNamespace(nsUri);

        currentPath.track(nsUri, localName, depth);

        /*if (!isObject) {
            currentPathWithoutObjects.track(nsUri, localName, depth);
        } else {
            currentPathWithoutObjects.track(null, null, depth);
        }*/

        String path = currentPath.toString();

        // TODO: version
        // skip first level gml properties
        if (path.startsWith(GML_NS_URI)) {
            return;
        }

        if (currentFeatureType != null && !isPathMapped(path)) {

            for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

                TargetMapping targetMapping = null;

                GML_TYPE dataType = GML_TYPE.fromString(type);

                if (dataType.isValid()) {

                    targetMapping = mappingProvider.getTargetMappingForProperty(currentPath.toFieldNameGml(), nsUri, localName, dataType);

                } else {

                    GML_GEOMETRY_TYPE geoType = GML_GEOMETRY_TYPE.fromString(type);

                    if (geoType.isValid()) {

                        targetMapping = mappingProvider.getTargetMappingForGeometry(currentPath.toFieldNameGml(), nsUri, localName, geoType);
                    } else {
                        LOGGER.debug("NOT MAPPED {} {}", currentPath.toFieldNameGml(), type);
                    }
                }

                if (targetMapping != null) {
                    mappedPaths.add(path);

                    currentFeatureTypeMapping.putMappings(path, ImmutableSourcePathMapping.builder().putMappings(mappingProvider.getTargetType(), targetMapping).build());
                    //currentFeatureType.getMappings().addMapping(path, mappingProvider.getTargetType(), targetMapping);
                }
            }
        }
    }

    // this prevents that we descend further on a mapped path
    private boolean isPathMapped(String path) {
        for (String mappedPath : mappedPaths) {
            if (path.startsWith(mappedPath + "/")) {
                return true;
            }
        }
        return false;
    }
}
