/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerObjectReduce extends FeaturePropertyContextTransformer {

    String TYPE = "REDUCE";

    @Override
    default String getType() {
        return TYPE;
    }

    @Override
    default ModifiableContext transform(String currentPropertyPath, ModifiableContext context) {
        if (Objects.equals(currentPropertyPath, getPropertyPath()) && context.currentSchema().filter(
            SchemaBase::isObject).isPresent()) {
            if (!context.transformed().containsKey(getPropertyPath())) {
                //String allPaths = String.join("|",
                //    context.currentSchema().get().getAllNestedPropertyPathStrings());
                context.putTransformed(getPropertyPath(), TYPE);// + "|" + allPaths);
                context.setIsBuffering(true);
            } else {
                context.transformed().remove(getPropertyPath());
                context.setIsBuffering(false);
                context.setCustomSchema(new ImmutableFeatureSchema.Builder()
                    .from(context.currentSchema().get())
                    .type(context.currentSchema().get().isArray() ? Type.VALUE_ARRAY : Type.STRING)
                    .valueType(Type.STRING)
                    .objectType(Optional.empty())
                    .propertyMap(ImmutableMap.of())
                    .build());
            }
        } else if (currentPropertyPath.startsWith(getPropertyPath()) && context.currentSchema().filter(
            SchemaBase::isValue).isPresent() && context.transformed().containsKey(getPropertyPath())) {
            //context.setDoNotPush(true);
            context.putValueBuffer(currentPropertyPath, context.value());
            //context.putTransformed(getPropertyPath(), context.transformed().get(getPropertyPath()).replace("|" + currentPropertyPath, ""));

            if (context.transformed().get(getPropertyPath()).equals(TYPE)) {
                //context.transformed().remove(getPropertyPath());
                //context.putTransformed(currentPropertyPath, TYPE);

            }
        }

        return context;
    }
}
