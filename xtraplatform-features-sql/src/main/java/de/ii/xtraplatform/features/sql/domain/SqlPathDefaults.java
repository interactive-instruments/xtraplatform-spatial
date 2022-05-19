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
 * # Source Path Syntax
 * @langEn The fundamental elements of the path syntax are demonstrated in the example above. The path to a property is
 * formed by concatenating the relative paths (`sourcePath`) with "/". A `sourcePath` has to be defined for the for
 * object that represents the feature type and most child objects.
 *
 * On the first level the path is formed by a "/" followed by the table name for the feature type. Every row in the
 * table corresponds to a feature. Example: `/kita`
 *
 * When defining a feature property on a deeper level using a column from the given table, the path equals the column
 * name, e.g. `name`. The full path will then be `/kita/name`.
 *
 * A join is defined using the pattern `[id=fk]tab`, where `id` is the primary key of the table from the parent object,
 * `fk` is the foreign key of the joining table and `tab` is the name of the joining table. Example from above:
 * `[oid=kita_fk]plaetze`. When a junction table should be used, two such joins are concatenated with "/", e.g. `[id=fka]a_2_b/[fkb=id]tab_b`.
 *
 * Rows for a table can be filtered by adding `{filter=expression}` after the table name, where `expression` is a
 * [CQL Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) expression. For details see the module [Filter / CQL](../services/filter.md), which provides the implementation but does not have to be enabled.
 *
 * To select capacity information only when the value is not NULL and greater than zero in the example above,
 * the filter would look like this: `[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}`
 *
 * A non-default sort key can be set by adding `{sortKey=columnName}` after the table name.
 * @langDe In dem Beispiel oben sind die wesentlichen Elemente der Pfadsyntax in der Datenbank bereits erkennbar.
 * Der Pfad zu einer Eigenschaft ergibt sich immer als Konkatenation der relativen Pfadangaben (`sourcePath`),
 * jeweils ergänzt um ein "/". Die Eigenschaft `sourcePath` ist beim ersten Objekt, das die Objektart repräsentiert,
 * angegeben und bei allen untergeordneten Schemaobjekten, außer es handelt sich um einen festen Wert.
 *
 * Auf der obersten Ebene entspricht der Pfad einem "/" gefolgt vom Namen der Tabelle zur Objektart. Jede Zeile in der
 * Tabelle entsprich einem Feature. Beispiel: `/kita`.
 *
 * Bei nachgeordneten relativen Pfadangaben zu einem Feld in derselben Tabelle wird einfach der Spaltenname angeben,
 * z.B. `name`. Daraus ergibt sich der Gesamtpfad `/kita/name`.
 *
 * Ein Join wird nach dem Muster `[id=fk]tab` angegeben, wobei `id` der Primärschlüssel der Tabelle aus dem übergeordneten
 * Schemaobjekt ist, `fk` der Fremdschlüssel aus der über den Join angebundenen Tabelle und `tab` der Tabellenname. Siehe
 * `[oid=kita_fk]plaetze` in dem Beispiel oben. Bei der Verwendung einer Zwischentabelle werden zwei dieser Joins
 * aneinandergehängt, z.B. `[id=fka]a_2_b/[fkb=id]tab_b`.
 *
 * Auf einer Tabelle (der Haupttabelle eines Features oder einer über Join-angebundenen Tabelle) kann zusätzlich ein
 * einschränkender Filter durch den Zusatz `{filter=ausdruck}` angegeben werden, wobei `ausdruck` das Selektionskriertium
 * in [CQL Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) spezifiziert. Für Details siehe das Modul
 * [Filter / CQL](../services/filter.md), welches die Implementierung bereitstellt, aber nicht aktiviert sein muss.
 *
 * Wenn z.B. in dem Beispiel oben nur Angaben zur Belegungskapazität selektiert werden sollen, deren Wert nicht NULL
 * und gleichzeitig größer als Null ist, dann könnte man schreiben: `[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}`.
 *
 * Ein vom Standard abweichender `sortKey` kann durch den Zusatz von `{sortKey=Spaltenname}` nach dem Tabellennamen angegeben werden.
 *
 * Ein vom Standard abweichender `primaryKey` kann durch den Zusatz von `{primaryKey=Spaltenname}` nach dem Tabellennamen angegeben werden.
 */

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
