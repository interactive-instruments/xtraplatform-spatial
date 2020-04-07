package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import org.immutables.value.Value;

@Value.Immutable
public interface SqlCondition {

    String getColumn();

    FeatureStoreAttributesContainer getTable();

    String getExpression();

    @Value.Default
    default boolean isOr() {
        return false;
    }
}
