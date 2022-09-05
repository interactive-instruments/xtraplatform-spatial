/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.AggregateStatsReader;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.threeten.extra.Interval;

class AggregateStatsReaderSql implements AggregateStatsReader<SchemaSql> {

  private final Supplier<SqlClient> sqlClient;
  private final AggregateStatsQueryGenerator queryGenerator;
  private final SqlDialect sqlDialect;
  private final EpsgCrs crs;

  AggregateStatsReaderSql(
      Supplier<SqlClient> sqlClient,
      AggregateStatsQueryGenerator queryGenerator,
      SqlDialect sqlDialect,
      EpsgCrs crs) {
    this.sqlClient = sqlClient;
    this.queryGenerator = queryGenerator;
    this.sqlDialect = sqlDialect;
    this.crs = crs;
  }

  @Override
  public Stream<Long> getCount(List<SchemaSql> sourceSchemas) {
    return Reactive.Source.iterable(sourceSchemas)
        .via(
            Reactive.Transformer.flatMap(
                schemaSql ->
                    sqlClient
                        .get()
                        .getSourceStream(
                            queryGenerator.getCountQuery(schemaSql), SqlQueryOptions.single())))
        .via(Reactive.Transformer.map(sqlRow -> Long.parseLong((String) sqlRow.getValues().get(0))))
        .to(Reactive.Sink.reduce(0L, Long::sum));
  }

  @Override
  public Stream<Optional<BoundingBox>> getSpatialExtent(
      List<SchemaSql> sourceSchemas, boolean is3d) {
    return Source.iterable(sourceSchemas)
        .via(
            Transformer.flatMap(
                main -> {
                  Optional<SchemaSql> spatial = main.getPrimaryGeometryParent();

                  if (spatial.isEmpty()) {
                    return Source.empty();
                  }

                  return sqlClient
                      .get()
                      .getSourceStream(
                          queryGenerator.getSpatialExtentQuery(main, spatial.get(), is3d),
                          SqlQueryOptions.single());
                }))
        .via(
            Transformer.map(
                sqlRow -> sqlDialect.parseExtent((String) sqlRow.getValues().get(0), crs)))
        .to(
            Reactive.Sink.reduce(
                Optional.empty(),
                (prev, next) -> {
                  if (next.isEmpty()) {
                    return prev;
                  }
                  if (prev.isEmpty()) {
                    return next;
                  }

                  return Optional.of(BoundingBox.merge(prev.get(), next.get()));
                }));
  }

  @Override
  public Stream<Optional<Interval>> getTemporalExtent(List<SchemaSql> sourceSchemas) {
    return Source.iterable(sourceSchemas)
        .via(
            Transformer.flatMap(
                main -> {
                  if (main.getPrimaryInstantParent().isPresent()) {
                    return sqlClient
                        .get()
                        .getSourceStream(
                            queryGenerator.getTemporalExtentQuery(
                                main, main.getPrimaryInstantParent().get()),
                            SqlQueryOptions.tuple());
                  }

                  if (main.getPrimaryIntervalStartParent().isPresent()
                      && main.getPrimaryIntervalEndParent().isPresent()) {
                    return sqlClient
                        .get()
                        .getSourceStream(
                            queryGenerator.getTemporalExtentQuery(
                                main,
                                main.getPrimaryIntervalStartParent().get(),
                                main.getPrimaryIntervalEndParent().get()),
                            SqlQueryOptions.tuple());
                  }

                  return Source.empty();
                }))
        .via(
            Transformer.map(
                sqlRow ->
                    sqlDialect.parseTemporalExtent(
                        (String) sqlRow.getValues().get(0), (String) sqlRow.getValues().get(1))))
        .to(
            Reactive.Sink.reduce(
                Optional.empty(),
                (prev, next) -> {
                  if (next.isEmpty()) {
                    return prev;
                  }
                  if (prev.isEmpty()) {
                    return next;
                  }

                  return Optional.of(prev.get().span(next.get()));
                }));
  }
}
