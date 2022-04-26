/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * # Source Path Defaults
 * @langEn Defaults for the path expressions in `sourcePath`, also see [Source Path Syntax](#path-syntax).
 * @langDe Defaults für die Pfad-Ausdrücke in `sourcePath`, siehe auch [SQL-Pfad-Syntax](#path-syntax).
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableSqlPathDefaults.Builder.class)
public interface SqlPathDefaults {

  /**
   * @langEn The default column that is used for join analysis if no differing primary key is set in the [sourcePath](#path-syntax).
   * @langDe Die Standard-Spalte die zur Analyse von Joins verwendet wird, wenn keine abweichende Spalte in `sourcePath` gesetzt wird.
   * @default `id`
   */
  @JsonAlias("defaultPrimaryKey")
  @Value.Default
  default String getPrimaryKey() {
    return "id";
  }

  /**
   * @langEn The default column that is used to sort rows if no differing sort key is set in the [sourcePath](#path-syntax).
   * @langDe Die Standard-Spalte die zur Sortierung von Reihen verwendet wird, wenn keine abweichende Spalte in
   * `sourcePath` gesetzt wird. Es wird empfohlen, dass als Datentyp eine Ganzzahl verwendet wird.
   * @default `id`
   */
  @JsonAlias("defaultSortKey")
  @Value.Default
  default String getSortKey() {
    return "id";
  }

  Optional<String> getJunctionTablePattern();
}
