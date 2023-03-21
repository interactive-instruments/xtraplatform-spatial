/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = DocIgnore.class)
@BuildableMapEncodingEnabled
@JsonDeserialize(builder = ImmutablePartialObjectSchema.Builder.class)
public interface PartialObjectSchema
    extends PropertiesSchema<FeatureSchema, ImmutableFeatureSchema.Builder, PartialObjectSchema> {

  Optional<String> getSourcePath();

  Optional<String> getSchema();

  @Override
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getPropertyMap();

  abstract class Builder
      extends PropertiesSchema.Builder<
          FeatureSchema, ImmutableFeatureSchema.Builder, PartialObjectSchema> {}

  @Override
  default ImmutablePartialObjectSchema.Builder getBuilder() {
    return new ImmutablePartialObjectSchema.Builder().from(this);
  }
}
