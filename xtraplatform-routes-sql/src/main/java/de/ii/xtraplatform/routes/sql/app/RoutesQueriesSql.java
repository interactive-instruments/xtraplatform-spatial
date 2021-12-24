/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.routes.sql.app;

import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.Geometry.Point;
import de.ii.xtraplatform.crs.domain.CoordinateTuple;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlQueryOptions.Builder;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueriesExtension;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.routes.sql.domain.RouteQuery;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class RoutesQueriesSql implements FeatureQueriesExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoutesQueriesSql.class);

  private final CrsTransformerFactory crsTransformerFactory;

  public RoutesQueriesSql(@Requires CrsTransformerFactory crsTransformerFactory) {
    this.crsTransformerFactory = crsTransformerFactory;
  }

  @Override
  public boolean isSupported(FeatureProviderConnector<?, ?, ?> connector) {
    return connector instanceof SqlConnector;
  }

  @Override
  public void on(LIFECYCLE_HOOK hook, FeatureProviderDataV2 data,
      FeatureProviderConnector<?, ?, ?> connector) {
    Optional<RoutesConfiguration> routesConfiguration = getRoutesConfiguration(data);

    if (routesConfiguration.isPresent()) {
      SqlClient sqlClient = ((SqlConnector) connector).getSqlClient();

      switch (hook) {
        case STARTED:
          if (routesConfiguration.get().shouldWarmup()) {
            LOGGER.debug("Warming up routes queries for {}", data.getId());
            List<String> queries = getWarmupSelects(routesConfiguration.get());
            AtomicInteger completed = new AtomicInteger();
            queries.forEach(query -> sqlClient.run(query, new Builder().build()).whenComplete((r,t) -> {
              completed.getAndIncrement();
              if (completed.get()==queries.size())
                LOGGER.debug("Routes queries for {} are warmed up", data.getId());
            }));
          }
          break;
      }
    }
  }

  @Override
  public void on(QUERY_HOOK hook, FeatureProviderDataV2 data, FeatureProviderConnector<?, ?, ?> connector,
      FeatureQuery query,
      BiConsumer<String, String> aliasResolver) {
    Optional<RoutesConfiguration> routesConfiguration = getRoutesConfiguration(data);
    Optional<RouteQuery> routeQuery = getRouteQuery(query);

    if (routesConfiguration.isPresent() && routeQuery.isPresent()) {
      SqlClient sqlClient = ((SqlConnector) connector).getSqlClient();

      switch (hook) {
        case BEFORE:
          String tableName = createRouteTable(routesConfiguration.get(), routeQuery.get(),
              sqlClient);
          aliasResolver.accept("_route_", tableName);
          break;
        case AFTER:
          deleteRouteTable(routesConfiguration.get(), routeQuery.get(), sqlClient);
          break;
      }
    }
  }

  private String createRouteTable(RoutesConfiguration routesConfiguration, RouteQuery routeQuery,
      SqlClient sqlClient) {
    String tableName = getTempTableName(routeQuery);

    createTempTable(sqlClient, tableName);

    getRouteSelects(routesConfiguration, routeQuery)
        .forEach(select -> insertIntoTempTable(sqlClient, tableName, select));

    return tableName;
  }

  private void deleteRouteTable(RoutesConfiguration routesConfiguration, RouteQuery routeQuery,
      SqlClient sqlClient) {
    String tableName = getTempTableName(routeQuery);

    deleteTempTable(sqlClient, tableName);
  }

  private void createTempTable(SqlClient sqlClient, String name) {
    String query = String.format(
        "DROP TABLE IF EXISTS %1$s; CREATE TABLE %1$s (id serial, seq int, path_seq int, node bigint, edge bigint, cost float, agg_cost float)",
        name);

    sqlClient.run(query, new Builder().build())
        .join();
  }

  private void insertIntoTempTable(SqlClient sqlClient, String name, String select) {
    String query = String.format(
        "INSERT INTO %s(seq, path_seq, node, edge, cost, agg_cost) (%s)",
        name, select);

    sqlClient.run(query, new Builder().build())
        .whenComplete((result, throwable) -> {
          if (Objects.nonNull(throwable)) {
            try {
              LOGGER.debug("Inserting into temp table failed, dropping it ({})", throwable.getMessage());
              deleteTempTable(sqlClient, name);
            } catch (Throwable t) {
              //ignore
            }
          }
        })
        .join();
  }

  private void deleteTempTable(SqlClient sqlClient, String name) {
    String query = String.format("DROP TABLE IF EXISTS %s", name);

    sqlClient.run(query, new Builder().build())
        .join();
  }

  private String getTempTableName(RouteQuery query) {
    return String.format("r_%d", query.hashCode()).replaceAll("-", "_");
  }

  private List<String> getRouteSelects(
      RoutesConfiguration cfg,
      RouteQuery query) {

    return getSegments(query, cfg.getNativeCrs()).stream()
        .map(segment -> getRouteSelect(cfg, segment.first(), segment.second(),
                                       query.getCostColumn(), query.getReverseCostColumn(),
                                       query.getMode(), query.getFlags(),
                                       query.getWeight(), query.getHeight(), query.getObstacles()))
        .collect(Collectors.toList());
  }

  private String getRouteSelect(RoutesConfiguration cfg,
                                Point start, Point end,
                                String costColumn, String reverseCostColumn,
                                String mode, List<String> flags,
                                Optional<Double> weight, Optional<Double> height,
                                Optional<Geometry.MultiPolygon> obstacles) {

    int mask = 0;
    for (String flag: flags) {
      if (cfg.getFlags().containsKey(flag)) {
        mask |= cfg.getFlags().get(flag);
      }
    }

    String fromToQuery = Objects.requireNonNull(cfg.getFromToQuery().get(mode),"Invalid Route Provider configuration. Mode '"+mode+"' is supported by the API configuration, but not the 'fromToQuery' in the provider.");
    String edgesQuery = Objects.requireNonNull(cfg.getEdgesQuery().get(mode),"Invalid Route Provider configuration. Mode '"+mode+"' is supported by the API configuration, but not the 'edgesQuery' in the provider.");

    String select = cfg.getRouteQuery()
        .replace("${edgesQuery}", "'" +  edgesQuery.replaceAll("'", "''") + "'")
        .replace("${from_vid}", "(SELECT oid FROM start_pnt LIMIT 1)")
        .replace("${to_vid}", "(SELECT oid FROM end_pnt LIMIT 1)")
        .replace("${flag_mask}", String.valueOf(mask))
        .replace("${cost_column}", costColumn)
        .replace("${reverse_cost_column}", reverseCostColumn)
        // TODO best is rwl-specific, move to an option or adjust the route query
        .replace("${best ? 1 : -1}", flags.contains("best") ? "1" : "-1");

    if (weight.isPresent()) {
      select = select
          .replace("${weight}", String.valueOf(weight.get()));
    } else {
      select = select
          .replace("${weight}", cfg.getWeightDefault().replaceAll("'", "''") );
    }

    if (height.isPresent()) {
      select = select
          .replace("${height}", String.valueOf(height.get()));
    } else {
      select = select
          .replace("${height}", cfg.getHeightDefault().replaceAll("'", "''") );
    }

    if (obstacles.isPresent()) {
      select = select
          .replace("${obstacles}","ST_GeomFromText(''"+getWkt(obstacles.get())+"'',"+obstacles.get().getCrs().orElse(OgcCrs.CRS84).getCode()+")");
    } else {
      select = select
          .replace("${obstacles}", cfg.getObstaclesDefault().replaceAll("'", "''") );
    }

    //TODO: if cfg.getNativeCrs() is not set use provider crs

    return "WITH\n"
        + " pnts AS (SELECT ST_GeomFromText('POINT(" + start.getCoordinates().get(0).get(0) + " " + start.getCoordinates().get(0).get(1) + ")', " + cfg.getNativeCrs().getCode() + ") AS pnt1,\n"
        + "                 ST_GeomFromText('POINT(" + end.getCoordinates().get(0).get(0) + " " + end.getCoordinates().get(0).get(1) + ")', " + cfg.getNativeCrs().getCode() + ") AS pnt2),\n"
        + " start_pnt AS (" + fromToQuery.replaceAll("\\$\\{point\\}", "(SELECT pnt1 FROM pnts)") + "),\n"
        + " end_pnt AS (" + fromToQuery.replaceAll("\\$\\{point\\}", "(SELECT pnt2 FROM pnts)") + ")\n"
        + select + " AS segment WHERE edge != -1";
  }

  private List<Tuple<Point, Point>> getSegments(RouteQuery query,
      EpsgCrs nativeCrs) {
    List<Tuple<Point, Point>> segments = new ArrayList<>();

    Point last = withCrs(query.getStart(), nativeCrs);

    for (Point wayPoint : query.getWayPoints()) {
      Point current = withCrs(wayPoint, nativeCrs);
      segments.add(Tuple.of(last, current));
      last = current;
    }

    segments.add(Tuple.of(last, withCrs(query.getEnd(), nativeCrs)));

    return segments;
  }

  private Point withCrs(Point point, EpsgCrs targetCrs) {
    EpsgCrs sourceCrs = point.getCrs().orElse(OgcCrs.CRS84);
    if (!Objects.equals(sourceCrs, targetCrs)) {
      Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(sourceCrs,
          targetCrs);
      if (transformer.isPresent()) {
        CoordinateTuple coordinateTuple = transformer.get()
            .transform(point.getCoordinates().get(0).get(0), point.getCoordinates().get(0).get(1));
        return Point.of(coordinateTuple.getX(), coordinateTuple.getY(), targetCrs);
      }
    }
    return point;
  }

  private List<String> getWarmupSelects(RoutesConfiguration cfg) {
    return cfg.getEdgesQuery()
        .values()
        .stream()
        .map(edgesQuery ->
                 cfg.getPreferences()
                     .values()
                     .stream()
                     .map(preference -> cfg.getRouteQuery()
                         .replace("${edgesQuery}", "'" +  edgesQuery.replaceAll("'", "''") + "'")
                         .replace("${from_vid}", "0")
                         .replace("${to_vid}", "0")
                         .replace("${flag_mask}", "0")
                         .replace("${cost_column}", preference.getCostColumn())
                         .replace("${reverse_cost_column}", preference.getReverseCostColumn())
                         .replace("${height}", cfg.getHeightDefault())
                         .replace("${weight}", cfg.getWeightDefault())
                         .replace("${obstacles}", cfg.getObstaclesDefault())
                         .replace("${best ? 1 : -1}", "-1"))
                     .collect(Collectors.toUnmodifiableList()))
        .flatMap(List::stream)
        .collect(Collectors.toUnmodifiableList());
  }

  private Optional<RouteQuery> getRouteQuery(FeatureQuery query) {
    if (Objects.isNull(query)) {
      return Optional.empty();
    }
    return query.getExtensions().stream()
        .filter(extension -> extension instanceof RouteQuery)
        .map(extension -> (RouteQuery) extension)
        .findFirst();
  }

  private Optional<RoutesConfiguration> getRoutesConfiguration(FeatureProviderDataV2 data) {
    return data.getExtensions().stream()
        .filter(extension -> extension.isEnabled() && extension instanceof RoutesConfiguration)
        .map(extension -> (RoutesConfiguration) extension)
        .findFirst();
  }

  private String getWkt(Geometry.MultiPolygon multiPolygon) {
    return "MULTIPOLYGON("+multiPolygon.getCoordinates().stream()
        .map(this::getPolygon)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.joining(","))+")";
  }

  private Optional<String> getPolygon(Geometry.Polygon polygon) {
    List<Optional<String>> rings = polygon.getCoordinates().stream()
        .map(this::getRing)
        .collect(Collectors.toUnmodifiableList());
    if (rings.get(0).isEmpty())
      return Optional.empty();
    return Optional.of("("+rings.stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.joining(","))+")");
  }

  private Optional<String> getRing(List<Geometry.Coordinate> ring) {
    if (ring.size() < 4)
      return Optional.empty();
    return Optional.of("("+ring.stream().map(this::getPos).collect(Collectors.joining(","))+")");
  }

  private String getPos(Geometry.Coordinate pos) {
    return pos.stream().map(String::valueOf).collect(Collectors.joining(" "));
  }

}
