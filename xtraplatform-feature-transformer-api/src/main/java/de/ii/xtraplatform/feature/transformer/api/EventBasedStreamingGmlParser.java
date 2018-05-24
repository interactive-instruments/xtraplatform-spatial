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
import akka.stream.alpakka.xml.Characters;
import akka.stream.alpakka.xml.EndElement;
import akka.stream.alpakka.xml.ParseEvent;
import akka.stream.alpakka.xml.StartElement;
import akka.stream.alpakka.xml.javadsl.XmlParsing;
import akka.stream.javadsl.Flow;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.util.ByteString;
import de.ii.xtraplatform.util.xml.XMLPathTracker;

import javax.xml.namespace.QName;
import java.util.Optional;

/**
 * @author zahnen
 */
public class EventBasedStreamingGmlParser {

    public static Flow<ByteString, GmlEvent, NotUsed> parser(final QName featureType) {
        return XmlParsing.parser().via(new Gml(featureType));
    }

    public static abstract class GmlEventMatcher {
        public  GmlEventMatcher(final GmlEvent gmlEvent) {
            if (gmlEvent instanceof GmlStart) {
                onGmlStart((GmlStart) gmlEvent);
            } else if (gmlEvent instanceof GmlEnd) {
                onGmlEnd((GmlEnd) gmlEvent);
            } else if (gmlEvent instanceof GmlFeatureStart) {
                onGmlFeatureStart((GmlFeatureStart) gmlEvent);
            } else if (gmlEvent instanceof GmlFeatureEnd) {
                onGmlFeatureEnd((GmlFeatureEnd) gmlEvent);
            } else if (gmlEvent instanceof GmlAttribute) {
                onGmlAttribute((GmlAttribute) gmlEvent);
            } else if (gmlEvent instanceof GmlPropertyStart) {
                onGmlPropertyStart((GmlPropertyStart) gmlEvent);
            } else if (gmlEvent instanceof GmlPropertyText) {
                onGmlPropertyText((GmlPropertyText) gmlEvent);
            } else if (gmlEvent instanceof GmlPropertyEnd) {
                onGmlPropertyEnd((GmlPropertyEnd) gmlEvent);
            }
        }

        protected abstract void onGmlStart(final GmlStart gmlStart);
        protected abstract void onGmlEnd(final GmlEnd gmlEnd);
        protected abstract void onGmlFeatureStart(final GmlFeatureStart gmlFeatureStart);
        protected abstract void onGmlFeatureEnd(final GmlFeatureEnd gmlFeatureEnd);
        protected abstract void onGmlAttribute(final GmlAttribute gmlAttribute);
        protected abstract void onGmlPropertyStart(final GmlPropertyStart gmlPropertyStart);
        protected abstract void onGmlPropertyText(final GmlPropertyText gmlPropertyText);
        protected abstract void onGmlPropertyEnd(final GmlPropertyEnd gmlPropertyEnd);
    }

    public interface GmlEvent {
    }

    static class GmlNamed implements GmlEvent {
        public final String namespaceUri;
        public final String localName;

        GmlNamed(String namespaceUri, String localName) {
            this.namespaceUri = namespaceUri;
            this.localName = localName;
        }

        GmlNamed(StartElement parseEvent) {
            this.namespaceUri = parseEvent.namespace().getOrElse(() -> null);
            this.localName = parseEvent.localName();
        }

        public String getQualifiedName() {
            return Optional.ofNullable(namespaceUri).map(ns -> ns + ":" + localName).orElse(localName);
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" +
                    "namespaceUri='" + namespaceUri + '\'' +
                    ", localName='" + localName + '\'' +
                    '}';
        }
    }

    public static final class GmlStart implements GmlEvent {
    }

    public static final class GmlEnd implements GmlEvent {
    }

    public static final class GmlFeatureStart extends GmlNamed implements GmlEvent {
        public GmlFeatureStart(String namespaceUri, String localName) {
            super(namespaceUri, localName);
        }

        public GmlFeatureStart(StartElement parseEvent) {
            super(parseEvent);
        }
    }

    public static final class GmlFeatureEnd implements GmlEvent {
    }

    public static final class GmlAttribute extends GmlNamed implements GmlEvent {
        public final String path;
        public final String value;

        GmlAttribute(String namespaceUri, String localName, String path, String value) {
            super(namespaceUri, localName);
            this.path = path;
            this.value = value;
        }

        public String getQualifiedName() {
            return Optional.ofNullable(namespaceUri).map(ns -> ns + ":@" + localName).orElse("@" + localName);
        }
    }

    public static final class GmlPropertyStart extends GmlNamed implements GmlEvent {
        public final String path;

        GmlPropertyStart(String namespaceUri, String localName, String path) {
            super(namespaceUri, localName);
            this.path = path;
        }

        public GmlPropertyStart(StartElement parseEvent, String path) {
            super(parseEvent);
            this.path = path;
        }

        @Override
        public String toString() {
            return "GmlPropertyStart{" +
                    "path='" + path + '\'' +
                    '}';
        }
    }

    public static final class GmlPropertyText implements GmlEvent {
        public final String text;

