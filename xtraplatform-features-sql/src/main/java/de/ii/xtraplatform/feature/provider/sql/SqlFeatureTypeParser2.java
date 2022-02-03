/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTypeV2;

import java.util.List;

public class SqlFeatureTypeParser2 {

    private final SqlPathSyntax syntax;

    public SqlFeatureTypeParser2(SqlPathSyntax syntax) {
        this.syntax = syntax;
    }


    public List<String> parse(FeatureTypeV2 featureType) {
        return featureType.getProperties()
                          .values()
                                  .stream()
                                  .map(this::toPathWithFlags)
                                  .collect(ImmutableList.toImmutableList());
    }

    private String toPathWithFlags(FeatureSchema property) {
        String path = property.getSourcePath().orElse("");

        path = syntax.setQueryableFlag(path, property.getName());

        boolean isOid = property.isId();

        /*Optional<Integer> sortPriority = generalMapping.map(TargetMapping::getSortPriority);
        boolean isOid = generalMapping.flatMap(targetMapping -> Optional.ofNullable(targetMapping.getType()))
                                      .map(enumValue -> enumValue.toString()
                                                                 .equals("ID"))
                                      .orElse(false);
        Optional<String> queryableName = generalMapping.flatMap(targetMapping -> Optional.ofNullable(targetMapping.getName()));*/
        boolean isSpatial = property.getType() == FeatureSchema.Type.GEOMETRY;

        if (isOid) {
            path = syntax.setOidFlag(path);
        }
        /*if (sortPriority.isPresent()) {
            path = syntax.setPriorityFlag(path, sortPriority.get());
        }
        if (queryableName.isPresent()) {
            path = syntax.setQueryableFlag(path, queryableName.get());
        }*/
        if (isSpatial) {
            path = syntax.setSpatialFlag(path);
        }

        return path;
    }
}
