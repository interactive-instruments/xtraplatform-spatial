/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.geojson;

import akka.stream.Shape;
import akka.stream.stage.GraphStageLogic;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public abstract class AbstractStreamingGeoJsonGraphStage extends GraphStageLogic {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamingGeoJsonGraphStage.class);

    private static JsonFactory factory = new JsonFactory();
    // will create async feeder based on byte[]:
    protected final JsonParser parser;
    protected final ByteArrayFeeder feeder;

    //private AsyncXMLInputFactory feeder = new InputFactoryImpl();
    //protected AsyncXMLStreamReader<AsyncByteArrayFeeder> parser = feeder.createAsyncFor(new byte[0]);

    int depth = -1;
    int featureDepth = 0;
    boolean inFeature = false;
    boolean inProperties = false;
    boolean inGeometry = false;
    JsonPathTracker pathTracker = new JsonPathTracker();

    private final FeatureConsumer featureConsumer;
    private OptionalLong numberReturned;
    private OptionalLong numberMatched;
    private boolean started;
    private Map<String, List<Integer>> multiplicities = new LinkedHashMap<>();
    private int lastNameIsArrayDepth = 0;
    private boolean singleFeature;
    private int nesting = 0;


    protected AbstractStreamingGeoJsonGraphStage(Shape shape, FeatureConsumer featureConsumer) throws IOException {
        super(shape);
        this.featureConsumer = featureConsumer;
        this.parser = factory.createNonBlockingByteArrayParser();
        this.feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
        this.numberReturned = OptionalLong.empty();
        this.numberMatched = OptionalLong.empty();
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
    }

    protected boolean advanceParser() throws Exception {
        //if (!parser.hasCurrentToken()) return true;

        boolean feedMeMore = false;

        try {
            JsonToken nextToken = parser.nextToken();
            String currentName = parser.currentName();
//LOGGER.debug("{}", currentName);
            if (Objects.isNull(nextToken)) {
                return true; // or completestage???
            }

            switch (nextToken) {
                case NOT_AVAILABLE:
                    feedMeMore = true;
                    break;

                case FIELD_NAME:
                    //if (started)
                    //pathTracker.track(currentName);
                    break;

                case START_OBJECT:
                    if (Objects.nonNull(currentName) && !started) {
                        switch (currentName) {
                            case "properties":
                            case "geometry":
                                startIfNecessary(false);
                                break;
                        }
                    }
                    if (Objects.nonNull(currentName) && started) {
                        switch (currentName) {
                            case "properties":
                                inProperties = true;
                                featureDepth = depth;
                                pathTracker.track(1);
                                break;
                            case "geometry":
                                inGeometry = true;
                                pathTracker.track(currentName,1);
                                break;
                            default:
                                if (inProperties || inGeometry)
                                    pathTracker.track(currentName);
                                break;
                        }
                    } else if (!pathTracker.asList().isEmpty() && started) {
                        increaseMultiplicity(pathTracker.toString());
                    } else if (depth == featureDepth - 1 && inFeature) {
                        //inFeature = false;
                        featureConsumer.onFeatureStart(pathTracker.asList());
                        this.multiplicities = new LinkedHashMap<>();
                    }
                    if (Objects.nonNull(currentName) || lastNameIsArrayDepth == 0)
                        depth += 1;
                    break;

                case START_ARRAY:
                    if (depth == 0 && Objects.nonNull(currentName)) {
                        switch (currentName) {
                            case "features":
                                startIfNecessary(true);
                                break;
                        }
                    } else if (Objects.nonNull(currentName) && (inProperties || inGeometry)) {
                        pathTracker.track(currentName);
                        lastNameIsArrayDepth += 1;
                        depth += 1;
                    } else if (inGeometry) {
                        pathTracker.track("[");
                        depth += 1;
                        nesting += 1;
                        if (nesting < 3)
                        featureConsumer.onPropertyStart(pathTracker.asList(), ImmutableList.of());
                    }
                    break;

                case END_ARRAY:
                    if (depth == 0 && Objects.nonNull(currentName)) {
                        switch (currentName) {
                            case "features":
                                inFeature = false;
                                break;
                        }
                    } else if (Objects.nonNull(currentName) && (inProperties || inGeometry)) {
                        if (inGeometry)
                            pathTracker.track(depth - featureDepth);
                        depth -= 1;
                        if (inProperties)
                        pathTracker.track(depth - featureDepth);
                        lastNameIsArrayDepth -= 1;
                    } else if (inGeometry) {
                        if (nesting < 3)
                        featureConsumer.onPropertyEnd(pathTracker.asList());
                        depth -= 1;
                        nesting -= 1;
                        pathTracker.track(depth - featureDepth + 1);
                    }
                    break;

                case END_OBJECT:
                    if (inGeometry)
                        pathTracker.track(depth - featureDepth);
                    if (Objects.nonNull(currentName) || lastNameIsArrayDepth == 0)
                        depth -= 1;
                    if (depth == -1) {
                        if (singleFeature) {
                            featureConsumer.onFeatureEnd(pathTracker.asList());
                        }
                        featureConsumer.onEnd();
                    } else if (depth == featureDepth - 1 && inFeature) {
                        //inFeature = false;
                        featureConsumer.onFeatureEnd(pathTracker.asList());
                    } else if (inFeature) {
                        //featureConsumer.onPropertyEnd(pathTracker.asList());
                    }

                    if (Objects.equals(currentName, "properties"))
                        inProperties = false;
                    if (Objects.equals(currentName, "geometry")) {
                        inGeometry = false;
                    }
                    if (inProperties)
                        pathTracker.track(depth - featureDepth);
                    break;

                case VALUE_STRING:
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                case VALUE_TRUE:
                case VALUE_FALSE:
                    if (depth == 0 && Objects.nonNull(currentName)) {
                        switch (currentName) {
                            case "numberReturned":
                                numberReturned = OptionalLong.of(parser.getLongValue());
                                break;
                            case "numberMatched":
                                numberMatched = OptionalLong.of(parser.getLongValue());
                                break;
                            case "type":
                                if (!parser.getValueAsString().equals("Feature")) break;
                            case "id":
                                startIfNecessary(false);
                                if (!currentName.equals("id")) break;

                                pathTracker.track(currentName, 1);
                                featureConsumer.onPropertyStart(pathTracker.asList(), multiplicities.getOrDefault(pathTracker.toString(), ImmutableList.of()));
                                featureConsumer.onPropertyText(parser.getValueAsString());
                                featureConsumer.onPropertyEnd(pathTracker.asList());
                                break;
                        }
                        //TODO: multiplicities
                    } else if (((inProperties || inGeometry)) || (inFeature && Objects.equals(currentName, "id"))) {
                        if (Objects.nonNull(currentName)) {
                            pathTracker.track(currentName);
                        }
                        if (inProperties) {
                            increaseMultiplicity(pathTracker.toString());
                        }
                        featureConsumer.onPropertyStart(pathTracker.asList(), multiplicities.getOrDefault(pathTracker.toString(), ImmutableList.of()));
                        featureConsumer.onPropertyText(parser.getValueAsString());
                        featureConsumer.onPropertyEnd(pathTracker.asList());
                        if (Objects.equals(currentName, "id"))
                            pathTracker.track(1);
                        else if (!inGeometry) {
                            pathTracker.track(depth - featureDepth);
                        } else if (inGeometry)
                            pathTracker.track(depth - featureDepth + 1);
                    } else if (inGeometry) {
                        // never reached
                        /*featureConsumer.onPropertyStart(pathTracker.asList(), ImmutableList.of());
                        featureConsumer.onPropertyText(parser.getValueAsString());
                        featureConsumer.onPropertyEnd(pathTracker.asList());*/
                    }

                    /*else if (matchesFeatureType(parser.getNamespaceURI(), parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;
                        featureConsumer.onFeatureStart(ImmutableList.of(getQualifiedName(parser.getNamespaceURI(), parser.getLocalName())));
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            featureConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    } else if (matchesFeatureType(parser.getLocalName())) {
                        inFeature = true;
                        featureDepth = depth;
                        featureConsumer.onNamespaceRewrite(featureType, parser.getNamespaceURI());
                        featureConsumer.onFeatureStart(ImmutableList.of(getQualifiedName(parser.getNamespaceURI(), parser.getLocalName())));
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            featureConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    } else if (inFeature) {
                        pathTracker.track(parser.getNamespaceURI(), parser.getLocalName(), depth - featureDepth);
                        featureConsumer.onPropertyStart(pathTracker.asList(), ImmutableMap.of());
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            featureConsumer.onGmlAttribute(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), pathTracker.asList(), parser.getAttributeValue(i));
                        }
                    }*/

                    if (inFeature) {
                        //String text = parser.getText();
                        // TODO
                        //if (!parser.isWhiteSpace()) // && !text.matches("\\s*")
                        //featureConsumer.onPropertyText(text);
                    }
                    break;

                // Do not support DTD, SPACE, NAMESPACE, NOTATION_DECLARATION, ENTITY_DECLARATION, PROCESSING_INSTRUCTION, COMMENT, CDATA
                // ATTRIBUTE is handled in START_ELEMENT implicitly

                default:
                    //advanceParser(in);
            }
        } catch (Exception e) {
            LOGGER.error("JSON stream parsing failed", e);
            failStage(e);
            throw e;
        }

        return feedMeMore;
    }

    private void startIfNecessary(boolean isCollection) throws Exception {
        if (!started) {
            featureConsumer.onStart(numberReturned, numberMatched);
            started = true;
            inFeature = true;
            if (isCollection) {
                featureDepth = 1;
            } else {
                singleFeature = true;
                featureConsumer.onFeatureStart(pathTracker.asList());
            }
        }
    }

    private void increaseMultiplicity(String path) {
        multiplicities.compute(pathTracker.toString(), (key, multiPath) -> {
            List<Integer> currentParent;
            if (path.contains(".")) {
                String parentPath = path.substring(0, path.lastIndexOf("."));
                if (multiplicities.containsKey(parentPath)) {
                    currentParent = multiplicities.get(parentPath);
                } else {
                    currentParent = Collections.nCopies(parentPath.split(".").length, 0);
                }
            } else {
                currentParent = ImmutableList.of();
            }

            int newMultiplicity = multiPath == null || (!currentParent.isEmpty() && !currentParent.equals(multiPath.subList(0, currentParent.size()))) ? 1 : multiPath.get(multiPath.size() - 1) + 1;

            return ImmutableList.<Integer>builder()
                    .addAll(currentParent)
                    .add(newMultiplicity)
                    .build();
        });
    }
}
