package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureProperty;

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

    default Optional<T> transform(T wrapper, FeatureProperty schema) {
        FeatureProperty transformedSchema = schema;

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

    Optional<T> transform(T wrapper, FeatureProperty transformedSchema, String transformedValue);

}
