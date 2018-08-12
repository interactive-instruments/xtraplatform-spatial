package de.ii.xtraplatform.feature.transformer.api;

import java.util.Optional;

/**
 * @author zahnen
 */
public interface FeatureTransformerService2 {
    Optional<FeatureTypeConfiguration> getFeatureTypeByName(String name);

    TransformingFeatureProvider getFeatureProvider();
}
