/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import akka.stream.Shape;
import akka.stream.stage.GraphStageLogic;
import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.xml.domain.XMLPathTracker;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

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

    private final List<QName> featureTypes;
    private final FeatureConsumer gmlConsumer;
    private final StringBuilder buffer;
    private final GmlMultiplicityTracker multiplicityTracker;
    private boolean isBuffering;

    protected AbstractStreamingGmlGraphStage(Shape shape, List<QName> featureTypes,
                                             FeatureConsumer gmlConsumer) throws XMLStreamException {
        super(shape);
        this.featureTypes = featureTypes;
        this.gmlConsumer = gmlConsumer;
        this.buffer = new StringBuilder();
        this.multiplicityTracker = new GmlMultiplicityTracker();
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
    }

    protected boolean advanceParser() throws Exception {
        if (!parser.hasNext()) return true;

        boolean feedMeMore = false;

        try {
            switch (parser.next()) {
                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    feedMeMore = true;
                    break;

                case XMLStreamConstants.START_DOCUMENT:

                    break;

                case XMLStreamConstants.END_DOCUMENT:

                    //completeStage();
                    break;

                case XMLStreamConstants.START_ELEMENT:
                    if (depth == 0) {
                        OptionalLong numberMatched;
                        OptionalLong numberReturned;
                        try {
                            numberMatched = OptionalLong.of(Long.parseLong(parser.getAttributeValue(null, "numberMatched")));
                        } catch (NumberFormatException e) {
                            numberMatched = OptionalLong.empty();
                        }
                        try {
                            numberReturned = OptionalLong.of(Long.parseLong(parser.getAttributeValue(null, "numberReturned")));
                        } catch (NumberFormatException e) {
                            numberReturned = OptionalLong.empty();
                        }
                        Map<String, String> attributes = new LinkedHashMap<>();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            attributes.put(getQualifiedName(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i)), parser.getAttributeValue(i));
                        }
                        gmlConsumer.onStart(numberReturned, numberMatched, attributes);
                        /*for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i), ImmutableList.of());
                        }*/
                    } else if (matchesFeatureType(parser.getNamespaceURI(), parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;
                        Map<String, String> attributes = new LinkedHashMap<>();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            attributes.put(getQualifiedName(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i)), parser.getAttributeValue(i));
                        }
                        gmlConsumer.onFeatureStart(ImmutableList.of(getQualifiedName(parser.getNamespaceURI(), parser.getLocalName())), attributes);
                        /*for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i), ImmutableList.of());
                        }*/
                    } else if (matchesFeatureType(parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;
                        //gmlConsumer.onNamespaceRewrite(getMatchingFeatureType(parser.getLocalName()), parser.getNamespaceURI());

                        Map<String, String> attributes = new LinkedHashMap<>();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            attributes.put(getQualifiedName(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i)), parser.getAttributeValue(i));
                        }
                        gmlConsumer.onFeatureStart(ImmutableList.of(getQualifiedName(parser.getNamespaceURI(), parser.getLocalName())), attributes);
                        /*for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i), ImmutableList.of());
                        }*/
                    } else if (inFeature) {
                        pathTracker.track(parser.getNamespaceURI(), parser.getLocalName(), depth - featureDepth, false);
                        multiplicityTracker.track(pathTracker.asList());

                        Map<String, String> attributes = new LinkedHashMap<>();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            attributes.put(getQualifiedName(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i)), parser.getAttributeValue(i));
                        }
                        gmlConsumer.onPropertyStart(pathTracker.asList(), multiplicityTracker.getMultiplicitiesForPath(pathTracker.asList()), attributes);
                        /*for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i), multiplicityTracker.getMultiplicitiesForPath(pathTracker.asList()));
                        }*/
                    }
                    depth += 1;
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    if (isBuffering) {
                        this.isBuffering = false;
                        if (buffer.length() > 0) {
                            gmlConsumer.onPropertyText(buffer.toString());
                            buffer.setLength(0);
                        }
                    }

                    depth -= 1;
                    if (depth == 0) {
                        gmlConsumer.onEnd();
                    } else if (matchesFeatureType(parser.getLocalName())) {
                        inFeature = false;
                        gmlConsumer.onFeatureEnd(ImmutableList.of(getQualifiedName(parser.getNamespaceURI(), parser.getLocalName())));
                        multiplicityTracker.reset();
                    } else if (inFeature) {
                        gmlConsumer.onPropertyEnd(pathTracker.asList());
                    }
                    pathTracker.track(depth - featureDepth);
                    break;

                case XMLStreamConstants.CHARACTERS:
                    if (inFeature) {
                        //String text = parser.getText();
                        // TODO: whitespace
                        // TODO: remove coalesce in transformer
                        if (!parser.isWhiteSpace()) { // && !text.matches("\\s*")
                            this.isBuffering = true;
                            buffer.append(parser.getText());
                            //gmlConsumer.onPropertyText(text);
                        }
                    }
                    break;

                // Do not support DTD, SPACE, NAMESPACE, NOTATION_DECLARATION, ENTITY_DECLARATION, PROCESSING_INSTRUCTION, COMMENT, CDATA
                // ATTRIBUTE is handled in START_ELEMENT implicitly

                default:
                    //advanceParser(in);
            }
        } catch (Exception e) {
            failStage(e);
            throw e;
        }
        return feedMeMore;
    }

    boolean matchesFeatureType(final String namespace, final String localName) {
        return featureTypes.stream()
                           .anyMatch(featureType -> featureType.getLocalPart()
                                                               .equals(localName) && Objects.nonNull(namespace) && featureType.getNamespaceURI()
                                                                                                                              .equals(namespace));
    }

    boolean matchesFeatureType(final String localName) {
        return featureTypes.stream()
                           .anyMatch(featureType -> featureType.getLocalPart()
                                                               .equals(localName));
    }

    QName getMatchingFeatureType(final String localName) {
        return featureTypes.stream()
                           .filter(featureType -> featureType.getLocalPart()
                                                             .equals(localName))
                           .findFirst()
                           .orElse(null);
    }

    private String getQualifiedName(String namespaceUri, String localName) {
        return Optional.ofNullable(namespaceUri)
                       .map(ns -> ns + ":" + localName)
                       .orElse(localName);
    }
}
