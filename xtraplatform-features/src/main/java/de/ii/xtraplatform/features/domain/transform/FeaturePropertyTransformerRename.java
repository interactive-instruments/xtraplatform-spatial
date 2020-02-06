package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty;
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
