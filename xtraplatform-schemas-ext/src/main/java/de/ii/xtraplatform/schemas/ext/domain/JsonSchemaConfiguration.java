/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.schemas.ext.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import java.util.Map;
import org.immutables.value.Value;

/**
 * @langAll <code>
 * ```yaml
 * - type: JSON_SCHEMA
 *   enabled: true
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableJsonSchemaConfiguration.Builder.class)
public interface JsonSchemaConfiguration extends ExtensionConfiguration {

  Map<String, String> getObjectTypeRefs();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableJsonSchemaConfiguration.Builder();
  }
}
