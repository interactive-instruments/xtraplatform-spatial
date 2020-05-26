/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ListMultimap;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Style(deepImmutablesDetection = true)
public interface FeatureSql2 {


    @Value.Modifiable
    interface Instance extends Object {

        //TODO
        Map<List<FeatureStoreRelation>, List<Integer>> getRowCounts();

    }

    @Value.Modifiable
    interface NestedObject extends Object {

        @Value.Parameter
        List<FeatureStoreRelation> getPath();

    }

    interface Object {

        ListMultimap<List<FeatureStoreRelation>, NestedObject> getNestedObjects();

        Map<String, String> getValues();

        Map<String, String> getIds();


        default NestedObject getRelatedValues(List<FeatureStoreRelation> path, List<Integer> parentRows) {
            if (path.isEmpty() || !getNestedObjects().containsKey(path.subList(0, 1))) {
                throw new IllegalStateException(String.format("No values found for path %s", path));
            }

            if (path.size() > 1) {
                int nestingLevel = path.get(0).isM2N() ? 2 : 1;

                return getNestedObjects().get(path.subList(0, 1))
                                         .get(parentRows.get(0))
                                         .getRelatedValues(path.subList(1, path.size()), parentRows.subList(nestingLevel, parentRows.size()));
            }


            List<NestedObject> values = getNestedObjects().get(path);
            int row = parentRows.isEmpty() ? 0 : parentRows.get(path.get(0).isM2N() ? 1 : 0);


            if (row > values.size() - 1) {
                throw new IllegalStateException(String.format("No values found for row %s of %s", row, path));
            }

            return values.get(row);
        }

        default Object getCurrentRelatedValues(List<FeatureStoreRelation> path) {
            if (path.isEmpty()) {
                return this;
            }

            int row = Optional.ofNullable(getNestedObjects().get(path))
                              .map(List::size)
                              .orElse(0);

            return getRelatedValues(path, row);
        }

        default NestedObject getRelatedValues(List<FeatureStoreRelation> path, int row) {
            if (path.isEmpty() || !getNestedObjects().containsKey(path.subList(0, 1))) {
                throw new IllegalStateException(String.format("No values found for path %s", path));
            }

            if (path.size() > 1) {
                return getNestedObjects().get(path.subList(0, 1))
                                         .get(row)
                                         .getRelatedValues(path.subList(1, path.size()), row);
            }


            List<NestedObject> values = getNestedObjects().get(path);


            if (row > values.size() - 1) {
                throw new IllegalStateException(String.format("No values found for row %s of %s", row, path));
            }

            return values.get(row);
        }
    }

}
