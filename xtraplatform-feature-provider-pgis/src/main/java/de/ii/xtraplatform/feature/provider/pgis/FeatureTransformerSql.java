/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.WktCoordinatesFormatter;
import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zahnen
 */

// TODO: this is FeatureTransformerSql, we need FeatureTransformerFromGeoJson
// TODO: better yet: move geometry parsing to parser, make FeatureTransformerFromConsumer generic
public class FeatureTransformerSql implements FeatureTransformer {
    private final SqlFeatureCreator sqlFeatureCreator;
    private String currentPropertyPath;
    private String currentRowPath;
    private Map<String,List<Integer>> lastMultiplicities = new LinkedHashMap<>();
    private final CrsTransformer crsTransformer;
    private CoordinatesWriterType.Builder cwBuilder;
    private StringWriter geometry = new StringWriter();
    private String currentFormat;
    private final List<String> ids;
    private final Optional<String> id;
    private boolean nestedClosed;

    public FeatureTransformerSql(SqlFeatureCreator sqlFeatureCreator, CrsTransformer crsTransformer) {
        this(sqlFeatureCreator, crsTransformer, null);
    }

    public FeatureTransformerSql(SqlFeatureCreator sqlFeatureCreator, CrsTransformer crsTransformer, String id) {
        this.sqlFeatureCreator = sqlFeatureCreator;
        this.crsTransformer = crsTransformer;
        this.ids = new ArrayList<>();
        this.id = Optional.ofNullable(id);
    }

    @Override
    public String getTargetFormat() {
        return "SQL";
    }

    public List<String> getIds() {
        return ids;
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {

    }

    @Override
    public void onEnd() throws Exception {
        //sqlFeatureCreator.runQueries().toCompletableFuture().join();
    }

    @Override
    public void onFeatureStart(TargetMapping mapping) throws Exception {

    }

    @Override
    public void onFeatureEnd() throws Exception {
        String id = this.id.isPresent() ? sqlFeatureCreator.runQueries(this.id.get())
                                    .toCompletableFuture()
                                    .get() : sqlFeatureCreator.runQueries()
                                                              .toCompletableFuture()
                                                              .get();
        ids.add(id);
        sqlFeatureCreator.reset();
    }

    @Override
    public void onPropertyStart(TargetMapping mapping, List<Integer> multiplicities) throws Exception {
        this.currentPropertyPath = mapping.getName();
        this.currentRowPath = Objects.nonNull(currentPropertyPath) ? currentPropertyPath.substring(0, currentPropertyPath.lastIndexOf("/")) : "";
        this.currentFormat = mapping.getFormat();

        //TODO: creates rows for every table, need to know which are 1_1 1_N M_N
        if (multiplicities.size() > 1) {
            //addRows(currentRowPath, multiplicities, lastMultiplicities);

            //this.lastMultiplicities.put(cleanPath(currentRowPath), multiplicities);
            lastMultiplicities.putAll(addRows(currentRowPath, multiplicities, lastMultiplicities));
        }
    }

    Map<String, List<Integer>> addRows(String path, List<Integer> multiplicities, Map<String, List<Integer>> previousMultiplicities) {
        Map<String, List<Integer>> newMultiplicities = new LinkedHashMap<>();

        if (!Objects.equals(previousMultiplicities.get(path), multiplicities)) {

            List<String> pathElements = Splitter.on("]/")
                                        .omitEmptyStrings()
                                        .splitToList(path);

            boolean parentPathDiffers = false;
            for (int i = 0; i < pathElements.size(); i++) {
                String subPath = cleanPath(createSubPath(pathElements, i + 1 - pathElements.size()));
                List<Integer> subPreviousMultiplicities = previousMultiplicities.getOrDefault(subPath, /*previousMultiplicities.containsKey(cleanPath(path)) ? previousMultiplicities.get(cleanPath(path)).subList(0, i+1) :*/ null);
                if (!parentPathDiffers) {
                    parentPathDiffers = !Objects.equals(subPreviousMultiplicities, multiplicities.subList(0, i + 1));
                }
                if (Objects.isNull(subPreviousMultiplicities) || parentPathDiffers || multiplicities.get(i) > subPreviousMultiplicities.get(i)) {
                    sqlFeatureCreator.row(subPath, multiplicities.subList(0, i+1));
                    newMultiplicities.put(subPath, multiplicities.subList(0, i+1));
                }
            }
        }

        return newMultiplicities;
    }

    List<String> getColumnNames(String path) {
        return path.lastIndexOf("/") > 0 ? Splitter.on(':').omitEmptyStrings().splitToList(path.substring(path.lastIndexOf("/")+1)) : ImmutableList.of();
    }

    String createSubPath(List<String> pathElements, int substractFromBack) {
        Joiner joiner = Joiner.on('/');
        return joiner.join(pathElements.subList(0, pathElements.size() + substractFromBack));
    }

    String cleanPath(String path) {
        return path.replaceAll("\\[\\w+\\]?(/|$)", "$1");
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        //TODO: if currentFormat (save above in onPropertyStart), duplicate path for double columns, split text and write two values
        if (Objects.nonNull(currentFormat)) {
            Matcher matcher = Pattern.compile(currentFormat).matcher(text);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Input does not match given pattern " + matcher.pattern());
            }
            getColumnNames(currentPropertyPath).forEach(col -> {

                sqlFeatureCreator.property(cleanPath(currentRowPath + "/" + col), matcher.group(col.replaceAll("_", "xyz"/*TODO UNDERSCORE_REPLACEMENT*/)));
            });
        } else {
            sqlFeatureCreator.property(cleanPath(currentPropertyPath), text);
        }
    }

    @Override
    public void onPropertyEnd() throws Exception {

    }

    @Override
    public void onGeometryStart(TargetMapping mapping, SimpleFeatureGeometry type, Integer dimension) throws Exception {
        this.currentPropertyPath = mapping.getName();
        SimpleFeatureGeometryFromToWkt wktType = SimpleFeatureGeometryFromToWkt.fromSimpleFeatureGeometry(type);
        geometry.append(wktType.toString());

        cwBuilder = CoordinatesWriterType.builder();
        cwBuilder.format(new WktCoordinatesFormatter(geometry));

        if (crsTransformer != null) {
            cwBuilder.transformer(crsTransformer);
        }

        if (dimension != null) {
            cwBuilder.dimension(dimension);
        }
        geometry.append("(");
    }

    @Override
    public void onGeometryNestedStart() throws Exception {
        if (nestedClosed) {
            nestedClosed = false;
            geometry.append(",");
        }
        geometry.append("(");
    }

    @Override
    public void onGeometryCoordinates(String text) throws Exception {
        Writer coordinatesWriter = cwBuilder.build();
        coordinatesWriter.write(text);
        coordinatesWriter.close();

        //geometry.append(text);
    }

    @Override
    public void onGeometryNestedEnd() throws Exception {
        geometry.append(")");
        nestedClosed = true;
    }

    @Override
    public void onGeometryEnd() throws Exception {
        geometry.append(")");
        sqlFeatureCreator.property(currentPropertyPath, geometry.toString());
        geometry = new StringWriter();
        nestedClosed = false;
    }
}
