package de.ii.xtraplatform.ogc.api.gml.parser;

import akka.stream.Shape;
import akka.stream.stage.GraphStageLogic;
import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import de.ii.xtraplatform.ogc.api.gml.parser.StreamingGmlParserFlow.GmlConsumerFlow;
import de.ii.xtraplatform.util.xml.XMLPathTracker;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author zahnen
 */
public abstract class AbstractStreamingGmlGraphStage extends GraphStageLogic {

    private AsyncXMLInputFactory feeder = new InputFactoryImpl();
    protected AsyncXMLStreamReader<AsyncByteArrayFeeder> parser = feeder.createAsyncFor(new byte[0]);

    int depth = 0;
    int featureDepth = 0;
    boolean inFeature = false;
    XMLPathTracker pathTracker = new XMLPathTracker();

    private final QName featureType;
    private final GmlConsumerFlow gmlConsumerFlow;

    protected AbstractStreamingGmlGraphStage(Shape shape, QName featureType, GmlConsumerFlow gmlConsumerFlow) throws XMLStreamException {
        super(shape);
        this.featureType = featureType;
        this.gmlConsumerFlow = gmlConsumerFlow;
    }

    protected boolean advanceParser(Supplier<Boolean> pull) throws XMLStreamException {
        if (!parser.hasNext()) return true;

        boolean feedMeMore = false;

        try {
            switch (parser.next()) {
                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    //if (!pull.get())
                    //    failStage(new IllegalStateException("Stream finished before event was fully parsed."));
                    feedMeMore = true;
                    break;

                case XMLStreamConstants.START_DOCUMENT:

                    break;

                case XMLStreamConstants.END_DOCUMENT:

                    //completeStage();
                    break;

                case XMLStreamConstants.START_ELEMENT:
                    if (depth == 0) {
                        gmlConsumerFlow.onGmlStart();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumerFlow.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    } else if (matchesFeatureType(parser.getNamespaceURI(), parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;
                        gmlConsumerFlow.onGmlFeatureStart(parser.getNamespaceURI(), parser.getLocalName(), pathTracker.asList());
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumerFlow.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    } else if (matchesFeatureType(parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;
                        gmlConsumerFlow.onNamespaceRewrite(featureType, parser.getNamespaceURI());
                        gmlConsumerFlow.onGmlFeatureStart(parser.getNamespaceURI(), parser.getLocalName(), pathTracker.asList());
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumerFlow.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    } else if (inFeature) {
                        pathTracker.track(parser.getNamespaceURI(), parser.getLocalName(), depth - featureDepth);
                        gmlConsumerFlow.onGmlPropertyStart(parser.getNamespaceURI(), parser.getLocalName(), pathTracker.asList());
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumerFlow.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    }
                    depth += 1;
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    depth -= 1;
                    if (depth == 0) {
                        gmlConsumerFlow.onGmlEnd();
                    } else if (matchesFeatureType(parser.getLocalName())) {
                        inFeature = false;
                        gmlConsumerFlow.onGmlFeatureEnd();
                    } else if (inFeature) {
                        gmlConsumerFlow.onGmlPropertyEnd(parser.getLocalName(), pathTracker.asList());
                    }
                    pathTracker.track(depth - featureDepth);
                    break;

                case XMLStreamConstants.CHARACTERS:
                    if (inFeature) {
                        String text = parser.getText();
                        // TODO
                        if (!parser.isWhiteSpace()) // && !text.matches("\\s*")
                            gmlConsumerFlow.onGmlPropertyText(text);
                    }
                    break;

                // Do not support DTD, SPACE, NAMESPACE, NOTATION_DECLARATION, ENTITY_DECLARATION, PROCESSING_INSTRUCTION, COMMENT, CDATA
                // ATTRIBUTE is handled in START_ELEMENT implicitly

                default:
                    //advanceParser(in);
            }
        } catch (Exception e) {
            failStage(e);
        }

        return feedMeMore;
    }

    boolean matchesFeatureType(final String namespace, final String localName) {
        return featureType.getLocalPart().equals(localName) && Objects.nonNull(namespace) && featureType.getNamespaceURI().equals(namespace);
    }

    boolean matchesFeatureType(final String localName) {
        return featureType.getLocalPart().equals(localName);
    }
}
