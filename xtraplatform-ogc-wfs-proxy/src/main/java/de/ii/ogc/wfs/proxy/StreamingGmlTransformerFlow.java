package de.ii.ogc.wfs.proxy;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import de.ii.ogc.wfs.proxy.StreamingGmlTransformerSink.GmlTransformer;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.xtraplatform.ogc.api.gml.parser.StreamingGmlParserFlow;
import de.ii.xtraplatform.ogc.api.gml.parser.StreamingGmlParserFlow.GmlConsumerFlow;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static de.ii.ogc.wfs.proxy.LambdaWithException.consumerMayThrow;
import static de.ii.ogc.wfs.proxy.LambdaWithException.mayThrow;

/**
 * @author zahnen
 */
public class StreamingGmlTransformerFlow {

    public static Flow<ByteString, ByteString, NotUsed> transformer(final QName featureType, final WfsProxyFeatureTypeMapping featureTypeMapping, final GmlTransformerFlow gmlTransformer) {
        return StreamingGmlParserFlow.parser(featureType, new GmlConsumerToTransformer(featureTypeMapping, gmlTransformer));
    }

    public interface GmlTransformerFlow extends GmlTransformer {
        void initialize(Consumer<ByteString> push, Consumer<Throwable> failStage);
    }

    static final List<String> GEOMETRY_PARTS = new ImmutableList.Builder<String>()
            .add("exterior")
            .add("interior")
            .add("outerBoundaryIs")
            .add("innerBoundaryIs")
            .add("LineString")
            .build();

    static final List<String> GEOMETRY_COORDINATES = new ImmutableList.Builder<String>()
            .add("posList")
            .add("pos")
            .add("coordinates")
            .build();


    static class GmlConsumerToTransformer implements GmlConsumerFlow {

        private final WfsProxyFeatureTypeMapping featureTypeMapping;
        private final GmlTransformerFlow gmlTransformer;
        private final String outputFormat;
        private boolean inProperty;
        private List<String> inGeometry;
        private boolean geometrySent;
        private boolean inCoordinates;
        private TargetMapping transformGeometry;
        private GML_GEOMETRY_TYPE transformGeometryType;
        private Integer transformGeometryDimension;
        private final Joiner joiner;
        private final StringBuilder stringBuilder;

        GmlConsumerToTransformer(WfsProxyFeatureTypeMapping featureTypeMapping, final GmlTransformerFlow gmlTransformer) {
            this.featureTypeMapping = featureTypeMapping;
            this.gmlTransformer = gmlTransformer;
            this.outputFormat = gmlTransformer.getTargetFormat();
            this.joiner = Joiner.on('/');
            this.stringBuilder = new StringBuilder();
        }

        @Override
        public void initialize(Consumer<ByteString> push, Consumer<Throwable> failStage) {
            gmlTransformer.initialize(push, failStage);
        }

        @Override
        public void onGmlStart() throws Exception {
            gmlTransformer.onStart();
        }

        @Override
        public void onGmlEnd() throws Exception {
            gmlTransformer.onEnd();
        }

        @Override
        public void onGmlFeatureStart(String namespace, String localName, List<String> path) throws Exception {
            final TargetMapping mapping = featureTypeMapping.findMappings(getQualifiedName(namespace, localName), outputFormat).stream()
                    .filter(TargetMapping::isEnabled)
                    .findFirst()
                    .orElse(null);
            gmlTransformer.onFeatureStart(mapping);
        }

        @Override
        public void onGmlFeatureEnd() throws Exception {
            gmlTransformer.onFeatureEnd();
        }

        @Override
        public void onGmlAttribute(String namespace, String localName, List<String> path, String value) {
            if (transformGeometry != null) {
                if (transformGeometryDimension == null && localName.equals("srsDimension")) {
                    try {
                        transformGeometryDimension = Integer.valueOf(value);
                    } catch (NumberFormatException e) {
                        // ignore
                    }

                }
                return;
            }

            featureTypeMapping.findMappings(getQualifiedName(namespace, "@" + localName), outputFormat).stream()
                    .filter(TargetMapping::isEnabled)
                    .forEach(consumerMayThrow(mapping -> gmlTransformer.onAttribute(mapping, value)));
        }

