package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureProperty;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerRename extends FeaturePropertySchemaTransformer {

    String TYPE = "RENAME";

    @Override
    default String getType() {
        return TYPE;
    }

    @Override
    default FeatureProperty transform(FeatureProperty input) {
        return new ImmutableFeatureProperty.Builder().from(input)
                                                     .name(getParameter())
                                                     .build();
    }
}
