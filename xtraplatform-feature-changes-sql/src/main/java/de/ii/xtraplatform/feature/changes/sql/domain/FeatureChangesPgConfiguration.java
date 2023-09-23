/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.changes.sql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import io.dropwizard.util.Duration;
import java.util.List;
import org.immutables.value.Value;

/**
 * @type FEATURE_CHANGES_PG
 * @langAll <code>
 * ```yaml
 * - type: FEATURE_CHANGES_PG
 *   enabled: true
 *   listenForTypes: # optional, default is to listen for all types
 *   - governmentalservice
 *   pollingInterval: 30s # optional, default is 60s
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "FEATURE_CHANGES_PG")
@JsonDeserialize(builder = ImmutableFeatureChangesPgConfiguration.Builder.class)
public interface FeatureChangesPgConfiguration extends ExtensionConfiguration {

  /**
   * @langEn List of types that should be observed. An empty list means all types.
   * @langDe Liste der Typen, die überwacht werden sollen. Eine leere Liste bedeutet alle Typen.
   * @default []
   * @since v3.3
   */
  List<String> getListenForTypes();

  /**
   * @langEn Since the JDBC driver for PostgreSQL does not support real reactivity, it has to be
   *     polled for changes with the given interval.
   * @langDe Da der JDBC-Treiber für PostgreSQL keine richtige Reaktivität unterstützt, müssen
   *     Änderungen mit dem angegebenen Intervall abgefragt werden.
   * @default 60s
   * @since v3.3
   */
  @Value.Default
  default Duration getPollingInterval() {
    return Duration.seconds(60);
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableFeatureChangesPgConfiguration.Builder();
  }
}