        public GmlPropertyText(String text) {
            this.text = text;
        }
    }

    public static final class GmlPropertyEnd extends GmlNamed implements GmlEvent {
        public final String path;

        public GmlPropertyEnd(EndElement parseEvent, String path) {
            super(null, parseEvent.localName());
            this.path = path;
        }

        @Override
        public String toString() {
            return "GmlPropertyEnd{" +
                    "path='" + path + '\'' +
                    '}';
        }
    }

    public static final class GmlFailure implements GmlEvent {
    }

    public static final class GmlNamespaceRewrite implements GmlEvent {
        final QName featureType;
        final String newNamespaceUri;

        public GmlNamespaceRewrite(QName featureType, String newNamespaceUri) {
            this.featureType = featureType;
            this.newNamespaceUri = newNamespaceUri;
        }
    }

    static class Gml extends GraphStage<FlowShape<ParseEvent, GmlEvent>> {

        public final Inlet<ParseEvent> in = Inlet.create("Gml.in");
        public final Outlet<GmlEvent> out = Outlet.create("Gml.out");
        private final FlowShape<ParseEvent, GmlEvent> shape = FlowShape.of(in, out);

        private final QName featureType;

        Gml(QName featureType) {
            this.featureType = featureType;
        }

        @Override
        public FlowShape<ParseEvent, GmlEvent> shape() {
            return shape;
        }

        @Override
        public GraphStageLogic createLogic(Attributes inheritedAttributes) {
            return new GraphStageLogic(shape) {

                {
                    setHandler(in, new AbstractInHandler() {
                        int depth = 0;
                        int featureDepth = 0;
                        boolean inFeature = false;
                        XMLPathTracker pathTracker = new XMLPathTracker();

                        @Override
                        public void onPush() throws Exception {
                            final ParseEvent parseEvent = grab(in);
                            if (parseEvent instanceof StartElement) {
                                final StartElement startElement = (StartElement) parseEvent;
                                if (depth == 0) {
                                    push(out, new GmlStart());
                                    startElement.attributesList().foreach(attribute -> {
                                        emit(out, new GmlAttribute(attribute.namespace().getOrElse(() -> null), attribute.name(), pathTracker.toString(), attribute.value()));
                                        return null;
                                    });
                                } else if (matchesFeatureType(startElement)) {
                                    inFeature = true;
                                    featureDepth = depth;
                                    push(out, new GmlFeatureStart(startElement));
                                    startElement.attributesList().foreach(attribute -> {
                                        emit(out, new GmlAttribute(attribute.namespace().getOrElse(() -> null), attribute.name(), pathTracker.toString(), attribute.value()));
                                        return null;
                                    });
                                } else if (matchesFeatureTypeLocal(startElement)) {
                                    inFeature = true;
                                    featureDepth = depth;
                                    push(out, new GmlNamespaceRewrite(featureType, startElement.namespace().getOrElse(() -> null)));
                                    emit(out, new GmlFeatureStart(startElement));
                                    startElement.attributesList().foreach(attribute -> {
                                        emit(out, new GmlAttribute(attribute.namespace().getOrElse(() -> null), attribute.name(), pathTracker.toString(), attribute.value()));
                                        return null;
                                    });
                                } else if (inFeature) {
                                    pathTracker.track(startElement.namespace().getOrElse(() -> null), startElement.localName(), depth - featureDepth);
                                    push(out, new GmlPropertyStart(startElement, pathTracker.toString()));
                                    startElement.attributesList().foreach(attribute -> {
                                        emit(out, new GmlAttribute(attribute.namespace().getOrElse(() -> null), attribute.name(), pathTracker.toString(), attribute.value()));
                                        return null;
                                    });
                                } else {
                                    pull(in);
                                }
                                depth += 1;
                            } else if (parseEvent instanceof EndElement) {
                                depth -= 1;
                                if (depth == 0) {
                                    push(out, new GmlEnd());
                                } else if (matchesFeatureType((EndElement) parseEvent)) {
                                    inFeature = false;
                                    push(out, new GmlFeatureEnd());
                                } else if (inFeature) {
                                    push(out, new GmlPropertyEnd(((EndElement) parseEvent), pathTracker.toString()));
                                } else {
                                    pull(in);
                                }
                                pathTracker.track(depth - featureDepth);
                            } else if (parseEvent instanceof Characters) {
                                if (inFeature && !((Characters)parseEvent).text().matches("\\s*")) {
                                    push(out, new GmlPropertyText(((Characters)parseEvent).text()));
                                } else {
                                    pull(in);
                                }
                            } else {
                                pull(in);
                            }
                        }

                        boolean matchesFeatureType(final StartElement parseEvent) {
                            return featureType.getLocalPart().equals(parseEvent.localName()) && parseEvent.namespace().nonEmpty() && featureType.getNamespaceURI().equals(parseEvent.namespace().get());
                        }

                        boolean matchesFeatureTypeLocal(final StartElement parseEvent) {
                            return featureType.getLocalPart().equals(parseEvent.localName());
                        }

                        boolean matchesFeatureType(final EndElement parseEvent) {
                            return featureType.getLocalPart().equals(parseEvent.localName());
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

    }
}
