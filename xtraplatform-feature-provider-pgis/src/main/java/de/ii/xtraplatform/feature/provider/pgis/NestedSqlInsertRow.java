/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
class NestedSqlInsertRow {
    final String path;
    final Map<String, String> values;
    final ListMultimap<String, NestedSqlInsertRow> rows;
    final Map<String, String> ids;

    NestedSqlInsertRow(String path, ListMultimap<String, NestedSqlInsertRow> rows) {
        this(path, new LinkedHashMap<>(), rows, new LinkedHashMap<>());
    }

    NestedSqlInsertRow(String path, Map<String, String> values, ListMultimap<String, NestedSqlInsertRow> rows, Map<String, String> ids) {
        this.path = path;
        this.values = values;
        this.rows = rows;
        this.ids = ids;
    }

    NestedSqlInsertRow getNested(List<String> path, List<Integer> parentRows) {
        NestedSqlInsertRow nested = this;
        for (int i = 1; i < path.size(); i++) {
            List<NestedSqlInsertRow> nestedRows = nested.rows.get(path.get(i));
            if (parentRows.size() > i && parentRows.get(i) >= nestedRows.size()) {
                throw new IllegalStateException(String.format("No values found for row %s of %s", parentRows.get(i), path.get(i)));
            }
            nested = nestedRows.get(parentRows.size() < i + 1 ? 0 : parentRows.get(i));
        }
        return nested;
    }

    NestedSqlInsertRow getNested(String path) {
        if (path.equals(this.path)) {
            return this;
        }
        for (String p : rows.keySet()) {
            NestedSqlInsertRow lastNestedRow = rows.get(p)
                                                   .get(rows.get(p)
                                                    .size() - 1);
            String nestedPath = this.path + lastNestedRow.path;
            if (path.startsWith(nestedPath)) {
                return lastNestedRow
                        .getNested(path.substring(this.path.length()));
            }
        }
        return null;
    }

    void addValue(String path, String value) {
        NestedSqlInsertRow nested = getNested(path.substring(0, path.lastIndexOf("/")));
        if (nested != null) {
            nested.values.put(nested.path + path.substring(path.lastIndexOf("/")), value != null ? "'" + value.replaceAll("'", "''") + "'" : null);
        }
    }

    void addRow(String path) {
        String parentPath = path.substring(0, path.lastIndexOf("/"));
        NestedSqlInsertRow nestedParent = getNested(parentPath);
        if (nestedParent == null && parentPath.contains("/")) {
            parentPath = parentPath.substring(0, parentPath.lastIndexOf("/"));
            nestedParent = getNested(parentPath);
        }
        if (nestedParent != null) {
            String subPath = path.substring(parentPath.length());
            nestedParent.rows.put(subPath, new NestedSqlInsertRow(subPath, ArrayListMultimap.create()));
        }
    }
}
