/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import java.util.List;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface SqlRowMeta extends SqlRow {

  @Nullable
  Object getMinKey();

  @Nullable
  Object getMaxKey();

  long getNumberReturned();

  OptionalLong getNumberMatched();

  List<Object> getCustomMinKeys();

  List<Object> getCustomMaxKeys();

  @Override
  default int compareTo(SqlRow row) {
    return -1;
  }
}
