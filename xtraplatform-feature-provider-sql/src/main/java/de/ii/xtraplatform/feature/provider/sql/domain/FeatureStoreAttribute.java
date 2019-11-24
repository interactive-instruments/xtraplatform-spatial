package de.ii.xtraplatform.feature.provider.sql.domain;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface FeatureStoreAttribute {

    String getName();

    List<String> getPath();

    Optional<String> getQueryable();

    @Value.Default
    default boolean isId() {
        return false;
    }

    @Value.Default
    default boolean isSpatial() {
        return false;
    }
}
