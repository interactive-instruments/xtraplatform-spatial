/**
 * Copyright 2018 interactive instruments GmbH
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
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.util.xml.XMLPathTracker;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
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

    private final QName featureType;
    private final GmlConsumer gmlConsumer;
    private final StringBuilder buffer;
    private boolean isBuffering;

    protected AbstractStreamingGmlGraphStage(Shape shape, QName featureType, GmlConsumer gmlConsumer) throws XMLStreamException {
        super(shape);
        this.featureType = featureType;
        this.gmlConsumer = gmlConsumer;
        this.buffer = new StringBuilder();
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

                        gmlConsumer.onStart(numberReturned, numberMatched);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    } else if (matchesFeatureType(parser.getNamespaceURI(), parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;
                        gmlConsumer.onFeatureStart(ImmutableList.of(getQualifiedName(parser.getNamespaceURI(), parser.getLocalName())));
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    } else if (matchesFeatureType(parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;
                        gmlConsumer.onNamespaceRewrite(featureType, parser.getNamespaceURI());
                        gmlConsumer.onFeatureStart(ImmutableList.of(getQualifiedName(parser.getNamespaceURI(), parser.getLocalName())));
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    } else if (inFeature) {
                        pathTracker.track(parser.getNamespaceURI(), parser.getLocalName(), depth - featureDepth);
                        gmlConsumer.onPropertyStart(pathTracker.asList(), ImmutableList.of());
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            gmlConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
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
        return featureType.getLocalPart().equals(localName) && Objects.nonNull(namespace) && featureType.getNamespaceURI().equals(namespace);
    }

    boolean matchesFeatureType(final String localName) {
        return featureType.getLocalPart().equals(localName);
    }

    private String getQualifiedName(String namespaceUri, String localName) {
        return Optional.ofNullable(namespaceUri)
                       .map(ns -> ns + ":" + localName)
                       .orElse(localName);
    }
}
