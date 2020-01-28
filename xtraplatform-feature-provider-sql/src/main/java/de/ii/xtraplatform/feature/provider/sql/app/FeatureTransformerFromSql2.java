/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.base.Splitter;
import de.ii.xtraplatform.api.functional.LambdaWithException;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer2;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public class FeatureTransformerFromSql2 implements FeatureConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerFromSql2.class);

    private final FeatureType featureType;
    protected final FeatureTransformer2 featureTransformer;
    private final String outputFormat;

    private boolean inProperty = false;
    private FeatureProperty geometry;
    private final List<String> fields;
    private final Map<List<String>, Map<List<Integer>, Integer>> pathVisitCounter;

    public FeatureTransformerFromSql2(FeatureType featureType, FeatureTransformer2 featureTransformer,
                                      List<String> fields) {
        this.featureType = featureType;
        this.featureTransformer = featureTransformer;
        this.outputFormat = featureTransformer.getTargetFormat();
        this.fields = fields;
        this.pathVisitCounter = new HashMap<>();
    }


    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched,
                        Map<String, String> additionalInfos) throws Exception {
        featureTransformer.onStart(numberReturned, numberMatched);
    }

    @Override
    public void onEnd() throws Exception {
        featureTransformer.onEnd();
    }

    @Override
    public void onFeatureStart(List<String> path, Map<String, String> additionalInfos) throws Exception {
        featureTransformer.onFeatureStart(featureType);
        this.pathVisitCounter.clear();
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        featureTransformer.onFeatureEnd();
    }

    @Override
    public void onPropertyStart(List<String> path, List<Integer> multiplicities,
                                Map<String, String> additionalInfos) throws Exception {
        if (shouldIgnoreProperty(path)) {
            return;
        }

        //TODO: save properties and text, run in loop in onPropertyEnd
        List<FeatureProperty> propertiesForPath = featureType.findPropertiesForPath(path);
        int propertyIndex = 0;

        if (propertiesForPath.size() > 1) {
            pathVisitCounter.putIfAbsent(path, new HashMap<>());
            Map<List<Integer>, Integer> multiPathVisitCounter = pathVisitCounter.get(path);
            multiPathVisitCounter.putIfAbsent(multiplicities, -1);
            multiPathVisitCounter.compute(multiplicities, (multiplicities2, counter) -> counter + 1);
            propertyIndex = multiPathVisitCounter.get(multiplicities);
        }

        if (propertiesForPath.size() > propertyIndex) {
            FeatureProperty featureProperty = propertiesForPath.get(propertyIndex);

            inProperty = true;
            if (featureProperty.isSpatial()) {
                geometry = featureProperty;
            } else {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("transforming property: {} {} {}", path, multiplicities, featureProperty);
                }
                featureTransformer.onPropertyStart(featureProperty, multiplicities);
            }
        }
    }

    private boolean shouldIgnoreProperty(List<String> path) {
        return !inProperty && !fields.contains("*") && !featureType.findPropertiesForPath(path)
                                                                   .stream()
                                                                   .anyMatch(this::isPropertyInWhitelist);
    }

    private boolean isPropertyInWhitelist(FeatureProperty featureProperty) {
        return featureProperty.isSpatial()
                || featureProperty.isId()
                || fields.contains(featureProperty.getName())
                || fields.stream()
                         .anyMatch(field -> {
                             String regex = field + "(?:\\[\\w*\\])?\\..*";
                             return featureProperty.getName()
                                                   .matches(regex);
                         });
    }

    //TODO: extract wkt parser
    @Override
    public void onPropertyText(String text) throws Exception {
        if (geometry != null) {
            StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(text));
            final int char128 = 128;
            final int skip32 = 32;
            final int char255 = 255;
            // set tokenizer to NOT parse numbers
            tokenizer.resetSyntax();
            tokenizer.wordChars('a', 'z');
            tokenizer.wordChars('A', 'Z');
            tokenizer.wordChars(char128 + skip32, char255);
            tokenizer.wordChars('0', '9');
            tokenizer.wordChars('-', '-');
            tokenizer.wordChars('+', '+');
            tokenizer.wordChars('.', '.');
            tokenizer.wordChars(',', ',');
            tokenizer.wordChars(' ', ' ');
            //tokenizer.whitespaceChars(0, ' ');
            //tokenizer.commentChar('#');

            String type = getNextWord(tokenizer);
            SimpleFeatureGeometry geometryType = SimpleFeatureGeometryFromToWkt.fromString(type)
                                                                               .toSimpleFeatureGeometry();

            if (!geometryType.isValid()) {
                geometry = null;
                return;
            }

            featureTransformer.onGeometryStart(geometry, geometryType, 2);

            switch (geometryType) {
                case POINT:
                    readCoordinates(tokenizer);
                    break;
                case MULTI_POINT:
                    readMultiPointText(tokenizer);
                    break;
                case LINE_STRING:
                    readCoordinates(tokenizer);
                    break;
                case MULTI_LINE_STRING:
                    readPolygonText(tokenizer);
                    break;
                case POLYGON:
                    readPolygonText(tokenizer);
                    break;
                case MULTI_POLYGON:
                    readMultiPolygonText(tokenizer);
                    break;
                case GEOMETRY_COLLECTION:
                    break;
            }

        } else if (inProperty) {
            featureTransformer.onPropertyText(text);
        }
    }

    @Override
    public void onPropertyEnd(List<String> path) throws Exception {
        if (geometry != null) {
            geometry = null;
            featureTransformer.onGeometryEnd();
            inProperty = false;
        } else if (inProperty) {
            featureTransformer.onPropertyEnd();
            inProperty = false;
        }
    }

    private static final String EMPTY = "EMPTY";
    private static final String COMMA = ",";
    private static final String L_PAREN = "(";
    private static final String R_PAREN = ")";

    private String getNextWord(StreamTokenizer tokenizer) throws IOException, ParseException {
        int type = tokenizer.nextToken();
        String value;
        switch (type) {
            case '(':
                value = L_PAREN;
                break;
            case ')':
                value = R_PAREN;
                break;
            case ',':
                value = COMMA;
                break;
            case StreamTokenizer.TT_WORD:
                String word = tokenizer.sval;
                if (word.equalsIgnoreCase(EMPTY)) {
                    value = EMPTY;
                    break;
                }
                value = word;
                break;
            default:
                // parseError("word", tokenizer);
                value = null;
                break;
        }
        return value;
    }

    // TODO: might use nested parentheses
    private void readMultiPointText(StreamTokenizer tokenizer) throws Exception {
        String nextToken = getNextEmptyOrOpener(tokenizer);
        if (!Objects.equals(nextToken, EMPTY)) {
            nextToken = getNextWord(tokenizer);
            featureTransformer.onGeometryNestedStart();
            Splitter.on(',')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(nextToken)
                    .forEach(LambdaWithException.consumerMayThrow(point -> {
                        featureTransformer.onGeometryCoordinates(point);
                    }));
            featureTransformer.onGeometryNestedEnd();
            nextToken = getNextCloserOrComma(tokenizer);
        }
    }

    private void readMultiPolygonText(StreamTokenizer tokenizer) throws Exception {
        String nextToken = getNextEmptyOrOpener(tokenizer);
        if (Objects.equals(nextToken, EMPTY)) {
            return;
        }
        featureTransformer.onGeometryNestedStart();
        readPolygonText(tokenizer);
        featureTransformer.onGeometryNestedEnd();
        nextToken = getNextCloserOrComma(tokenizer);
        while (Objects.equals(nextToken, COMMA)) {
            featureTransformer.onGeometryNestedStart();
            readPolygonText(tokenizer);
            featureTransformer.onGeometryNestedEnd();
            nextToken = getNextCloserOrComma(tokenizer);
        }
    }

    private void readPolygonText(StreamTokenizer tokenizer) throws Exception {
        String nextToken = getNextEmptyOrOpener(tokenizer);
        if (Objects.equals(nextToken, EMPTY)) {
            return;
        }
        featureTransformer.onGeometryNestedStart();
        readCoordinates(tokenizer);
        featureTransformer.onGeometryNestedEnd();
        nextToken = getNextCloserOrComma(tokenizer);
        while (Objects.equals(nextToken, COMMA)) {
            featureTransformer.onGeometryNestedStart();
            readCoordinates(tokenizer);
            featureTransformer.onGeometryNestedEnd();
            nextToken = getNextCloserOrComma(tokenizer);
        }
    }

    private void readCoordinates(StreamTokenizer tokenizer) throws Exception {
        String nextToken = getNextEmptyOrOpener(tokenizer);
        if (!Objects.equals(nextToken, EMPTY)) {
            nextToken = getNextWord(tokenizer);
            featureTransformer.onGeometryCoordinates(nextToken);
            nextToken = getNextCloserOrComma(tokenizer);
        }
    }

    private String getNextEmptyOrOpener(StreamTokenizer tokenizer)
            throws IOException, ParseException {
        String nextWord = getNextWord(tokenizer);
        if (EMPTY.equals(nextWord) || L_PAREN.equals(nextWord)) {
            return nextWord;
        }
        //parseError(EMPTY + " or " + L_PAREN, tokenizer);
        return null;
    }

    private String getNextCloserOrComma(StreamTokenizer tokenizer)
            throws IOException, ParseException {
        String nextWord = getNextWord(tokenizer);
        if (COMMA.equals(nextWord) || R_PAREN.equals(nextWord)) {
            return nextWord;
        }
        //parseError(COMMA + " or " + R_PAREN, tokenizer);
        return null;
    }
}
