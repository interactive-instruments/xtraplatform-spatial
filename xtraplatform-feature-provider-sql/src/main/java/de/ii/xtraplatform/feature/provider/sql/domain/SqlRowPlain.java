package de.ii.xtraplatform.feature.provider.sql.domain;

import org.immutables.value.Value;

@Value.Modifiable
public interface SqlRowPlain extends SqlRow {

    @Override
    default int compareTo(SqlRow sqlRow) {
        return 0;
    }
}