        @Override
        public void onGmlPropertyStart(String namespace, String localName, List<String> path) throws Exception {
            boolean mapped = false;
            if (!inProperty) {
                mapped = featureTypeMapping.findMappings2(path, outputFormat).stream()
                        .filter(TargetMapping::isEnabled)
                        .filter(targetMapping -> !targetMapping.isGeometry())
                        .map(mayThrow(mapping -> {gmlTransformer.onPropertyStart(mapping); return true;}))
                        .reduce(false, (a, b) -> a || b);
            } else if (transformGeometry != null) {
                onGeometryPart(localName);
            }

            inProperty = inProperty || mapped;

            if (!inProperty && transformGeometry == null) {
                featureTypeMapping.findMappings2(path, outputFormat).stream()
                        .filter(TargetMapping::isEnabled)
                        .filter(TargetMapping::isGeometry)
                        .findFirst()
                        .ifPresent(mapping -> transformGeometry = mapping);

                if (transformGeometry != null) {
                    inProperty = true;
                    // has to be copy, as path is a reference to the list in pathTracker
                    inGeometry = ImmutableList.copyOf(path);
                }
            }
        }

        private String join(List<String> elements) {
            stringBuilder.setLength(0);
            return joiner.appendTo(stringBuilder, elements).toString();
        }

        @Override
        public void onGmlPropertyText(String text) throws Exception {
            if (inProperty) {
                if (inCoordinates) {
                    gmlTransformer.onGeometryCoordinates(text);
                } else {
                    gmlTransformer.onPropertyText(text);
                }
            }
        }

        @Override
        public void onGmlPropertyEnd(String localName, List<String> path) throws Exception {
            if (transformGeometry != null) {
                if (inGeometry != null && inGeometry.equals(path)) {
                    inGeometry = null;
                    transformGeometry = null;
                    transformGeometryType = null;
                    transformGeometryDimension = null;
                    geometrySent = false;
                    gmlTransformer.onGeometryEnd();
                    inProperty = false;
                } else {
                    onGeometryPartEnd(localName);
                }
            } else if (inProperty) {
                gmlTransformer.onPropertyEnd();
                inProperty = false;
            }
        }

        @Override
        public void onNamespaceRewrite(QName featureType, String namespace) {

        }

        private void onGeometryPart(final String localName) throws Exception {
            if (transformGeometry == null) return;

            if (transformGeometryType == null) {
                final GML_GEOMETRY_TYPE geometryType = GML_GEOMETRY_TYPE.fromString(localName);
                if (geometryType.isValid()) {
                    transformGeometryType = geometryType;
                }
            }

            if (transformGeometryType != null) {
                if (GEOMETRY_PARTS.contains(localName)) {
                    if (!geometrySent) {
                        gmlTransformer.onGeometryStart(transformGeometry, transformGeometryType, transformGeometryDimension);
                        geometrySent = true;
                    }
                    gmlTransformer.onGeometryNestedStart();
                } else if (GEOMETRY_COORDINATES.contains(localName)) {
                    if (!geometrySent) {
                        gmlTransformer.onGeometryStart(transformGeometry, transformGeometryType, transformGeometryDimension);
                        geometrySent = true;
                    }
                    inCoordinates = true;
                }
            }
        }

        private void onGeometryPartEnd(final String localName) {
            if (transformGeometry == null) return;

            if (GEOMETRY_PARTS.contains(localName)) {
                try {
                    gmlTransformer.onGeometryNestedEnd();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (GEOMETRY_COORDINATES.contains(localName)) {
                inCoordinates = false;
            }
        }

        private String getQualifiedName(String namespaceUri, String localName) {
            return Optional.ofNullable(namespaceUri).map(ns -> ns + ":" + localName).orElse(localName);
        }
    }
}
