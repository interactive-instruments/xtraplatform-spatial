/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import akka.NotUsed;
import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.javadsl.Flow;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.util.ByteString;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.EventBasedStreamingGmlParser.GmlEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
public class EventBasedStreamingFeatureTransformer {

    public static Flow<ByteString, TransformEvent, NotUsed> parser(final QName featureType, final FeatureTypeMapping featureTypeMapping, final String outputFormat) {
        return EventBasedStreamingGmlParser.parser(featureType).via(new EventBasedStreamingFeatureTransformer.Gml(featureTypeMapping, outputFormat));
    }

    public static abstract class TransformEventHandler {
        public  TransformEventHandler(final TransformEvent transformEvent) throws IOException {
            if (transformEvent instanceof TransformStart) {
                onStart((TransformStart) transformEvent);
            } else if (transformEvent instanceof TransformEnd) {
                onEnd((TransformEnd) transformEvent);
            } else if (transformEvent instanceof TransformFeatureStart) {
                onFeatureStart((TransformFeatureStart) transformEvent);
            } else if (transformEvent instanceof TransformFeatureEnd) {
                onFeatureEnd((TransformFeatureEnd) transformEvent);
            } else if (transformEvent instanceof TransformAttribute) {
                onAttribute((TransformAttribute) transformEvent);
            } else if (transformEvent instanceof TransformProperty) {
                onPropertyStart((TransformProperty) transformEvent);
            } else if (transformEvent instanceof TransformPropertyText) {
                onPropertyText((TransformPropertyText) transformEvent);
            } else if (transformEvent instanceof TransformPropertyEnd) {
                onPropertyEnd((TransformPropertyEnd) transformEvent);
            } else if (transformEvent instanceof TransformGeometry) {
                onGeometryStart((TransformGeometry) transformEvent);
            } else if (transformEvent instanceof TransformGeometryNestedStart) {
                onGeometryNestedStart((TransformGeometryNestedStart) transformEvent);
            } else if (transformEvent instanceof TransformGeometryCoordinates) {
                onGeometryCoordinates((TransformGeometryCoordinates) transformEvent);
            } else if (transformEvent instanceof TransformGeometryNestedEnd) {
                onGeometryNestedEnd((TransformGeometryNestedEnd) transformEvent);
            } else if (transformEvent instanceof TransformGeometryEnd) {
                onGeometryEnd((TransformGeometryEnd) transformEvent);
            }
        }

        protected abstract void onStart(final TransformStart transformStart) throws IOException;
        protected abstract void onEnd(final TransformEnd transformEnd) throws IOException;
        protected abstract void onFeatureStart(final TransformFeatureStart transformFeatureStart) throws IOException;
        protected abstract void onFeatureEnd(final TransformFeatureEnd transformFeatureEnd) throws IOException;
        protected abstract void onAttribute(final TransformAttribute transformAttribute) throws IOException;
        protected abstract void onPropertyStart(final TransformProperty transformProperty) throws IOException;
        protected abstract void onPropertyText(final TransformPropertyText transformPropertyText) throws IOException;
        protected abstract void onPropertyEnd(final TransformPropertyEnd transformPropertyEnd) throws IOException;
        protected abstract void onGeometryStart(final TransformGeometry transformGeometry) throws IOException;
        protected abstract void onGeometryNestedStart(final TransformGeometryNestedStart transformGeometryNestedStart) throws IOException;
        protected abstract void onGeometryCoordinates(final TransformGeometryCoordinates transformGeometryCoordinates) throws IOException;
        protected abstract void onGeometryNestedEnd(final TransformGeometryNestedEnd transformGeometryNestedEnd) throws IOException;
        protected abstract void onGeometryEnd(final TransformGeometryEnd transformGeometryEnd) throws IOException;
    }

    public interface TransformEvent {
    }

    static class TransformWithMapping implements TransformEvent {
        public final Optional<TargetMapping> mapping;
        public final Optional<String> value;

        public TransformWithMapping() {
            this(null, null);
        }

        public TransformWithMapping(final TargetMapping mapping) {
            this(mapping, null);
        }

        public TransformWithMapping(final TargetMapping mapping, final String value) {
            this.mapping = Optional.ofNullable(mapping);
            this.value = Optional.ofNullable(value);
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" +
                    "mapping=" + mapping +
                    ", value=" + value +
                    '}';
        }
    }

    public static final class TransformStart implements TransformEvent {
        @Override
        public String toString() {
            return "TransformStart{}";
        }
    }

    public static final class TransformEnd implements TransformEvent {
        @Override
        public String toString() {
            return "TransformEnd{}";
        }
    }

