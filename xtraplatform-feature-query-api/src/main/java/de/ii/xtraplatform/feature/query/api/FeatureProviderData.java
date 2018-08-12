package de.ii.xtraplatform.feature.query.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xsf.dropwizard.cfg.JacksonProvider;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "providerType")
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public abstract class FeatureProviderData {

    public abstract String getProviderType();
}
