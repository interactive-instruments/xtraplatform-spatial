package de.ii.xtraplatform.feature.transformer.api;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.query.api.FeatureProviderData;
import de.ii.xtraplatform.feature.query.api.TargetMapping;

import java.util.Optional;

/**
 * @author zahnen
 */
public abstract class FeatureProviderDataTransformer extends FeatureProviderData {

    public abstract ImmutableMap<String, FeatureTypeMapping> getMappings();

    public abstract boolean isFeatureTypeEnabled(final String featureType);
}
