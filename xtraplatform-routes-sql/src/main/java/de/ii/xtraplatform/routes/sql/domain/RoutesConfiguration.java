/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.routes.sql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langAll <code>
 * ```yaml
 * extensions:
 *   - type: ROUTES
 *     enabled: true
 *     warmup: true
 *     fromToQuery:
 *       driving: 'SELECT id as oid, ST_Distance(${point},the_geom) AS distance FROM v_ways_vertices_pgr WHERE connected and ST_DWithin(${point},the_geom, 0.25) ORDER BY distance ASC LIMIT 1'
 *     edgesQuery:
 *       driving: 'SELECT gid as id, source, target, x1, y1, x2, y2, ${cost_column} AS cost, ${reverse_cost_column} AS reverse_cost, maxheight, maxweight FROM v_ways WHERE tag_id NOT IN (113, 114, 115, 116, 117, 118, 119, 120, 122, 201, 202, 203, 204, 301, 302, 303, 304, 305)'
 *     routeQuery: 'SELECT * from pgr_aStar(${edgesQuery}, ${from_vid}, ${to_vid}, ${height}, ${weight}, ''SELECT gid FROM v_ways WHERE ST_Intersects(the_geom, ${obstacles})'', false, 5, (1+8+16))'
 *     nativeCrs:
 *       code: 4326
 *       forceAxisOrder: LON_LAT
 *     preferences:
 *       fastest:
 *         label: Fastest
 *         costColumn: cost_s
 *         reverseCostColumn: reverse_cost_s
 *       shortest:
 *         label: Shortest
 *         costColumn: cost
 *         reverseCostColumn: reverse_cost
 *     modes:
 *       driving: Driving
 * types:
 *   route:
 *     type: OBJECT
 *     sourcePath: /_route_
 *     properties:
 *       id:
 *         sourcePath: id
 *         type: INTEGER
 *         role: ID
 *       edge:
 *         sourcePath: edge
 *         type: INTEGER
 *       node:
 *         sourcePath: node
 *         type: INTEGER
 *       cost:
 *         sourcePath: cost
 *         type: FLOAT
 *       agg_cost:
 *         sourcePath: agg_cost
 *         type: FLOAT
 *       data:
 *         type: OBJECT
 *         sourcePath: '[edge=gid]v_ways{sortKey=gid}'
 *         properties:
 *           geometry:
 *             sourcePath: the_geom
 *             type: GEOMETRY
 *             geometryType: LINE_STRING
 *             role: PRIMARY_GEOMETRY
 *             forcePolygonCCW: false
 *           source:
 *             sourcePath: source
 *             type: INTEGER
 *           target:
 *             sourcePath: target
 *             type: INTEGER
 *           type:
 *             sourcePath: tag_id
 *             type: STRING
 *             transformations:
 *               codelist: wayTag
 *           #osm_id:
 *           #  sourcePath: osm_id
 *           #  type: INTEGER
 *           length_m:
 *             sourcePath: length_m
 *             type: FLOAT
 *           duration_forward_s:
 *             sourcePath: cost_s
 *             type: FLOAT
 *           duration_backward_s:
 *             sourcePath: reverse_cost_s
 *             type: FLOAT
 *           roadName:
 *             sourcePath: name
 *             type: STRING
 *           oneway:
 *             sourcePath: oneway
 *             type: STRING
 *           maxspeed_forward:
 *             sourcePath: maxspeed_forward
 *             type: FLOAT
 *           maxspeed_backward:
 *             sourcePath: maxspeed_backward
 *             type: FLOAT
 *           maxHeight_m:
 *             sourcePath: maxheight
 *             type: FLOAT
 *           maxWeight_t:
 *             sourcePath: maxweight
 *             type: FLOAT
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableRoutesConfiguration.Builder.class)
public interface RoutesConfiguration extends ExtensionConfiguration {

  /**
   * @langEn *Required* The SQL query to determine the start and end vertex of the route. `${point}`
   *     will be replaced with the WKT POINT geometry of the waypoint to use as start/end.
   * @langDe *Required* Die SQL-Abfrage zur Bestimmung des Start- und Endpunkts der Route.
   *     `${point}` wird durch die WKT-POINT-Geometrie des Wegpunktes ersetzt, der als Start/Ende
   *     verwendet werden soll.
   * @default null
   * @since v3.1
   */
  Map<String, String> getFromToQuery();

  /**
   * @langEn *Required* The SQL query to determine the edges / route segments of the route.
   *     `${cost_column}` and `${reverse_cost_column}` will be replaced with the column names to use
   *     for optimizing the route. `${cost_column}` is the value in the direction of the edge,
   *     `${reverse_cost_column}` in the opposite direction.
   * @langDe *Required* Die SQL-Abfrage zur Bestimmung der Kanten/Routensegmente der Route.
   *     `${cost_column}` und `${reverse_cost_column}` werden durch die Spaltennamen ersetzt, die
   *     für die Optimierung der Route verwendet werden sollen. Dabei ist `${cost_column}` der Wert
   *     in Richtung der Kante, `${reverse_cost_column}` in der entgegengesetzten Richtung.
   * @default null
   * @since v3.1
   */
  Map<String, String> getEdgesQuery();

  /**
   * @langEn *Required* The SQL query to compute the route. Use `${edgesQuery}` as placeholder for
   *     the `edgesQuery`, `${from_vid}` and `${to_vid}` as placeholders for the start/end vertex.
   *     If supported, use `${height}`, `${weight}`, and `${obstacles}` as placeholders for the
   *     height, weight and area constraints. `${flag_mask}`, if provided, is replaced by the sum of
   *     all additional flags set for the routing request.
   * @langDe *Required* Die SQL-Abfrage zur Berechnung der Route. Verwenden Sie `${edgesQuery}` als
   *     Platzhalter für die `edgesQuery`, `${from_vid}` und `${to_vid}` als Platzhalter für den
   *     Start/Endpunkt. Falls unterstützt, verwenden Sie `${height}`, `${weight}`, und
   *     `${obstacles}` als Platzhalter für die Einschränkungen bezüglich Höhe, Gewicht und Gebiet.
   *     `${flag_mask}`, falls angegeben, wird durch die Summe aller für die Routing-Anfrage
   *     gesetzten zusätzlichen Flags ersetzt.
   * @default null
   * @since v3.1
   */
  String getRouteQuery();

  /**
   * @langEn Additional flags that can be set to consider during the computation of the route. The
   *     integer value of a flag must be a unique bit value.
   * @langDe Zusätzliche Flags, die gesetzt werden können, um sie bei der Berechnung der Route zu
   *     berücksichtigen. Der ganzzahlige Wert eines Flags muss ein eindeutiger Bit-Wert sein.
   * @default {}
   * @since v3.1
   */
  // Not supported by standard pgRouting functions, ignore in documentation
  @DocIgnore
  Map<String, Integer> getFlags();

  /**
   * @langEn Coordinate reference system of geometries in the routing dataset. The EPSG code of the
   *     coordinate reference system is given as integer in `code`. `forceAxisOrder` may be set to
   *     use a non-default axis order: `LON_LAT` uses longitude/east as first value and
   *     latitude/north as second value, `LAT_LON` uses the reverse. `NONE` uses the default axis
   *     order and is the default value. Example: The default coordinate reference system `CRS84`
   *     would look like this: `code: 4326` and `forceAxisOrder: LON_LAT`.
   * @langDe Das Koordinatenreferenzsystem, in dem Geometrien in dem Routing-Datensatz geführt
   *     werden. Der EPSG-Code des Koordinatenreferenzsystems wird als Integer in `code` angegeben.
   *     Mit `forceAxisOrder` kann die Koordinatenreihenfolge geändert werden: `NONE` verwendet die
   *     Reihenfolge des Koordinatenreferenzsystems, `LON_LAT` verwendet stets Länge/Ostwert als
   *     ersten und Breite/Nordwert als zweiten Wert, `LAT_LON` entsprechend umgekehrt. Beispiel:
   *     Das Default-Koordinatenreferenzsystem `CRS84` entspricht `code: 4326` und `forceAxisOrder:
   *     LON_LAT`.
   * @default CRS84
   * @since v3.1
   */
  EpsgCrs getNativeCrs();

  /**
   * @langEn *Required* Lists the available options for optimizing the route. The key is the
   *     preference id, the value an object of key-value pairs with the following required keys:
   *     "label", "costColumn" (name of the column in the table of network segment to minimize, if
   *     the segment is traveled in positive direction), "reverseCostColumn" (same, but for
   *     travelling in negative direction).
   * @langDe *Required* Listet die verfügbaren Optionen zur Optimierung der Route auf. Der Schlüssel
   *     ist die Id, der Wert ein Objekt aus Key-Value-Paaren mit den folgenden erforderlichen Keys:
   *     "label", "costColumn" (Name der Spalte in der Tabelle des zu minimierenden Netzsegments,
   *     wenn das Segment in positiver Richtung befahren wird), "reverseCostColumn" (dasselbe, aber
   *     für die Fahrt in negativer Richtung).
   * @default {}
   * @since v3.1
   */
  Map<String, Preference> getPreferences();

  /**
   * @langEn Lists the available modes of transportation, the key is the mode id, the value a
   *     descriptive label.
   * @langDe Listet die verfügbaren Verkehrsmittel auf, der Schlüssel ist die Id, der Wert eine
   *     Bezeichnung.
   * @default {}
   * @since v3.1
   */
  Map<String, String> getModes();

  /**
   * @langEn Sets a default value for the weight of a vehicle in tons. `0` means no weight.
   * @langDe Legt einen Standardwert für das Gewicht des Verkehrsmittels in Tonnen fest. `0`
   *     bedeutet keine Gewichtsangabe.
   * @default 0
   * @since v3.1
   */
  @Value.Default
  default String getWeightDefault() {
    return "0";
  }

  /**
   * @langEn Sets a default value for the height of a vehicle in meters. `0` means no height.
   * @langDe Legt einen Standardwert für die Höhe des Verkehrsmittels in Metern fest. `0` bedeutet
   *     keine Höhenangabe.
   * @default 0
   * @since v3.1
   */
  @Value.Default
  default String getHeightDefault() {
    return "0";
  }

  /**
   * @langEn Sets a default value for an area to avoid.
   * @langDe Legt einen Standardwert für ein zu vermeidendes Gebiet fest.
   * @default EMPTY
   * @since v3.1
   */
  @Value.Default
  default String getObstaclesDefault() {
    return "ST_GeomFromText('GEOMETRYCOLLECTION EMPTY')";
  }

  /**
   * @langEn Enables a route calculation with the default values at provider startup. This can be
   *     useful, if the routing algorithm caches certain information.
   * @langDe Aktiviert eine Routenberechnung mit den Standardwerten beim Start des Providers. Dies
   *     kann sinnvoll sein, wenn der Routing-Algorithmus bestimmte Informationen in einem Cache
   *     speichert.
   * @default false
   * @since v3.1
   */
  @Nullable
  Boolean getWarmup();

  @Value.Lazy
  default boolean shouldWarmup() {
    return Objects.equals(getWarmup(), true);
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableRoutesConfiguration.Builder();
  }
}
