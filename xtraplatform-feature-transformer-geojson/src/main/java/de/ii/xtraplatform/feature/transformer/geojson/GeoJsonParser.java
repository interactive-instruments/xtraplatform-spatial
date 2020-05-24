/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.geojson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureReaderGeneric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public class GeoJsonParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonParser.class);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();


    private final FeatureReaderGeneric featureReader;
    private final JsonParser parser;
    private final ByteArrayFeeder feeder;
    private final JsonPathTracker pathTracker;

    private boolean started;
    private int depth = -1;
    private int featureDepth = 0;
    private boolean inFeature = false;
    private boolean inProperties = false;
    private boolean inGeometry = false;
    private int lastNameIsArrayDepth = 0;
    private boolean singleFeature;
    private int startArray = 0;
    private int endArray = 0;
    private String geometryType;

    private OptionalLong numberReturned;
    private OptionalLong numberMatched;


    protected GeoJsonParser(FeatureReaderGeneric featureReader) throws IOException {
        this.featureReader = featureReader;
        this.parser = JSON_FACTORY.createNonBlockingByteArrayParser();
        this.feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
        this.pathTracker = new JsonPathTracker();
        this.numberReturned = OptionalLong.empty();
        this.numberMatched = OptionalLong.empty();
    }

    public void parse(String data) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        feedInput(dataBytes, 0, dataBytes.length);
        endOfInput();
    }

    public boolean feedInput(byte[] data, int offset, int end) throws Exception {
        feeder.feedInput(data, offset, end);


        boolean feedMeMore = false;
        while (!feedMeMore) {
            feedMeMore = advanceParser();
        }

        return feedMeMore;
    }

    public void endOfInput() {
        feeder.endOfInput();
    }

    public boolean advanceParser() throws Exception {

        boolean feedMeMore = false;

        try {
            JsonToken nextToken = parser.nextToken();
            String currentName = parser.currentName();

            //TODO: null is end-of-input
            if (Objects.isNull(nextToken)) {
                return true; // or completestage???
            }

            switch (nextToken) {
                case NOT_AVAILABLE:
                    feedMeMore = true;
                    break;

                case FIELD_NAME:
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
                                pathTracker.track(currentName, 1);
                                break;
                            default:
                                if (inProperties/* || inGeometry*/) {
                                    pathTracker.track(currentName);
                                }
                                break;
                        }
                        // nested array_object start
                    } else if (!pathTracker.asList()
                                           .isEmpty() && started) {
                        featureReader.onObjectStart(pathTracker.asList(), ImmutableMap.of());
                        // feature in collection start
                    } else if (depth == featureDepth - 1 && inFeature) {
                        //inFeature = false;
                        featureReader.onFeatureStart(pathTracker.asList(), ImmutableMap.of());
                    }

                    // nested object start?
                    if (Objects.nonNull(currentName) || lastNameIsArrayDepth == 0) {
                        depth += 1;
                        if (depth > featureDepth && (inProperties /*|| inGeometry*/) && !Objects.equals(currentName, "properties")) {
                            featureReader.onObjectStart(pathTracker.asList(), ImmutableMap.of());
                        }
                    }
                    break;

                case START_ARRAY:
                    // start features array
                    if (depth == 0 && Objects.nonNull(currentName)) {
                        switch (currentName) {
                            case "features":
                                startIfNecessary(true);
                                break;
                        }
                        // start prop/coord array
                    } else if (Objects.nonNull(currentName) && (inProperties || inGeometry)) {
                        if (!inGeometry) {
                            pathTracker.track(currentName);
                        }
                        lastNameIsArrayDepth += 1;
                        depth += 1;

                        featureReader.onArrayStart(pathTracker.asList(), ImmutableMap.of("geometryType", geometryType));
                        // start nested geo array
                    } else if (inGeometry) {
                        if (endArray > 0) {
                            for (int i = 0; i < endArray - 1; i++) {
                                featureReader.onArrayEnd(pathTracker.asList());
                            }
                            endArray = 0;
                        }
                        depth += 1;
                        startArray++;
                    }
                    break;

                case END_ARRAY:
                    // end features array
                    if (depth == 0 && Objects.nonNull(currentName)) {
                        switch (currentName) {
                            case "features":
                                inFeature = false;
                                break;
                        }
                        // end prop/coord array
                    } else if (Objects.nonNull(currentName) && (inProperties || inGeometry)) {
                        if (endArray > 0) {
                            for (int i = 0; i < endArray - 1; i++) {
                                featureReader.onArrayEnd(pathTracker.asList());
                            }
                            endArray = 0;
                        }

                        featureReader.onArrayEnd(pathTracker.asList());

                        if (inGeometry)
                            pathTracker.track(depth - featureDepth);
                        depth -= 1;
                        if (inProperties)
                            pathTracker.track(depth - featureDepth);
                        lastNameIsArrayDepth -= 1;
                        // end nested geo array
                    } else if (inGeometry) {
                        endArray++;
                        depth -= 1;
                        pathTracker.track(depth - featureDepth + 1);
                    }
                    break;

                case END_OBJECT:

                    // end nested object
                    if (Objects.nonNull(currentName) || lastNameIsArrayDepth == 0) {
                        if (depth > featureDepth && (inProperties || inGeometry) && !Objects.equals(currentName, "properties")) {
                            featureReader.onObjectEnd(pathTracker.asList());
                        }
                        // end geo
                        if (inGeometry)
                            pathTracker.track(depth - featureDepth);

                        depth -= 1;
                    } else if (lastNameIsArrayDepth > 0) {
                        featureReader.onObjectEnd(pathTracker.asList());
                    }

                    // end all
                    if (depth == -1) {
                        if (singleFeature) {
                            featureReader.onFeatureEnd(pathTracker.asList());
                        }
                        featureReader.onEnd();
                        // end feature in collection
                    } else if (depth == featureDepth - 1 && inFeature) {
                        //inFeature = false;
                        featureReader.onFeatureEnd(pathTracker.asList());
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
                    // feature or collection prop value
                    if (depth == 0 && Objects.nonNull(currentName)) {
                        switch (currentName) {
                            case "numberReturned":
                                numberReturned = OptionalLong.of(parser.getLongValue());
                                break;
                            case "numberMatched":
                                numberMatched = OptionalLong.of(parser.getLongValue());
                                break;
                            case "type":
                                if (!parser.getValueAsString()
                                           .equals("Feature")) break;
                            case "id":
                                startIfNecessary(false);
                                if (!currentName.equals("id")) break;

                                pathTracker.track(currentName, 1);

                                featureReader.onValue(pathTracker.asList(), parser.getValueAsString(), ImmutableMap.of());
                                break;
                        }
                        // feature id or props or geo value
                    } else if (((inProperties || inGeometry)) || (inFeature && Objects.equals(currentName, "id"))) {

                        if (Objects.nonNull(currentName)) {
                            if (inGeometry && Objects.equals(currentName, "type")) {
                                geometryType = GeoJsonGeometryType.forString(parser.getValueAsString()).toSimpleFeatureGeometry().toString();
                                featureReader.onObjectStart(pathTracker.asList(), ImmutableMap.of("geometryType", geometryType));
                                break;
                            }
                            pathTracker.track(currentName);
                        }

                        if (inGeometry && startArray > 0) {
                            for (int i = 0; i < startArray - 1; i++) {
                                featureReader.onArrayStart(pathTracker.asList(), ImmutableMap.of("geometryType", geometryType));
                            }
                            startArray = 0;
                        }

                        featureReader.onValue(pathTracker.asList(), parser.getValueAsString(), ImmutableMap.of());

                        // feature id
                        if (Objects.equals(currentName, "id"))
                            pathTracker.track(1);
                            // why reset depth?
                        else if (!inGeometry) {
                            pathTracker.track(depth - featureDepth);
                        } else if (inGeometry)
                            pathTracker.track(depth - featureDepth + 1);
                    }
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("JSON stream parsing failed", e);
            throw e;
        }

        return feedMeMore;
    }

    private void startIfNecessary(boolean isCollection) throws Exception {
        if (!started) {
            featureReader.onStart(numberReturned, numberMatched, ImmutableMap.of());
            started = true;
            inFeature = true;
            if (isCollection) {
                featureDepth = 1;
            } else {
                singleFeature = true;
                featureReader.onFeatureStart(pathTracker.asList(), ImmutableMap.of());
            }
        }
    }
}
