package de.ii.xtraplatform.feature.provider.sql.app;

import akka.japi.Pair;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FeatureStoreInsertGenerator {
    Function<FeatureSql, Pair<String, Consumer<String>>> createInsert(
            SchemaSql schema, List<Integer> parentRows, boolean withId);

    Function<FeatureSql, Pair<String, Consumer<String>>> createJunctionInsert(
            SchemaSql schema, List<Integer> parentRows);

    Function<FeatureSql, Pair<String, Consumer<String>>> createForeignKeyUpdate(
            SchemaSql schema, List<Integer> parentRows);
}
