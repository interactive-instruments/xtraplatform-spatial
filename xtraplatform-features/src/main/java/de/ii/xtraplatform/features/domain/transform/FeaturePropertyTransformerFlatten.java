/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.util.function.BiFunction;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerFlatten extends FeaturePropertySchemaTransformer {

    String TYPE = "FLATTEN";

    @Override
    default String getType() {
        return TYPE;
    }

    enum INCLUDE {ALL, OBJECTS, ARRAYS}

    @Value.Default
    default INCLUDE include() {
        return INCLUDE.ALL;
    }

    BiFunction<String, String, String> flattenedPathProvider();

    @Override
    default FeatureSchema transform(String currentPropertyPath, FeatureSchema input) {
        String flattenedPath = flattenedPathProvider().apply(getParameter(), input.getName());
        return new ImmutableFeatureSchema.Builder()
            .from(input)
            .name(flattenedPath)
            .type(input.getType() == Type.VALUE_ARRAY ? input.getValueType().orElse(Type.STRING) : input.getType())
            .build();
    }
}
