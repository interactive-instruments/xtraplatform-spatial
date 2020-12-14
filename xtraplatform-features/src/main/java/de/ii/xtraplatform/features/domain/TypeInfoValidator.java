/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureProviderDataV2.VALIDATION;
import java.util.List;
import org.immutables.value.Value;

public interface TypeInfoValidator {

  default ValidationResult validate(FeatureStoreTypeInfo typeInfo, VALIDATION mode) {
    return typeInfo.getInstanceContainers().get(0).getAllAttributesContainers().stream()
        .map(attributesContainer -> validate(typeInfo.getName(), attributesContainer, mode))
        .reduce(ValidationResult.of(), ValidationResult::mergeWith);
  }

  ValidationResult validate(
      String typeName, FeatureStoreAttributesContainer attributesContainer, VALIDATION mode);

  @Value.Immutable
  interface ValidationResult {

    static ValidationResult of() {
      return ImmutableValidationResult.builder().mode(VALIDATION.NONE).build();
    }

    default ValidationResult mergeWith(ValidationResult other) {
      return ImmutableValidationResult.builder()
          .from(this)
          .mode(other.getMode())
          .addAllErrors(other.getErrors())
          .addAllStrictErrors(other.getStrictErrors())
          .addAllWarnings(other.getWarnings())
          .build();
    }

    VALIDATION getMode();

    List<String> getErrors();

    List<String> getStrictErrors();

    List<String> getWarnings();

    @Value.Derived
    default boolean isSuccess() {
      return getErrors().isEmpty() && (getMode() == VALIDATION.LAX || getStrictErrors().isEmpty());
    }
  }
}
