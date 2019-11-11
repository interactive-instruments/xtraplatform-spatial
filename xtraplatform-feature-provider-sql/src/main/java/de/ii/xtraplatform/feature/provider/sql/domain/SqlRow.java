package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Optional;

public interface SqlRow extends Comparable<SqlRow> {

    Optional<SqlColumn> next();

    default List<String> getIds() {
        return Lists.newArrayList((String) null);
    }

    String getName();

    default List<String> getPath() {
        return ImmutableList.of();
    }

    class SqlColumn {
        private final List<String> path;
        private final String name;
        private final String value;

        SqlColumn(List<String> path, String name, String value) {
            this.path = path;
            this.name = name;
            this.value = value;
        }

        public List<String> getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
