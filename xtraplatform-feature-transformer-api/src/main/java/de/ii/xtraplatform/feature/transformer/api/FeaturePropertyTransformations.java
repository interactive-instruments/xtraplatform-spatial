package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.provider.api.FeatureProperty;

import java.util.List;
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
            transformedSchema = schemaTransformer.transform(transformedSchema);
        }

        String transformedValue = getValue(wrapper);

        for (FeaturePropertyValueTransformer valueTransformer : getValueTransformers()) {
            transformedValue = valueTransformer.transform(transformedValue);
        }

        return transform(wrapper, transformedSchema, transformedValue);
    }

    Optional<T> transform(T wrapper, FeatureProperty transformedSchema, String transformedValue);

}
