/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.util.Objects;
import java.util.function.Function;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerStringFormat extends FeaturePropertyValueTransformer {

  String TYPE = "STRING_FORMAT";
  String DEFAULT_SUBSTITUTION_KEY = "value";

  @Override
  default String getType() {
    return TYPE;
  }

  Function<String, String> getSubstitutionLookup();

  @Override
  default String transform(String currentPropertyPath, String input) {
    Function<String, String> lookup =
        key -> {
          if (Objects.isNull(key)) {
            return null;
          }
          if (Objects.equals(key, DEFAULT_SUBSTITUTION_KEY)
              || Objects.equals(getPropertyPath(), key)) {
            return input;
          }

          String lookupWithKey = getSubstitutionLookup().apply(key);

          if (Objects.nonNull(lookupWithKey)) {
            return lookupWithKey;
          }

          return getSubstitutionLookup().apply(currentPropertyPath + "." + key);
        };

    return StringTemplateFilters.applyTemplate(getParameter(), lookup);
  }
}
