package de.ii.xtraplatform.feature.provider.sql.app;

import akka.stream.javadsl.Flow;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.FeatureEncoder;
import java.util.Optional;
import scala.concurrent.ExecutionContextExecutor;

public class FeatureEncoderSql implements FeatureEncoder<String, PropertySql, FeatureSql, SchemaSql> {

  private final FeatureMutationsSql featureMutationsSql;
  private final ExecutionContextExecutor dispatcher;
  private final Optional<String> id;

  public FeatureEncoderSql(
      FeatureMutationsSql featureMutationsSql,
      ExecutionContextExecutor dispatcher,
      Optional<String> id) {
    this.featureMutationsSql = featureMutationsSql;
    this.dispatcher = dispatcher;
    this.id = id;
  }

  //TODO: merge with FeatureMutationsSql
  @Override
  public Flow<FeatureSql, String, ?> flow(SchemaSql schema) {
    return id.isPresent()
        ? featureMutationsSql
        .getUpdaterFlow(schema, dispatcher, id.get())
        : featureMutationsSql.getCreatorFlow(schema, dispatcher);
  }
}
