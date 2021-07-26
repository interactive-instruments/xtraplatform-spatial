/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface FeaturePropertyTransformations<T> {

    List<FeaturePropertySchemaTransformer> getSchemaTransformers();

    List<FeaturePropertyValueTransformer> getValueTransformers();

    //FeatureProperty getSchema(T wrapper);

    String getValue(T wrapper);

    /*default T transform(T wrapper) {
        return transform(wrapper, getSchema(wrapper));
    }*/

    default Optional<T> transform(T wrapper, FeatureSchema schema) {
        FeatureSchema transformedSchema = schema;

        for (FeaturePropertySchemaTransformer schemaTransformer : getSchemaTransformers()) {
            if (Objects.nonNull(transformedSchema)) {
                transformedSchema = schemaTransformer.transform(transformedSchema);
            }
        }

        String transformedValue = getValue(wrapper);

        for (FeaturePropertyValueTransformer valueTransformer : getValueTransformers()) {
            transformedValue = valueTransformer.transform(transformedValue);
        }

        return transform(wrapper, transformedSchema, transformedValue);
    }

    Optional<T> transform(T wrapper, FeatureSchema transformedSchema, String transformedValue);

}
