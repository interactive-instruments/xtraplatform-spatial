/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.google.common.collect.ImmutableList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RowCursor {

  private final Map<List<String>, List<Integer>> rows;

  public RowCursor(List<String> path) {
    this.rows = new LinkedHashMap<>();
    rows.put(path, ImmutableList.of(0));
  }

  public List<Integer> track(List<String> path, List<String> parentPath, Integer currentRow) {
    List<Integer> parentRows = rows.get(parentPath);
    List<Integer> newParentRows =
        ImmutableList.<Integer>builder().addAll(parentRows).add(currentRow).build();
    rows.put(path, newParentRows);

    return newParentRows;
  }

  public List<Integer> get(List<String> path) {
    return rows.get(path);
  }

  public int getCurrent(List<String> path) {
    return rows.get(path).get(rows.get(path).size() - 1);
  }
}
