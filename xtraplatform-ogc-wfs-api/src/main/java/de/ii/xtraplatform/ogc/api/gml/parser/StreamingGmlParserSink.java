package de.ii.xtraplatform.ogc.api.gml.parser;

import akka.NotUsed;
import akka.stream.*;
import akka.stream.alpakka.xml.Characters;
import akka.stream.alpakka.xml.EndElement;
import akka.stream.alpakka.xml.ParseEvent;
import akka.stream.alpakka.xml.StartElement;
import akka.stream.alpakka.xml.javadsl.XmlParsing;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.util.ByteString;
import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import de.ii.xtraplatform.util.xml.XMLPathTracker;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
public class StreamingGmlParserSink {

    public static Sink<ByteString, NotUsed> parser(final QName featureType, final GmlConsumer gmlConsumer) {
        return Sink.fromGraph(new GmlSink(featureType, gmlConsumer));
    }

    public interface GmlConsumer {
        void onGmlStart() throws Exception;
        void onGmlEnd() throws Exception;
        void onGmlFeatureStart(final String namespace, final String localName, final List<String> path) throws Exception;
        void onGmlFeatureEnd() throws Exception;
        void onGmlAttribute(final String namespace, final String localName, final List<String> path, final String value) throws Exception;
        void onGmlPropertyStart(final String namespace, final String localName, final List<String> path) throws Exception;
        void onGmlPropertyText(final String text) throws Exception;
        void onGmlPropertyEnd(final String localName, final List<String> path) throws Exception;
        void onNamespaceRewrite(final QName featureType, final String namespace) throws Exception;
    }

    static class GmlSink extends GraphStage<SinkShape<ByteString>> {

        public final Inlet<ByteString> in = Inlet.create("GmlSink.in");
        private final SinkShape<ByteString> shape = SinkShape.of(in);

        private final QName featureType;
        private final GmlConsumer gmlConsumer;

        GmlSink(QName featureType, GmlConsumer gmlConsumer) {
            this.featureType = featureType;
            this.gmlConsumer = gmlConsumer;
        }

        @Override
        public SinkShape<ByteString> shape() {
            return shape;
        }

        @Override
        public GraphStageLogic createLogic(Attributes inheritedAttributes) throws XMLStreamException {
            return new GraphStageLogic(shape) {

                private AsyncXMLInputFactory feeder = new InputFactoryImpl();
                private AsyncXMLStreamReader<AsyncByteArrayFeeder> parser = feeder.createAsyncFor(new byte[0]);


                @Override
                public void preStart() throws Exception {
                    super.preStart();
                    System.out.println("preStart");
                    pull(in);
                }

                {
                    setHandler(in, new AbstractInHandler() {
                        int depth = 0;
                        int featureDepth = 0;
                        boolean inFeature = false;
                        XMLPathTracker pathTracker = new XMLPathTracker();

                        @Override
                        public void onPush() throws Exception {
                            byte[] bytes = grab(in).toArray();
                            parser.getInputFeeder().feedInput(bytes, 0, bytes.length);
                            while (parser.hasNext() && !hasBeenPulled(in)) {
                                advanceParser();
                            }
                            if (!isClosed(in) && !hasBeenPulled(in))
                                pull(in);
                        }

                        @Override
                        public void onUpstreamFinish() throws Exception {
                            parser.getInputFeeder().endOfInput();
                            while (parser.hasNext()) {
                                advanceParser();
                            }
                            completeStage();
                        }

                        private void advanceParser() throws XMLStreamException {
                            if (parser.hasNext()) {
                                try {
                                    switch (parser.next()) {
                                        case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                                            if (!isClosed(in))
                                                pull(in);
                                            else
                                                failStage(new IllegalStateException("Stream finished before event was fully parsed."));
                                            break;

                                        case XMLStreamConstants.START_DOCUMENT:

                                            break;

                                        case XMLStreamConstants.END_DOCUMENT:

                                            //completeStage();
                                            break;

                                        case XMLStreamConstants.START_ELEMENT:
                                            if (depth == 0) {
                                                gmlConsumer.onGmlStart();
                                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                                    gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                                                }
                                            } else if (matchesFeatureType(parser.getNamespaceURI(), parser.getLocalName())) {
                                                inFeature = true;
                                                featureDepth = depth;
                                                gmlConsumer.onGmlFeatureStart(parser.getNamespaceURI(), parser.getLocalName(), pathTracker.asList());
                                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                                    gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                                                }
                                            } else if (matchesFeatureType(parser.getLocalName())) {
                                                inFeature = true;
                                                featureDepth = depth;
                                                gmlConsumer.onNamespaceRewrite(featureType, parser.getNamespaceURI());
                                                gmlConsumer.onGmlFeatureStart(parser.getNamespaceURI(), parser.getLocalName(), pathTracker.asList());
                                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                                    gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                                                }
                                            } else if (inFeature) {
                                                pathTracker.track(parser.getNamespaceURI(), parser.getLocalName(), depth - featureDepth);
                                                gmlConsumer.onGmlPropertyStart(parser.getNamespaceURI(), parser.getLocalName(), pathTracker.asList());
                                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                                    gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                                                }
                                            }
                                            depth += 1;
                                            break;

                                        case XMLStreamConstants.END_ELEMENT:
                                            depth -= 1;
                                            if (depth == 0) {
                                                gmlConsumer.onGmlEnd();
                                            } else if (matchesFeatureType(parser.getLocalName())) {
                                                inFeature = false;
                                                gmlConsumer.onGmlFeatureEnd();
                                            } else if (inFeature) {
                                                gmlConsumer.onGmlPropertyEnd(parser.getLocalName(), pathTracker.asList());
                                            }
                                            pathTracker.track(depth - featureDepth);
                                            break;

                                        case XMLStreamConstants.CHARACTERS:
                                            if (inFeature) {
                                                String text = parser.getText();
                                                // TODO
                                                if (!parser.isWhiteSpace()) // && !text.matches("\\s*")
                                                    gmlConsumer.onGmlPropertyText(text);
                                            }
                                            break;

                                        // Do not support DTD, SPACE, NAMESPACE, NOTATION_DECLARATION, ENTITY_DECLARATION, PROCESSING_INSTRUCTION, COMMENT, CDATA
                                        // ATTRIBUTE is handled in START_ELEMENT implicitly

                                        default:
                                            //advanceParser();
                                    }
                                } catch (Exception e) {
                                    failStage(e);
                                }
                            } //else completeStage();
                        }

                        boolean matchesFeatureType(final String namespace, final String localName) {
                            return featureType.getLocalPart().equals(localName) && Objects.nonNull(namespace) && featureType.getNamespaceURI().equals(namespace);
                        }

                        boolean matchesFeatureType(final String localName) {
                            return featureType.getLocalPart().equals(localName);
                        }
                    });
                }
            };
        }

    }
}
