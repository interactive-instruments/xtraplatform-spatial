/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.pool.ProxyConnection;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.app.FeatureSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.sql.domain.SqlDialectGpkg;
import de.ii.xtraplatform.features.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlRow;
import de.ii.xtraplatform.streams.domain.Reactive;
import hu.akarnokd.rxjava3.bridge.RxJavaBridge;
import io.reactivex.Flowable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.Tx;
import org.davidmoten.rx.jdbc.internal.DelegatedConnection;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlClientRx implements SqlClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlClientRx.class);

  /* NOTE: If the db uses e.g. the DE collation and some sort key actually contains e.g. umlauts
           this might lead to wrong results.
           To cover such cases, the locale would need to be configurable.
  */
  private static final Collator COLLATOR_DEFAULT = Collator.getInstance(Locale.US);

  private final Database session;
  private final SqlDialect dialect;
  private final Collator collator;

  public SqlClientRx(Database session, Dialect dialect) {
    this.session = session;
    this.dialect = dialect == Dialect.GPKG ? new SqlDialectGpkg() : new SqlDialectPostGis();
    this.collator = dialect == Dialect.PGIS ? COLLATOR_DEFAULT : null;
  }

  @Override
  public CompletableFuture<Collection<SqlRow>> run(String query, SqlQueryOptions options) {
    if (LOGGER.isDebugEnabled(MARKER.SQL)) {
      LOGGER.debug(MARKER.SQL, "Executing statement: {}", query);
    }
    CompletableFuture<Collection<SqlRow>> result = new CompletableFuture<>();

    if (options.getColumnTypes().isEmpty()) {
      session
          .update(query)
          .complete()
          .subscribe(() -> result.complete(ImmutableList.of()), result::completeExceptionally);

      return result;
    }

    session
        .select(query)
        .get(resultSet -> new SqlRowVals(collator).read(resultSet, options))
        .toList()
        .subscribe(result::complete, result::completeExceptionally);

    return result;
  }

  @Override
  public Reactive.Source<SqlRow> getSourceStream(String query, SqlQueryOptions options) {
    if (LOGGER.isDebugEnabled(MARKER.SQL)) {
      LOGGER.debug(MARKER.SQL, "Executing statement: {}", query);
    }
    List<SqlRow> logBuffer = new ArrayList<>(5);

    Flowable<SqlRow> flowable =
        session
            .select(query)
            .get(
                resultSet -> {
                  SqlRow row = new SqlRowVals(collator).read(resultSet, options);

                  if (LOGGER.isDebugEnabled(MARKER.SQL_RESULT) && logBuffer.size() < 5) {
                    logBuffer.add(row);
                  }

                  return row;
                });

    // TODO: prettify, see
    // https://github.com/slick/slick/blob/main/slick/src/main/scala/slick/jdbc/StatementInvoker.scala
    if (LOGGER.isDebugEnabled(MARKER.SQL_RESULT)) {
      flowable =
          flowable.doOnComplete(
              () -> {
                LOGGER.debug(MARKER.SQL, "Executed statement: {}", query);
                for (int i = 0; i < logBuffer.size(); i++) {
                  if (i == 0) {
                    String columns =
                        Stream.concat(
                                logBuffer.get(i).getSortKeyNames().stream(),
                                logBuffer.get(i).getColumnPaths().stream()
                                    .map(path -> path.get(path.size() - 1)))
                            .collect(Collectors.joining(" | "));
                    LOGGER.debug(MARKER.SQL_RESULT, columns);
                  }
                  String values =
                      Stream.concat(
                              logBuffer.get(i).getSortKeys().stream()
                                  .map(val -> Objects.nonNull(val) ? val.toString() : "null"),
                              logBuffer.get(i).getValues().stream()
                                  .map(val -> Objects.nonNull(val) ? val.toString() : "null"))
                          .collect(Collectors.joining(" | "));
                  LOGGER.debug(MARKER.SQL_RESULT, values);
                }
              });
    }

    return Reactive.Source.publisher(RxJavaBridge.toV3Flowable(flowable));
  }

  @Override
  public Reactive.Source<String> getMutationSource(
      FeatureSql feature,
      List<Function<FeatureSql, String>> toStatements,
      List<Consumer<String>> idConsumers,
      Object executionContext) {
    List<Function<FeatureSql, String>> toStatementsWithLog =
        toStatements.stream()
            .map(
                function ->
                    (Function<FeatureSql, String>)
                        featureSql -> {
                          String statement = function.apply(featureSql);

                          if (LOGGER.isDebugEnabled(MARKER.SQL)) {
                            LOGGER.debug(MARKER.SQL, "Executing statement: {}", statement);
                          }

                          return statement;
                        })
            .collect(Collectors.toList());

    int[] i = {0};
    BiFunction<ResultSet, String, String> mapper =
        (slickRow, previousId) -> {
          LOGGER.debug("QUERY {}", i[0]);
          // null not allowed as return value
          String id = null;
          try {
            id = slickRow.getString(1);
            LOGGER.debug("RETURNED {}", id);
            idConsumers.get(i[0]).accept(id);
            // LOGGER.debug("VALUES {}", values);
            LOGGER.debug("");
          } catch (SQLException e) {
            e.printStackTrace();
          }
          i[0]++;

          return previousId != null ? previousId : id;
        };

    String first = toStatementsWithLog.get(0).apply(feature);

    Flowable<Tx<String>> txFlowable =
        session
            .update(first)
            .transacted()
            .returnGeneratedKeys()
            .get(resultSet -> mapper.apply(resultSet, null))
            .filter(tx -> !tx.isComplete());

    for (int j = 1; j < toStatementsWithLog.size(); j++) {
      String next = toStatementsWithLog.get(j).apply(feature);

      txFlowable =
          txFlowable.flatMap(
              tx ->
                  tx.update(next)
                      .returnGeneratedKeys()
                      .get(resultSet -> mapper.apply(resultSet, tx.value()))
                      .filter(tx2 -> !tx2.isComplete()));
    }

    Flowable<String> flowable = txFlowable.map(Tx::value);

    return Reactive.Source.publisher(RxJavaBridge.toV3Flowable(flowable));
  }

  @Override
  public Reactive.Transformer<FeatureSql, String> getMutationFlow(
      Function<FeatureSql, List<Function<FeatureSql, Tuple<String, Consumer<String>>>>> mutations,
      Object executionContext,
      Optional<String> id) {

    Reactive.Transformer<FeatureSql, String> toQueries =
        Reactive.Transformer.flatMap(
            feature -> {
              List<Function<FeatureSql, Tuple<String, Consumer<String>>>> m =
                  mutations.apply(feature);

              List<Function<FeatureSql, String>> toStatements =
                  m.stream()
                      .map(
                          queryFunction ->
                              Objects.isNull(queryFunction.apply(feature).first())
                                  ? null
                                  : (Function<FeatureSql, String>)
                                      feature2 -> queryFunction.apply(feature2).first())
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());

              List<Consumer<String>> idConsumers =
                  m.stream()
                      .map(
                          queryFunction -> {
                            // TODO
                            Tuple<String, Consumer<String>> query = queryFunction.apply(feature);
                            return query.second();
                          })
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());

              return getMutationSource(feature, toStatements, idConsumers, executionContext);
            });

    if (id.isPresent()) {
      // TODO: check that feature id equals given id
      Reactive.Transformer<FeatureSql, FeatureSql> filter =
          Reactive.Transformer.filter(featureSql -> true);

      return filter.via(toQueries);
    }

    return toQueries;
  }

  @Override
  public Connection getConnection() {
    return session.connection().blockingGet();
  }

  @Override
  public SqlDialect getSqlDialect() {
    return dialect;
  }

  @Override
  public List<String> getNotifications(Connection connection) {
    Connection actualConnection = connection;

    if (actualConnection instanceof DelegatedConnection) {
      actualConnection = ((DelegatedConnection) actualConnection).con();
    }
    if (actualConnection instanceof ProxyConnection) {
      try {
        actualConnection = actualConnection.unwrap(Connection.class);
      } catch (SQLException e) {
        // ignore
      }
    }

    if (actualConnection instanceof PGConnection) {
      try {
        return Arrays.stream(((PGConnection) actualConnection).getNotifications())
            .map(PGNotification::getParameter)
            .collect(Collectors.toList());
      } catch (SQLException e) {
        // ignore
      }
    }
    return ImmutableList.of();
  }
}
