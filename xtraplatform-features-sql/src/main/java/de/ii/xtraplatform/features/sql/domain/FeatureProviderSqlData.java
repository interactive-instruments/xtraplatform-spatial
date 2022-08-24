/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.DocMarker;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * # SQL Features
 *
 * @langEn The specifics of the SQL feature provider.
 * @langDe Hier werden die Besonderheiten des SQL-Feature-Providers beschrieben.
 * @langAll {@docTable:properties}
 * @langAll ## Connection Info
 * @langEn The connection info object for SQL databases has the following properties:
 * @langDe Das Connection-Info-Objekt für SQL-Datenbanken wird wie folgt beschrieben:
 * @langAll {@docTable:connectionInfo}
 * @langAll ### Pool
 * @langEn Settings for the connection pool.
 * @langDe Einstellungen für den Connection-Pool.
 * @langAll {@docTable:pool}
 * @langEn ## Source Path Defaults
 *     <p>Defaults for the path expressions in `sourcePath`, also see [Source Path
 *     Syntax](#path-syntax).
 * @langDe ## SQL-Pfad-Defaults
 *     <p>Defaults für die Pfad-Ausdrücke in `sourcePath`, siehe auch
 *     [SQL-Pfad-Syntax](#path-syntax).
 * @langAll {@docTable:sourcePathDefaults}
 * @langEn ## Source Path Syntax
 *     <p>The fundamental elements of the path syntax are demonstrated in the example above. The
 *     path to a property is formed by concatenating the relative paths (`sourcePath`) with "/". A
 *     `sourcePath` has to be defined for the for object that represents the feature type and most
 *     child objects.
 *     <p>On the first level the path is formed by a "/" followed by the table name for the feature
 *     type. Every row in the table corresponds to a feature. Example: `/kita`
 *     <p>When defining a feature property on a deeper level using a column from the given table,
 *     the path equals the column name, e.g. `name`. The full path will then be `/kita/name`.
 *     <p>A join is defined using the pattern `[id=fk]tab`, where `id` is the primary key of the
 *     table from the parent object, `fk` is the foreign key of the joining table and `tab` is the
 *     name of the joining table. Example from above: `[oid=kita_fk]plaetze`. When a junction table
 *     should be used, two such joins are concatenated with "/", e.g. `[id=fka]a_2_b/[fkb=id]tab_b`.
 *     <p>Rows for a table can be filtered by adding `{filter=expression}` after the table name,
 *     where `expression` is a [CQL
 *     Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) expression. For details see
 *     the module [Filter / CQL](../services/building-blocks/filter.md), which provides the
 *     implementation but does not have to be enabled.
 *     <p>To select capacity information only when the value is not NULL and greater than zero in
 *     the example above, the filter would look like this: `[oid=kita_fk]plaetze{filter=anzahl IS
 *     NOT NULL AND anzahl>0}`
 *     <p>A non-default sort key can be set by adding `{sortKey=columnName}` after the table name.
 * @langDe ## SQL-Pfad-Syntax
 *     <p>In dem Beispiel oben sind die wesentlichen Elemente der Pfadsyntax in der Datenbank
 *     bereits erkennbar. Der Pfad zu einer Eigenschaft ergibt sich immer als Konkatenation der
 *     relativen Pfadangaben (`sourcePath`), jeweils ergänzt um ein "/". Die Eigenschaft
 *     `sourcePath` ist beim ersten Objekt, das die Objektart repräsentiert, angegeben und bei allen
 *     untergeordneten Schemaobjekten, außer es handelt sich um einen festen Wert.
 *     <p>Auf der obersten Ebene entspricht der Pfad einem "/" gefolgt vom Namen der Tabelle zur
 *     Objektart. Jede Zeile in der Tabelle entsprich einem Feature. Beispiel: `/kita`.
 *     <p>Bei nachgeordneten relativen Pfadangaben zu einem Feld in derselben Tabelle wird einfach
 *     der Spaltenname angeben, z.B. `name`. Daraus ergibt sich der Gesamtpfad `/kita/name`.
 *     <p>Ein Join wird nach dem Muster `[id=fk]tab` angegeben, wobei `id` der Primärschlüssel der
 *     Tabelle aus dem übergeordneten Schemaobjekt ist, `fk` der Fremdschlüssel aus der über den
 *     Join angebundenen Tabelle und `tab` der Tabellenname. Siehe `[oid=kita_fk]plaetze` in dem
 *     Beispiel oben. Bei der Verwendung einer Zwischentabelle werden zwei dieser Joins
 *     aneinandergehängt, z.B. `[id=fka]a_2_b/[fkb=id]tab_b`.
 *     <p>Auf einer Tabelle (der Haupttabelle eines Features oder einer über Join-angebundenen
 *     Tabelle) kann zusätzlich ein einschränkender Filter durch den Zusatz `{filter=ausdruck}`
 *     angegeben werden, wobei `ausdruck` das Selektionskriertium in [CQL
 *     Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) spezifiziert. Für Details
 *     siehe das Modul [Filter / CQL](../services/building-blocks/filter.md), welches die
 *     Implementierung bereitstellt, aber nicht aktiviert sein muss.
 *     <p>Wenn z.B. in dem Beispiel oben nur Angaben zur Belegungskapazität selektiert werden
 *     sollen, deren Wert nicht NULL und gleichzeitig größer als Null ist, dann könnte man
 *     schreiben: `[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}`.
 *     <p>Ein vom Standard abweichender `sortKey` kann durch den Zusatz von `{sortKey=Spaltenname}`
 *     nach dem Tabellennamen angegeben werden.
 *     <p>Ein vom Standard abweichender `primaryKey` kann durch den Zusatz von
 *     `{primaryKey=Spaltenname}` nach dem Tabellennamen angegeben werden.
 * @langEn ## Query Generation
 *     <p>Options for query generation.
 * @langDe ## Query-Generierung
 *     <p>Optionen für die Query-Generierung in `queryGeneration`.
 * @langAll {@docTable:queryGeneration}
 * @ref:properties {@link de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData}
 * @ref:connectionInfo {@link de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql}
 * @ref:pool {@link de.ii.xtraplatform.features.sql.domain.ImmutablePoolSettings}
 * @ref:sourcePathDefaults {@link de.ii.xtraplatform.features.sql.domain.ImmutableSqlPathDefaults}
 * @ref:queryGeneration {@link
 *     de.ii.xtraplatform.features.sql.domain.ImmutableQueryGeneratorSettings}
 */
@DocFile(
    path = "providers",
    name = "sql.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:properties}"),
            @DocStep(type = Step.JSON_PROPERTIES),
            @DocStep(type = Step.MARKED, params = "specific")
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "connectionInfo",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:connectionInfo}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "pool",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:pool}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "sourcePathDefaults",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:sourcePathDefaults}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "queryGeneration",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:queryGeneration}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    })
@Value.Immutable
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = DocIgnore.class)
@JsonDeserialize(builder = ImmutableFeatureProviderSqlData.Builder.class)
public interface FeatureProviderSqlData
    extends FeatureProviderDataV2, WithConnectionInfo<ConnectionInfoSql> {

  /**
   * @langEn See [Connection Info](#connection-info).
   * @langDe Siehe [Connection-Info](#connection-info).
   */
  @DocMarker("specific")
  @Nullable
  @Override
  ConnectionInfoSql getConnectionInfo();

  /**
   * @langEn Defaults for the path expressions in `sourcePath`, for details see [Source Path
   *     Defaults](#source-path-defaults) below.
   * @langDe Defaults für die Pfad-Ausdrücke in `sourcePath`, für Details siehe
   *     [SQL-Pfad-Defaults](#source-path-defaults).
   */
  // @JsonProperty(value = "sourcePathDefaults", access = JsonProperty.Access.WRITE_ONLY) // means
  // only read from json
  // @Value.Default
  // can't use interface, bug in immutables when using attributeBuilderDetection and Default
  // default SqlPathDefaults getSourcePathDefaults() {
  //    return new ImmutableSqlPathDefaults.Builder().build();
  // }
  @DocMarker("specific")
  @Nullable
  SqlPathDefaults getSourcePathDefaults();

  /**
   * @langEn Options for query generation, for details see [Query Generation](#query-generation)
   *     below.
   * @langDe Einstellungen für die Query-Generierung, für Details siehe
   *     [Query-Generierung](#query-generation).
   */
  @DocMarker("specific")
  @JsonProperty(
      value = "queryGeneration",
      access = JsonProperty.Access.WRITE_ONLY) // means only read from json
  // @Value.Default
  // can't use interface, bug in immutables when using attributeBuilderDetection and Default
  /*default QueryGeneratorSettings getQueryGeneration() {
    ImmutableQueryGeneratorSettings.Builder builder = new ImmutableQueryGeneratorSettings.Builder();

    getConnectionInfo().getComputeNumberMatched().ifPresent(builder::computeNumberMatched);

    return builder.build();
  }*/
  @Nullable
  QueryGeneratorSettings getQueryGeneration();

  // for json ordering
  @Override
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getTypes();

  @Value.Check
  default FeatureProviderSqlData initNestedDefault() {
    /*
     workaround for https://github.com/interactive-instruments/ldproxy/issues/225
     TODO: remove when fixed
    */
    if (Objects.isNull(getConnectionInfo())) {
      ImmutableFeatureProviderSqlData.Builder builder =
          new ImmutableFeatureProviderSqlData.Builder().from(this);
      builder.connectionInfoBuilder().database("");

      return builder.build();
    }

    /*
     - apply defaults for sourcePathDefaults and queryGeneration if necessary (cannot be set via @Value.Default due to a bug in immutables)
     - migrate from old syntax for sourcePathDefaults and queryGeneration in connectionInfo
    */
    boolean queryGenerationIsNull = Objects.isNull(getQueryGeneration());
    boolean computeNumberMatchedDiffers =
        !queryGenerationIsNull
            && getConnectionInfo().getComputeNumberMatched().isPresent()
            && !Objects.equals(
                getConnectionInfo().getComputeNumberMatched().get(),
                getQueryGeneration().getComputeNumberMatched());
    boolean sourcePathDefaultsIsNull = Objects.isNull(getSourcePathDefaults());
    boolean sourcePathDefaultsDiffers =
        !sourcePathDefaultsIsNull
            && getConnectionInfo().getPathSyntax().isPresent()
            && !Objects.equals(getConnectionInfo().getPathSyntax().get(), getSourcePathDefaults());

    if ((queryGenerationIsNull || computeNumberMatchedDiffers)
        || sourcePathDefaultsIsNull
        || sourcePathDefaultsDiffers) {
      ImmutableFeatureProviderSqlData.Builder builder =
          new ImmutableFeatureProviderSqlData.Builder().from(this);
      ImmutableConnectionInfoSql.Builder connectionInfoBuilder = builder.connectionInfoBuilder();

      if (queryGenerationIsNull || computeNumberMatchedDiffers) {
        ImmutableQueryGeneratorSettings.Builder queryGenerationBuilder =
            builder.queryGenerationBuilder();
        getConnectionInfo()
            .getComputeNumberMatched()
            .ifPresent(
                computeNumberMatched -> {
                  queryGenerationBuilder.computeNumberMatched(computeNumberMatched);
                  connectionInfoBuilder.computeNumberMatched(Optional.empty());
                });
      }
      if (sourcePathDefaultsIsNull || sourcePathDefaultsDiffers) {
        ImmutableSqlPathDefaults.Builder sourcePathDefaultsBuilder =
            builder.sourcePathDefaultsBuilder();
        getConnectionInfo()
            .getPathSyntax()
            .ifPresent(
                pathSyntax -> {
                  if (!Objects.equals(pathSyntax.getSortKey(), "id")) {
                    sourcePathDefaultsBuilder.sortKey(pathSyntax.getSortKey());
                  }
                  if (!Objects.equals(pathSyntax.getPrimaryKey(), "id")) {
                    sourcePathDefaultsBuilder.primaryKey(pathSyntax.getPrimaryKey());
                  }
                  if (pathSyntax.getJunctionTablePattern().isPresent()) {
                    sourcePathDefaultsBuilder.junctionTablePattern(
                        pathSyntax.getJunctionTablePattern());
                  }
                  connectionInfoBuilder.pathSyntax(Optional.empty());
                });
      }

      return builder.build();
    }

    return this;
  }

  @Value.Check
  default FeatureProviderSqlData mergeExtensions() {
    List<ExtensionConfiguration> distinctExtensions = getMergedExtensions();

    // remove duplicates
    if (getExtensions().size() > distinctExtensions.size()) {
      return new ImmutableFeatureProviderSqlData.Builder()
          .from(this)
          .extensions(distinctExtensions)
          .build();
    }

    return this;
  }

  @Value.Check
  default FeatureProviderSqlData applyLabelTemplate() {
    if (getLabelTemplate().isPresent()) {

      Map<String, FeatureSchema> types =
          getTypes().entrySet().stream()
              .map(
                  entry ->
                      new SimpleImmutableEntry<>(
                          entry.getKey(),
                          entry
                              .getValue()
                              .accept(
                                  (SchemaVisitorTopDown<FeatureSchema, FeatureSchema>)
                                      (schema, parents, visitedProperties) -> {
                                        ImmutableFeatureSchema.Builder builder =
                                            new ImmutableFeatureSchema.Builder().from(schema);
                                        visitedProperties.forEach(
                                            prop -> builder.putPropertyMap(prop.getName(), prop));
                                        if (schema.getLabel().isPresent()) {
                                          Map<String, String> lookup = new HashMap<>();
                                          schema
                                              .getLabel()
                                              .ifPresent(label -> lookup.put("value", label));
                                          schema
                                              .getUnit()
                                              .ifPresent(unit -> lookup.put("unit", unit));

                                          builder.label(
                                              StringTemplateFilters.applyTemplate(
                                                  getLabelTemplate().get(), lookup::get));
                                        }

                                        return builder.build();
                                      })))
              .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

      return new ImmutableFeatureProviderSqlData.Builder()
          .from(this)
          .types(types)
          .labelTemplate(Optional.empty())
          .build();
    }

    return this;
  }

  abstract class Builder
      extends FeatureProviderDataV2.Builder<ImmutableFeatureProviderSqlData.Builder>
      implements EntityDataBuilder<FeatureProviderDataV2> {

    public abstract ImmutableFeatureProviderSqlData.Builder connectionInfo(
        ConnectionInfoSql connectionInfo);

    @Override
    public ImmutableFeatureProviderSqlData.Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .featureProviderType(EntityDataDefaults.PLACEHOLDER)
          .connectionInfo(
              new ImmutableConnectionInfoSql.Builder()
                  .database(EntityDataDefaults.PLACEHOLDER)
                  .build());
    }
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableQueryGeneratorSettings.Builder.class)
  interface QueryGeneratorSettings {

    /**
     * @langEn Option to disable computation of the number of selected features for performance
     *     reasons that are returned in `numberMatched`. As a general rule this should be disabled
     *     for big datasets.
     * @langDe Steuert, ob bei Abfragen die Anzahl der selektierten Features berechnet und in
     *     `numberMatched` zurückgegeben werden soll oder ob dies aus Performancegründen
     *     unterbleiben soll. Bei großen Datensätzen empfiehlt es sich in der Regel, die Option zu
     *     deaktivieren.
     * @default true
     */
    @Value.Default
    default int getChunkSize() {
      return 1000;
    }

    @Value.Default
    default boolean getComputeNumberMatched() {
      return true;
    }

    // TODO
    @DocIgnore
    @Value.Default
    default Optional<String> getAccentiCollation() {
      return Optional.empty();
    }
  }
}
