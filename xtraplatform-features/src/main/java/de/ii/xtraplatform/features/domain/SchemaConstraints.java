/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * # Constraints
 *
 * @langEn Currently only available in [German](../../de/providers/details/constraints.md).
 * @langDe In der Konfiguration der Objektarten im Feature-Provider können Schema-Einschränkungen
 *     dokumentiert werden. Diese werden z.B. bei der Erzeugung von JSON-Schema-Dokumenten
 *     verwendet.
 *     <p>{@docTable:properties}
 *     <p>Als Beispiel hier die Eigenschaften der
 *     [Abschnitte/Äste-Features](https://demo.ldproxy.net/strassen/collections/abschnitteaeste/items)
 *     in der API [Straßennetz und Unfälle in NRW](https://demo.ldproxy.net/strassen) mit
 *     Constraints:
 *     <p><code>
 * ```yaml
 * types:
 *   abschnitteaeste:
 *     label: Abschnitte und Äste
 *     sourcePath: /abschnitteaeste
 *     type: OBJECT
 *     properties:
 *       kennung:
 *         label: Kennung
 *         description: 16-stellige Kennung des Abschnittes oder Astes
 *         sourcePath: abs
 *         type: STRING
 *         role: ID
 *         constraints:
 *           regex: '^[0-9]{7}[A-Z][0-9]{7}[A-Z]$'
 *       strasse:
 *         label: Straße
 *         type: OBJECT
 *         objectType: Strasse
 *         properties:
 *           bez:
 *             label: Straßenbezeichnung
 *             sourcePath: strbez
 *             type: STRING
 *           klasse:
 *             label: Straßenklasse
 *             sourcePath: strkl
 *             type: STRING
 *             constraints:
 *               enum:
 *               - A
 *               - B
 *               - L
 *               - K
 *           nummer:
 *             label: Straßennummer
 *             sourcePath: strnr
 *             type: INTEGER
 *             constraints:
 *               min: 1
 *               max: 9999
 *           zusatz:
 *             label: Buchstabenzusatz
 *             description: Buchstabenzusatz zur Straßennummer
 *             sourcePath: strzus
 *             type: STRING
 *             constraints:
 *               regex: '^[A-Z]$'
 *       ...
 *       absast:
 *         label: Art
 *         description: Art des Abschnittes oder Astes
 *         sourcePath: absast
 *         type: STRING
 *         constraints:
 *           enum:
 *           - Abschnitt
 *           - Ast
 *       ...
 *       laenge_m:
 *         label: Länge [m]
 *         description: Länge des Abschnittes oder Astes (m)
 *         sourcePath: laenge
 *         type: INTEGER
 *         constraints:
 *           min: 0
 * ```
 * </code>
 *     <p>
 * @ref:properties {@link de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints}
 */
@DocFile(
    path = "providers/details",
    name = "constraints.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:properties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    })
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableSchemaConstraints.Builder.class)
public interface SchemaConstraints {

  /**
   * @langEn TODO
   * @langDe Identifiziert eine [Codelist](../../codelists/README.md), die für die Eigenschaft gilt.
   *     Nur bei String- oder Integer-Eigenschaften sinnvoll.
   */
  Optional<String> getCodelist();

  /**
   * @langEn TODO
   * @langDe Liste von erlaubten Werten für die Eigenschaft. Nur bei String- oder
   *     Integer-Eigenschaften sinnvoll.
   */
  @JsonProperty(value = "enum")
  List<String> getEnumValues();

  /**
   * @langEn TODO
   * @langDe Ein regulärer Ausdruck, der von allen Werten erfüllt werden muss. Nur bei
   *     String-Eigenschaften sinnvoll.
   */
  Optional<String> getRegex();

  /**
   * @langEn TODO
   * @langDe Eine Eigenschaft kann als Pflichteigenschaft, die in allen Instanzen gesetzt sein muss,
   *     qualifiziert werden.
   */
  Optional<Boolean> getRequired();

  /**
   * @langEn TODO
   * @langDe Mindestwert für alle Instanzen. Nur bei numerischen Eigenschaften sinnvoll.
   */
  Optional<Double> getMin();

  /**
   * @langEn TODO
   * @langDe Maximalwert für alle Instanzen. Nur bei numerischen Eigenschaften sinnvoll.
   */
  Optional<Double> getMax();

  /**
   * @langEn TODO
   * @langDe Mindestanzahl von Werten für alle Instanzen. Nur bei Array-Eigenschaften sinnvoll.
   */
  Optional<Integer> getMinOccurrence();

  /**
   * @langEn TODO
   * @langDe Maximalanzahl von Werten für alle Instanzen. Nur bei Array-Eigenschaften sinnvoll.
   */
  Optional<Integer> getMaxOccurrence();

  /**
   * @langEn Flag to indicate that all geometry components are connected. Only relevant for
   *     properties with MultiLineString and MultiPolygon geometries.
   * @langDe Indikator, dass alle Einzelgeometrien zusammenhängen. Nur bei MultiLineString- und
   *     MultiPolygon-Eigenschaften sinnvoll.
   */
  Optional<Boolean> getComposite();

  /**
   * @langEn Flag to indicate that all geometry values are closed. Only relevant for geometry
   *     properties.
   * @langDe Indikator, dass die Geometrie geschlossen ist. Nur bei Geometrie-Eigenschaften
   *     sinnvoll.
   */
  Optional<Boolean> getClosed();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isRequired() {
    return getRequired().filter(required -> Objects.equals(required, true)).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isComposite() {
    return getComposite().filter(composite -> Objects.equals(composite, true)).isPresent();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isClosed() {
    return getClosed().filter(closed -> Objects.equals(closed, true)).isPresent();
  }
}
