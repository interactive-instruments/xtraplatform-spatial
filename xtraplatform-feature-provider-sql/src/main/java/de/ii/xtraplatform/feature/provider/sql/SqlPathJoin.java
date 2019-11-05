package de.ii.xtraplatform.feature.provider.sql;

import org.immutables.value.Value;

@Value.Immutable
public interface SqlPathJoin {

    String getSourceColumn();

    String getTargetColumn();

    String getTargetTable();
}
