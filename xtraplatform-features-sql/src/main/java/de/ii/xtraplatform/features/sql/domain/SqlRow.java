/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.google.common.collect.ImmutableList;
import java.util.List;

public interface SqlRow extends Comparable<SqlRow> {

  List<Object> getValues();

  default String getName() {
    return "unknown";
  }

  default List<String> getPath() {
    return ImmutableList.of();
  }

  default List<Comparable<?>> getIds() {
    return ImmutableList.of();
  }

  default List<Comparable<?>> getSortKeys() {
    return getIds();
  }

  default List<String> getSortKeyNames() {
    return ImmutableList.of();
  }

  default int getPriority() {
    return 0;
  }

  default List<List<String>> getColumnPaths() {
    return ImmutableList.of();
  }

  default List<Boolean> getSpatialAttributes() {
    return ImmutableList.of();
  }

  default List<Boolean> getTemporalAttributes() {
    return ImmutableList.of();
  }

  @Override
  default int compareTo(SqlRow sqlRow) {
    return 0;
  }
  /*Optional<SqlColumn> next();

  default List<String> getIds() {
      return Lists.newArrayList((String) null);
  }

  String getName();

  default List<String> getPath() {
      return ImmutableList.of();
  }*/

  class SqlColumn {
    private final List<String> path;
    private final String value;

    SqlColumn(List<String> path, String value) {
      this.path = path;
      this.value = value;
    }

    public List<String> getPath() {
      return path;
    }

    public String getValue() {
      return value;
    }
  }
}
