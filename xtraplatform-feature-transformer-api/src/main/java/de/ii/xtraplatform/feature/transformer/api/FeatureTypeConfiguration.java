package de.ii.xtraplatform.feature.transformer.api;

import org.immutables.value.Value;

import java.util.Optional;

/**
 * @author zahnen
 */
public abstract class FeatureTypeConfiguration {

    public abstract String getId();

    public abstract String getLabel();

    public abstract Optional<String> getDescription();

}