    public static final class TransformFeatureStart extends TransformWithMapping {
        public TransformFeatureStart() {
            super();
        }

        public TransformFeatureStart(TargetMapping mapping) {
            super(mapping);
        }
    }

    public static final class TransformFeatureEnd implements TransformEvent {
        @Override
        public String toString() {
            return "TransformFeatureEnd{}";
        }
    }

    public static final class TransformAttribute extends TransformWithMapping {
        public TransformAttribute(TargetMapping mapping, String value) {
            super(mapping, value);
        }
    }

    public static final class TransformProperty extends TransformWithMapping {

        public TransformProperty(TargetMapping mapping) {
            super(mapping);
        }
    }

    public static final class TransformGeometry extends TransformWithMapping {
        public GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE type;
        public Integer dimension;

        public TransformGeometry(TargetMapping mapping) {
            super(mapping);
        }

        @Override
        public String toString() {
            return "TransformGeometry{" +
                    "type=" + type +
                    ", dimension=" + dimension +
                    '}';
        }
    }

    public static final class TransformGeometryNestedStart implements TransformEvent {
        @Override
        public String toString() {
            return "TransformGeometryNestedStart{}";
        }
    }

    public static final class TransformGeometryNestedEnd implements TransformEvent {
        @Override
        public String toString() {
            return "TransformGeometryNestedEnd{}";
        }
    }

    public static final class TransformGeometryCoordinates implements TransformEvent {
        public final String text;

        public TransformGeometryCoordinates(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return "TransformGeometryCoordinates{" +
                    "text='" + text + '\'' +
                    '}';
        }
    }

    public static final class TransformGeometryEnd implements TransformEvent {
        @Override
        public String toString() {
            return "TransformGeometryEnd{}";
        }
    }

    public static final class TransformPropertyText implements TransformEvent {
        public final String text;

        public TransformPropertyText(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return "TransformPropertyText{" +
                    "text='" + text + '\'' +
                    '}';
        }
    }

    public static final class TransformPropertyEnd implements TransformEvent {
        @Override
        public String toString() {
            return "TransformPropertyEnd{}";
        }
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


    static class Gml extends GraphStage<FlowShape<GmlEvent, TransformEvent>> {
        private static final Logger LOGGER = LoggerFactory.getLogger(Gml.class);

        public final Inlet<GmlEvent> in = Inlet.create("Gml.in");
        public final Outlet<TransformEvent> out = Outlet.create("Gml.out");
        private final FlowShape<GmlEvent, TransformEvent> shape = FlowShape.of(in, out);

        private final FeatureTypeMapping featureTypeMapping;
        private final String outputFormat;
        private boolean inProperty;
        private String inGeometry = "";
        private boolean geometrySent;
        private boolean inCoordinates;
        private TransformGeometry transformGeometry;

        Gml(FeatureTypeMapping featureTypeMapping, String outputFormat) {
            this.featureTypeMapping = featureTypeMapping;
            this.outputFormat = outputFormat;
        }

        @Override
        public FlowShape<GmlEvent, TransformEvent> shape() {
            return shape;
        }



        @Override
        public GraphStageLogic createLogic(Attributes inheritedAttributes) {
            return new GraphStageLogic(shape) {

                // TODO: state

                {
                    setHandler(in, new AbstractInHandler() {

                        @Override
                        public void onPush() throws Exception {
                            final GmlEvent gmlEvent = grab(in);

                            final List<TransformEvent> transformEvents = transform(gmlEvent);

                            if (!transformEvents.isEmpty()) {
                                emitMultiple(out, transformEvents.iterator());
                            } else {
                                pull(in);
                            }

                        }
                    });

                    setHandler(out, new AbstractOutHandler() {
                        @Override
                        public void onPull() throws Exception {
                            pull(in);
                        }
                    });
                }
            };
        }

