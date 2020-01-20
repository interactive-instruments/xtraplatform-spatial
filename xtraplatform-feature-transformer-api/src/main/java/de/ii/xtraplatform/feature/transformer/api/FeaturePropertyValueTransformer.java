package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface FeaturePropertyValueTransformer extends FeaturePropertyTransformer<String> {

    Logger LOGGER = LoggerFactory.getLogger(FeaturePropertyValueTransformer.class);

    @JsonIgnore
    Optional<String> getPropertyName();

    @JsonIgnore
    List<FeatureProperty.Type> getSupportedPropertyTypes();

    default boolean matches(FeatureProperty featureProperty) {
        boolean isNameMatching = getPropertyName().isPresent() && Objects.equals(getPropertyName().get(), featureProperty.getName());
        boolean isTypeMatching = getSupportedPropertyTypes().isEmpty() || getSupportedPropertyTypes().contains(featureProperty.getType());

        if (!isTypeMatching) {
            LOGGER.warn("Skipping {} transformation for property '{}', type {} is not supported. Supported types: {}", getType(), featureProperty.getName(), featureProperty.getType(), getSupportedPropertyTypes());
        }
        //TODO: name really needed?
        return isNameMatching && isTypeMatching;
    }

}
