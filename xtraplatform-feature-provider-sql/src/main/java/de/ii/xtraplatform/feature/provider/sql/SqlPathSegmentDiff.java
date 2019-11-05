package de.ii.xtraplatform.feature.provider.sql;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface SqlPathSegmentDiff {

    String getPath();

    String getTable();

    SqlPathTable.TYPE getType();

    List<SqlPathColumn> getColumns();

    Optional<String> getParentPath();

    List<SqlPathJoin> getJoins();
}
