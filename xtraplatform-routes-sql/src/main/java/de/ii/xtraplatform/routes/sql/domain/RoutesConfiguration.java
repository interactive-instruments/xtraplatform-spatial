/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.routes.sql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableRoutesConfiguration.Builder.class)
public interface RoutesConfiguration extends ExtensionConfiguration {

  String getFromToQuery();

  String getEdgesQuery();

  String getRouteQuery();

  Map<String, Integer> getFlags();

  EpsgCrs getNativeCrs();

  @Value.Lazy
  default boolean shouldWarmup() {
    return false;
  }

  abstract class Builder extends ExtensionConfiguration.Builder {
  }

  @Override
  default Builder getBuilder() {
    return new ImmutableRoutesConfiguration.Builder();
  }
}
