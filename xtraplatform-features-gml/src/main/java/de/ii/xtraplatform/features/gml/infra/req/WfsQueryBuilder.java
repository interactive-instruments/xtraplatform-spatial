/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.infra.req;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.gml.infra.fes.FesFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WfsQueryBuilder {
  private final List<String> typeNames;
  private Optional<FesFilter> filter;
  private EpsgCrs crs;

  public WfsQueryBuilder() {
    this.typeNames = new ArrayList<>();
    this.filter = Optional.empty();
  }

  public WfsQueryBuilder typeName(String typeName) {
    if (!Objects.isNull(typeName)) {
      this.typeNames.add(typeName);
    }
    return this;
  }

  public WfsQueryBuilder filter(Optional<FesFilter> filter) {
    this.filter = filter;
    return this;
  }

  public WfsQueryBuilder crs(EpsgCrs crs) {
    this.crs = crs;
    return this;
  }

  public WfsQuery build() {
    final List<String> types = ImmutableList.copyOf(typeNames);
    final WfsQuery query = new WfsQuery(types, filter, crs);

    // WFSQuery2 has no getters, so we pass the members to validate
    validate(query, types);

    return query;
  }

  private void validate(final WfsQuery query, final List<String> types) {
    Objects.requireNonNull(query, "Query may not be null");
    Objects.requireNonNull(types, "At least on type name is required");
    if (types.isEmpty()) {
      throw new IllegalStateException("At least on type name is required");
    }
  }
}
