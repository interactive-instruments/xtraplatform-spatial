/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableSqlPathDefaults.Builder.class)
public interface SqlPathDefaults {

  @JsonAlias("defaultPrimaryKey")
  @Value.Default
  default String getPrimaryKey() {
    return "id";
  }

  @JsonAlias("defaultSortKey")
  @Value.Default
  default String getSortKey() {
    return "id";
  }

  Optional<String> getJunctionTablePattern();
}
