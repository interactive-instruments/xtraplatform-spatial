/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import com.google.common.base.Splitter;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
public class FeatureTransformerFromSql implements FeatureConsumer {

    private final FeatureTypeMapping featureTypeMapping;
    protected final FeatureTransformer featureTransformer;
    private final String outputFormat;

    private boolean inProperty = false;
    private TargetMapping geometryMapping;
    private final List<String> fields;

    public FeatureTransformerFromSql(FeatureTypeMapping featureTypeMapping, FeatureTransformer featureTransformer, List<String> fields) {
        this.featureTypeMapping = featureTypeMapping;
        this.featureTransformer = featureTransformer;
        this.outputFormat = featureTransformer.getTargetFormat();
        this.fields = fields;
    }


    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {
        featureTransformer.onStart(numberReturned, numberMatched);
    }

    @Override
    public void onEnd() throws Exception {
        featureTransformer.onEnd();
    }

    @Override
    public void onFeatureStart(List<String> path) throws Exception {
        final TargetMapping mapping = featureTypeMapping.findMappings(path, outputFormat)
                                                        .orElse(null);
        featureTransformer.onFeatureStart(mapping);
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        featureTransformer.onFeatureEnd();
    }

    @Override
    public void onPropertyStart(List<String> path, List<Integer> multiplicities) throws Exception {
        if (shouldIgnoreProperty(path)) {
            return;
        }

        featureTypeMapping.findMappings(path, outputFormat)
                          .ifPresent(consumerMayThrow(mapping -> {
                              inProperty = true;
                              if (mapping.isSpatial()) {
                                  geometryMapping = mapping;
                              } else {
                                  featureTransformer.onPropertyStart(mapping, multiplicities);
                              }
                          }));
    }

    private boolean shouldIgnoreProperty(List<String> path) {
        return !inProperty && !fields.contains("*") && !featureTypeMapping.findMappings(path, TargetMapping.BASE_TYPE)
                                                                          .filter(this::isPropertyInWhitelist)
                                                                          .isPresent();
    }

    private boolean isPropertyInWhitelist(TargetMapping targetMapping) {
        return targetMapping.isSpatial()
                || targetMapping.getType()
                                .toString()
                                .toUpperCase()
                                .equals("ID")
                || fields.contains(targetMapping.getName())
                || fields.stream()
                         .anyMatch(field -> {
                             String regex = field + "(?:\\[\\w*\\])?\\..*";
                             return targetMapping.getName()
                                          .matches(regex);
                         });
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        if (geometryMapping != null) {
            //TODO: parse wkt
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
                geometryMapping = null;
                return;
            }

            featureTransformer.onGeometryStart(geometryMapping, geometryType, 2);

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
        if (geometryMapping != null) {
            geometryMapping = null;
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
                    .forEach(consumerMayThrow(point -> {
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
        //TODO
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
