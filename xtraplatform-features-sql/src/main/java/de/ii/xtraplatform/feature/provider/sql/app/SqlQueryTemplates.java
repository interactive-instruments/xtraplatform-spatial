package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.features.domain.Tuple;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface SqlQueryTemplates {

  MetaQueryTemplate getMetaQueryTemplate();

  List<ValueQueryTemplate> getValueQueryTemplates();

  List<SchemaSql> getQuerySchemas();

  @FunctionalInterface
  interface MetaQueryTemplate {
    String generateMetaQuery(long limit, long offset, List<SortKey> additionalSortKeys, Optional<CqlFilter> filter);
  }

  @FunctionalInterface
  interface ValueQueryTemplate {
    String generateValueQuery(long limit, long offset, List<SortKey> additionalSortKeys, Optional<CqlFilter> filter, Optional<Tuple<Object, Object>> minMaxKeys);
  }
}
