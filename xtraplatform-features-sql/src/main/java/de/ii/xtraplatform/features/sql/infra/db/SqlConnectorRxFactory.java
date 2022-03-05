/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableSet;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.base.domain.AppConfiguration;
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
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.setup.Environment;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class SqlConnectorRxFactory implements ConnectorFactory2<SqlRow, SqlQueries, SqlQueryOptions>,
    DropwizardPlugin {

  private final FactoryAssisted factoryAssisted;
  private final Map<String, SqlConnector> instances;
  private MetricRegistry metricRegistry;
  private HealthCheckRegistry healthCheckRegistry;

  @Inject
  public SqlConnectorRxFactory(
      AppContext appContext, //TODO: needed because dagger-auto does not parse SqlConnectorSlick
      FactoryAssisted factoryAssisted) {
    this.factoryAssisted = factoryAssisted;
    this.instances = new LinkedHashMap<>();
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    this.metricRegistry = environment.metrics();
    this.healthCheckRegistry = environment.healthChecks();
  }

  @Override
  public String type() {
    return FeatureProviderSql.PROVIDER_TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(SqlConnectorRx.CONNECTOR_TYPE);
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
    SqlConnector sqlConnector = factoryAssisted.create(metricRegistry, healthCheckRegistry, (FeatureProviderSqlData) data);
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
    SqlConnectorRx create(MetricRegistry metricRegistry, HealthCheckRegistry healthCheckRegistry, FeatureProviderSqlData data);
  }
}