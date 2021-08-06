/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaToSourcePathsVisitor;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlFeatureTypeParser {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(SqlFeatureTypeParser.class);

    private final SqlPathSyntax syntax;

    public SqlFeatureTypeParser(SqlPathSyntax syntax) {
        this.syntax = syntax;
    }


    public List<String> parse(FeatureSchema schema) {

      Map<List<String>, List<FeatureSchema>> accept = schema.accept(
          new SchemaToSourcePathsVisitor<>()).asMap()
          .entrySet()
          .stream()
          .sorted(Comparator.comparing(entry -> syntax.getPriorityFlag(entry.getKey().get(entry.getKey().size()-1)).orElse(10000)))
          .map(entry -> new AbstractMap.SimpleImmutableEntry<>(
              entry.getKey(), Lists.newArrayList(entry.getValue())))
          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

      Map<List<String>, Integer> pathCounter = new HashMap<>();

      List<String> collect1 = accept.entrySet().stream()
          .flatMap(entry -> entry.getValue().stream()
              .filter(FeatureSchema::isValue)
              .map(property -> toPathWithFlags2(entry.getKey(), property, pathCounter)))
          .collect(Collectors.toList());

      return collect1;
    }

    private String toPathWithFlags2(List<String> path, FeatureSchema property,
        Map<List<String>, Integer> pathCounter) {
                pathCounter.computeIfPresent(property.getFullPath(), (p, i) -> i + 1);
                pathCounter.putIfAbsent(property.getFullPath(), 0);
                int index = pathCounter.get(property.getFullPath());

                if (property.getSourcePaths().size() <= index) {
                  LOGGER.warn("No source path found: {} {}", property.getFullPath(), index);
                  return "";
                }
      String current = property.getSourcePaths().get(index);
                current = current.indexOf('{') > -1
                    ? current.substring(0, current.indexOf('{'))
                    : current;
      String[] split = current.split("/");
      String sourcePath = "/" + String.join("/", path.subList(0, path.size()-split.length))
                    + "/" + current;

                sourcePath = syntax.setQueryableFlag(sourcePath, property.getName());

                boolean isOid = property.isId();

        /*Optional<Integer> sortPriority = generalMapping.map(TargetMapping::getSortPriority);
        boolean isOid = generalMapping.flatMap(targetMapping -> Optional.ofNullable(targetMapping.getType()))
                                      .map(enumValue -> enumValue.toString()
                                                                 .equals("ID"))
                                      .orElse(false);
        Optional<String> queryableName = generalMapping.flatMap(targetMapping -> Optional.ofNullable(targetMapping.getName()));*/
                boolean isSpatial = property.isSpatial();

                if (isOid) {
                    sourcePath = syntax.setOidFlag(sourcePath);
                }
        /*if (sortPriority.isPresent()) {
            path = syntax.setPriorityFlag(path, sortPriority.get());
        }
        if (queryableName.isPresent()) {
            path = syntax.setQueryableFlag(path, queryableName.get());
        }*/
                if (isSpatial) {
                    sourcePath = syntax.setSpatialFlag(sourcePath);
                }

                boolean isTemporal = property.isTemporal();

                if (isTemporal) {
                    sourcePath = syntax.setTemporalFlag(sourcePath);
                }

                return sourcePath;
    }
}