        private List<TransformEvent> transform(final GmlEvent gmlEvent) {
            final List<TransformEvent> transformEvents = new ArrayList<>();

            //LOGGER.getLogger().debug(gmlEvent.toString());

            new EventBasedStreamingGmlParser.GmlEventMatcher(gmlEvent) {

                @Override
                protected void onGmlStart(EventBasedStreamingGmlParser.GmlStart gmlStart) {
                    transformEvents.add(new TransformStart());
                }

                @Override
                protected void onGmlEnd(EventBasedStreamingGmlParser.GmlEnd gmlEnd) {
                    transformEvents.add(new TransformEnd());
                }

                @Override
                protected void onGmlFeatureStart(EventBasedStreamingGmlParser.GmlFeatureStart gmlFeatureStart) {
                    final TransformFeatureStart transformFeatureStart = featureTypeMapping.findMappings(gmlFeatureStart.getQualifiedName(), outputFormat)
                            .map(mapping -> new TransformFeatureStart(mapping))
                            .orElse(new TransformFeatureStart());
                    transformEvents.add(transformFeatureStart);
                }

                @Override
                protected void onGmlFeatureEnd(EventBasedStreamingGmlParser.GmlFeatureEnd gmlFeatureEnd) {
                    transformEvents.add(new TransformFeatureEnd());
                }

                @Override
                protected void onGmlAttribute(EventBasedStreamingGmlParser.GmlAttribute gmlAttribute) {
                    if (transformGeometry != null) {
                        if (transformGeometry.dimension == null && gmlAttribute.localName.equals("srsDimension")) {
                            transformGeometry.dimension = Integer.valueOf(gmlAttribute.value);
                        }
                        return;
                    }

                    featureTypeMapping.findMappings(gmlAttribute.getQualifiedName(), outputFormat)
                            .map(mapping -> new TransformAttribute(mapping, gmlAttribute.value))
                            .ifPresent(transformEvents::add);
                }

                // TODO: Geometry
                @Override
                protected void onGmlPropertyStart(EventBasedStreamingGmlParser.GmlPropertyStart gmlPropertyStart) {

                    if (!inProperty) {
                        featureTypeMapping.findMappings(gmlPropertyStart.path, outputFormat)
                                .filter(targetMapping -> !targetMapping.isSpatial())
                                .map(TransformProperty::new)
                                .ifPresent(transformEvents::add);
                    } else if (transformGeometry != null) {
                        onGeometryPart(gmlPropertyStart);
                    }

                    inProperty = inProperty || !transformEvents.isEmpty();

                    if (!inProperty && transformGeometry == null) {
                        featureTypeMapping.findMappings(gmlPropertyStart.path, outputFormat)
                                .filter(TargetMapping::isSpatial)
                                .map(TransformGeometry::new)
                                .ifPresent(transformGeometry1 -> transformGeometry = transformGeometry1);

                        if (transformGeometry != null) {
                            inProperty = true;
                            inGeometry = gmlPropertyStart.path;
                        }
                    }
                }

                @Override
                protected void onGmlPropertyText(EventBasedStreamingGmlParser.GmlPropertyText gmlPropertyText) {
                    if (inProperty) {
                        if (inCoordinates) {
                            transformEvents.add(new TransformGeometryCoordinates(gmlPropertyText.text));
                        } else {
                            transformEvents.add(new TransformPropertyText(gmlPropertyText.text));
                        }
                    }
                }

                @Override
                protected void onGmlPropertyEnd(EventBasedStreamingGmlParser.GmlPropertyEnd gmlPropertyEnd) {
                    if (transformGeometry != null) {
                        if (inGeometry.equals(gmlPropertyEnd.path)) {
                            inGeometry = "";
                            transformGeometry = null;
                            geometrySent = false;
                            transformEvents.add(new TransformGeometryEnd());
                            inProperty = false;
                        } else {
                            onGeometryPartEnd(gmlPropertyEnd);
                        }
                    } else if (inProperty) {
                        transformEvents.add(new TransformPropertyEnd());
                        inProperty = false;
                    }
                }

                private void onGeometryPart(EventBasedStreamingGmlParser.GmlPropertyStart gmlPropertyStart) {
                    if (transformGeometry == null) return;

                    if (transformGeometry.type == null) {
                        final GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE geometryType = GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE.fromString(gmlPropertyStart.localName);
                        if (geometryType.isValid()) {
                            transformGeometry.type = geometryType;
                        }
                    }

                    if (transformGeometry.type != null) {
                        if (GEOMETRY_PARTS.contains(gmlPropertyStart.localName)) {
                            if (!geometrySent) {
                                transformEvents.add(transformGeometry);
                                geometrySent = true;
                            }
                            transformEvents.add(new TransformGeometryNestedStart());
                        } else if (GEOMETRY_COORDINATES.contains(gmlPropertyStart.localName)) {
                            if (!geometrySent) {
                                transformEvents.add(transformGeometry);
                                geometrySent = true;
                            }
                            inCoordinates = true;
                        }
                    }
                }

                private void onGeometryPartEnd(EventBasedStreamingGmlParser.GmlPropertyEnd gmlPropertyEnd) {
                    if (transformGeometry == null) return;

                    if (GEOMETRY_PARTS.contains(gmlPropertyEnd.localName)) {
                        transformEvents.add(new TransformGeometryNestedEnd());
                    } else if (GEOMETRY_COORDINATES.contains(gmlPropertyEnd.localName)) {
                        inCoordinates = false;
                    }
                }
            };

            return transformEvents;
        }

    }
}
