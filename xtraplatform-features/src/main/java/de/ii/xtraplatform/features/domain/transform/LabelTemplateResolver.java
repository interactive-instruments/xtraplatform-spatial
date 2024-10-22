/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.TypesResolver;
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LabelTemplateResolver implements TypesResolver {

  private final Optional<String> labelTemplate;

  public LabelTemplateResolver(Optional<String> labelTemplate) {
    this.labelTemplate = labelTemplate;
  }

  @Override
  public boolean needsResolving(Map<String, FeatureSchema> types) {
    return labelTemplate.isPresent() && !"{{value}}".equals(labelTemplate.get());
  }

  @Override
  public boolean needsResolving(FeatureSchema property, boolean isFeature) {
    return needsResolving(Map.of());
  }

  @Override
  public FeatureSchema resolve(FeatureSchema property, List<FeatureSchema> parents) {
    Builder builder = new Builder().from(property);

    Map<String, String> lookup = new HashMap<>();
    lookup.put("value", property.getLabel().orElse(property.getName()));
    property.getUnit().ifPresent(unit -> lookup.put("unit", unit));

    builder.label(StringTemplateFilters.applyTemplate(labelTemplate.get(), lookup::get));

    return builder.build();
  }
}
