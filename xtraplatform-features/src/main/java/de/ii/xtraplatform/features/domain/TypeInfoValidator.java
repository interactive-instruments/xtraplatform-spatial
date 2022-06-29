/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;

public interface TypeInfoValidator {

  default ValidationResult validate(FeatureStoreTypeInfo typeInfo, MODE mode) {
    return typeInfo.getInstanceContainers().get(0).getAllAttributesContainers().stream()
        .map(attributesContainer -> validate(typeInfo.getName(), attributesContainer, mode))
        .reduce(ValidationResult.of(), ValidationResult::mergeWith);
  }

  ValidationResult validate(
      String typeName, FeatureStoreAttributesContainer attributesContainer, MODE mode);
}
