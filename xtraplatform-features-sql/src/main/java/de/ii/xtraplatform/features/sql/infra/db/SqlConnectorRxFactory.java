package de.ii.xtraplatform.features.sql.infra.db;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableSet;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.features.domain.ConnectorFactory2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.sql.domain.SqlQueries;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlRow;
import de.ii.xtraplatform.web.domain.Dropwizard;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class SqlConnectorRxFactory implements ConnectorFactory2<SqlRow, SqlQueries, SqlQueryOptions> {

  private final FactoryAssisted factoryAssisted;
  private final Map<String, SqlConnector> instances;

  @Inject
  public SqlConnectorRxFactory(
      AppContext appContext, Dropwizard dropwizard, //TODO: needed because dagger-auto does not parse SqlConnectorSlick
      FactoryAssisted factoryAssisted) {
    this.factoryAssisted = factoryAssisted;
    this.instances = new LinkedHashMap<>();
  }

  @Override
  public String type() {
    return FeatureProviderSql.PROVIDER_TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(SqlConnectorSlick.CONNECTOR_TYPE);
  }

  @Override
  public Optional<FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions>> instance(
      String id) {
    return Optional.ofNullable(instances.get(id));
  }

  @Override
  public Set<FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions>> instances() {
    return ImmutableSet.copyOf(instances.values());
  }

  @Override
  public FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions> createInstance(
      FeatureProviderDataV2 data) {
    SqlConnector sqlConnector = factoryAssisted.create((FeatureProviderSqlData) data);
    sqlConnector.start();
    instances.put(sqlConnector.getProviderId(), sqlConnector);

    return sqlConnector;
  }

  @Override
  public void deleteInstance(String id) {
    instance(id).ifPresent(FeatureProviderConnector::stop);
    instances.remove(id);
  }

  @AssistedFactory
  public interface FactoryAssisted {
    SqlConnectorSlick create(FeatureProviderSqlData data);
  }
}
