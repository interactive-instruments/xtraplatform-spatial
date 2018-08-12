package de.ii.xtraplatform.feature.query.api;

/**
 * @author zahnen
 */
public interface FeatureProviderRegistry {
    boolean isSupported(String type);

    FeatureProvider createFeatureProvider(FeatureProviderData featureProviderData);
}
