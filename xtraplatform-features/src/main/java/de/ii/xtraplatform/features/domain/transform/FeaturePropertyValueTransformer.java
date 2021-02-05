/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.features.domain.FeatureProperty;
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
