/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

public interface TypeQuery {

  String getType();

  Optional<Cql2Expression> getFilter();

  List<SortKey> getSortKeys();

  @Value.Default
  default List<String> getFields() {
    return ImmutableList.of("*");
  }

  @Value.Default
  default boolean skipGeometry() {
    return false;
  }
}
