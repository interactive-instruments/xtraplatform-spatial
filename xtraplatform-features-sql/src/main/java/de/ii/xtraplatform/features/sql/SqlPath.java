/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface SqlPath {

  @Nullable
  SqlPath getParent();

  String getTablePath();

  List<String> getColumns();

  @Value.Default
  default boolean isRoot() {
    return false;
  }

  @Value.Default
  default boolean isJunction() {
    return false;
  }

  @Value.Default
  default boolean hasOid() {
    return false;
  }

  OptionalInt getSortPriority();

  String getQueryable();

  @Value.Default
  default boolean isSpatial() {
    return false;
  }

  @Value.Default
  default boolean isTemporal() {
    return false;
  }

  Optional<String> getConstantValue();

  Map<String, String> getTableFlags();

  @Value.Derived
  default String getTablePathWithFlags() {
    String tablePathWithFlags = getTablePath();

    for (Entry<String, String> entry : getTableFlags().entrySet()) {
      String table = entry.getKey();
      String flags = entry.getValue();

      tablePathWithFlags =
          tablePathWithFlags.replaceFirst("(\\/|])(" + table + ")(\\/|$)", "$1$2" + flags + "$3");
    }

    return tablePathWithFlags;
  }
}
