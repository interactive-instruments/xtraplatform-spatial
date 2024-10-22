/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import java.util.Map;
import java.util.Objects;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public interface FeaturePropertyTransformerMap extends FeaturePropertyValueTransformer {

  Logger LOGGER = LoggerFactory.getLogger(FeaturePropertyTransformerMap.class);
  String TYPE = "MAP";
  String MATCH_ALL = "*";

  @Override
  default String getType() {
    return TYPE;
  }

  Map<String, String> getMapping();

  @Override
  default String transform(String currentPropertyPath, String input) {

    String resolvedValue = getMapping().get(input);

    if (Objects.isNull(resolvedValue) && getMapping().containsKey(MATCH_ALL)) {
      resolvedValue = getMapping().get(MATCH_ALL);
    }

    return Objects.nonNull(resolvedValue) ? resolvedValue : input;
  }
}
