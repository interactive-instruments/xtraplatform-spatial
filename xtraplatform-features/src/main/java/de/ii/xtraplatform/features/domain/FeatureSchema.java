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
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.entities.domain.maptobuilder.Buildable;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableMap;
import de.ii.xtraplatform.entities.domain.maptobuilder.encoding.BuildableMapEncodingEnabled;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = DocIgnore.class)
@BuildableMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureSchema.Builder.class)
@JsonPropertyOrder({
  "sourcePath",
  "sourcePaths",
  "type",
  "role",
  "valueType",
  "geometryType",
  "objectType",
  "label",
  "description",
  "unit",
  "scope",
  "excludedScopes",
  "transformations",
  "constraints",
  "properties"
})
public interface FeatureSchema
    extends FeatureSchemaBase<FeatureSchema>,
        Buildable<FeatureSchema>,
        PropertiesSchema<FeatureSchema, ImmutableFeatureSchema.Builder, FeatureSchema> {

  Logger LOGGER = LoggerFactory.getLogger(FeatureSchema.class);

  @JsonIgnore
  @Override
  String getName();

  @JsonIgnore
  @Override
  List<String> getPath();

  @JsonIgnore
  @Override
  List<String> getParentPath();

  /**
   * @langEn The relative path for this schema object. The syntax depends on the provider types, see
   *     [SQL](10-sql.md#path-syntax) or [WFS](50-wfs.md#path-syntax).
   * @langDe Der relative Pfad zu diesem Schemaobjekt. Die Pfadsyntax ist je nach Provider-Typ
   *     unterschiedlich ([SQL](10-sql.md#path-syntax) und [WFS](50-wfs.md#path-syntax)).
   */
  @Override
  Optional<String> getSourcePath();

  /**
   * @langEn Data type of the schema object. Default is `OBJECT` when `properties` is set, otherwise
   *     it is `STRING`. Possible values:
   *     <p><code>
   * - `FLOAT`, `INTEGER`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE` for simple values.
   * - `GEOMETRY` for geometries.
   * - `OBJECT` for objects.
   * - `OBJECT_ARRAY` a list of objects.
   * - `VALUE_ARRAY` a list of simple values.
   * </code>
   *     <p>
   * @langDe Der Datentyp des Schemaobjekts. Der Standardwert ist `STRING`, sofern nicht auch die
   *     Eigenschaft `properties` angegeben ist, dann ist es `OBJECT`. Erlaubt sind:
   *     <p><code>
   * - `FLOAT`, `INTEGER`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE` für einfache Werte.
   * - `GEOMETRY` für eine Geometrie.
   * - `OBJECT` für ein Objekt.
   * - `OBJECT_ARRAY` für eine Liste von Objekten.
   * - `VALUE_ARRAY`für eine Liste von einfachen Werten.
   * </code>
   *     <p>
   * @default STRING/OBJECT
   */
  @Nullable
  @JsonProperty("type")
  Type getDesiredType();

  @Value.Derived
  @JsonIgnore
  @Override
  default Type getType() {
    return Objects.requireNonNullElse(
        getDesiredType(), getPropertyMap().isEmpty() ? Type.STRING : Type.OBJECT);
  }

  /**
   * @langEn Indicates special meanings of the property. `ID` is to be specified at the property of
   *     an object to be used for the `featureId` in the API. This property is typically the first
   *     property in the `properties` object. Allowed characters in these properties are all
   *     characters except the space character (" ") and the horizontal bar ("/"). `TYPE` can be
   *     specified at the property of an object that contains the name of a subobject type. If an
   *     object type has multiple geometry properties, then specify `PRIMARY_GEOMETRY` at the
   *     property to be used for `bbox` queries and to be encoded in data formats with exactly one
   *     or a singled out geometry (e.g. in GeoJSON `geometry`). If an object type has multiple
   *     temporal properties, then `PRIMARY_INSTANT` should be specified at the property to be used
   *     for `datetime` queries, provided that a time instant describes the temporal extent of the
   *     features. If, on the other hand, the temporal extent is a time interval, then
   *     `PRIMARY_INTERVAL_START` and `PRIMARY_INTERVAL_END` should be specified at the respective
   *     temporal properties.
   * @langDe Kennzeichnet besondere Bedeutungen der Eigenschaft. `ID` ist bei der Eigenschaft eines
   *     Objekts anzugeben, die für die `featureId` in der API zu verwenden ist. Diese Eigenschaft
   *     ist typischerweise die erste Eigenschaft im `properties`-Objekt. Erlaubte Zeichen in diesen
   *     Eigenschaften sind alle Zeichen bis auf das Leerzeichen (" ") und der Querstrich ("/").
   *     `TYPE` kann bei der Eigenschaft eines Objekts angegeben werden, die den Namen einer
   *     Unterobjektart enthält. Hat eine Objektart mehrere Geometrieeigenschaften, dann ist
   *     `PRIMARY_GEOMETRY` bei der Eigenschaft anzugeben, die für `bbox`-Abfragen verwendet werden
   *     soll und die in Datenformaten mit genau einer oder einer herausgehobenen Geometrie (z.B. in
   *     GeoJSON `geometry`) kodiert werden soll. Hat eine Objektart mehrere zeitliche
   *     Eigenschaften, dann sollte `PRIMARY_INSTANT` bei der Eigenschaft angegeben werden, die für
   *     `datetime`-Abfragen verwendet werden soll, sofern ein Zeitpunkt die zeitliche Ausdehnung
   *     der Features beschreibt. Ist die zeitliche Ausdehnung hingegen ein Zeitintervall, dann sind
   *     `PRIMARY_INTERVAL_START` und `PRIMARY_INTERVAL_END` bei den jeweiligen zeitlichen
   *     Eigenschaften anzugeben.
   * @default null
   */
  @Override
  Optional<Role> getRole();

  /**
   * @langEn Only needed when `type` is `VALUE_ARRAY`. Possible values: `FLOAT`, `INTEGER`,
   *     `STRING`, `BOOLEAN`, `DATETIME`, `DATE`
   * @langDe Wird nur benötigt, wenn `type` auf `VALUE_ARRAY` gesetzt ist. Mögliche Werte: `FLOAT`,
   *     `INTEGER`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE`
   * @default STRING
   */
  @Override
  Optional<Type> getValueType();

  /**
   * @langEn The specific geometry type for properties with `type: GEOMETRY`. Possible values are
   *     simple feature geometry types: `POINT`, `MULTI_POINT`, `LINE_STRING`, `MULTI_LINE_STRING`,
   *     `POLYGON`, `MULTI_POLYGON`, `GEOMETRY_COLLECTION` and `ANY`
   * @langDe Mit der Angabe kann der Geometrietype spezifiziert werden. Die Angabe ist nur bei
   *     Geometrieeigenschaften (`type: GEOMETRY`) relevant. Erlaubt sind die
   *     Simple-Feature-Geometrietypen, d.h. `POINT`, `MULTI_POINT`, `LINE_STRING`,
   *     `MULTI_LINE_STRING`, `POLYGON`, `MULTI_POLYGON`, `GEOMETRY_COLLECTION` und `ANY`.
   * @default null
   */
  @Override
  Optional<SimpleFeatureGeometry> getGeometryType();

  /**
   * @langEn Optional name for an object type, used for example in JSON Schema. For properties that
   *     should be mapped as links according to *RFC 8288*, use `Link`.
   * @langDe Optional kann ein Name für den Typ spezifiziert werden. Der Name hat i.d.R. nur
   *     informativen Charakter und wird z.B. bei der Erzeugung von JSON-Schemas verwendet. Bei
   *     Eigenschaften, die als Web-Links nach RFC 8288 abgebildet werden sollen, ist immer "Link"
   *     anzugeben.
   * @default
   */
  Optional<String> getObjectType();

  /**
   * @langEn Label for the schema object, used for example in HTML representations.
   * @langDe Eine Bezeichnung des Schemaobjekts, z.B. für die Angabe in der HTML-Ausgabe.
   */
  Optional<String> getLabel();

  /**
   * @langEn Description for the schema object, used for example in HTML representations or JSON
   *     Schema.
   * @langDe Eine Beschreibung des Schemaobjekts, z.B. für die HTML-Ausgabe oder das JSON-Schema.
   */
  Optional<String> getDescription();

  /**
   * @langEn The unit of measurement of the value, only relevant for numeric properties.
   * @langDe Die Maßeinheit des Wertes, nur relevant bei numerischen Eigenschaften.
   */
  Optional<String> getUnit();

  /**
   * @langEn Might be used instead of `sourcePath` to define a property with a constant value.
   * @langDe Alternativ zu `sourcePath` kann diese Eigenschaft verwendet werden, um im
   *     Feature-Provider eine Eigenschaft mit einem festen Wert zu belegen.
   * @default `null`
   */
  Optional<String> getConstantValue();

  /**
   * @langEn Optional exclusion of a property from a schema scope. See [Schema
   *     Scopes](../details/scopes.md) for a description of the scopes.
   * @langDe Optionaler Ausschluss einer Eigenschaft aus einem Schema-Anwendungsbereich. Siehe
   *     [Schema-Anwendungsbereiche](../details/scopes.md) für eine Beschreibung der Bereiche.
   * @default []
   */
  @Override
  Set<Scope> getExcludedScopes();

  @Override
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean queryable() {
    return !isObject()
        && !isMultiSource()
        && !Objects.equals(getType(), Type.UNKNOWN)
        && !getExcludedScopes().contains(Scope.QUERYABLE);
  }

  @Override
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean sortable() {
    return !isSpatial()
        && !isObject()
        && !isArray()
        && !isMultiSource()
        && !Objects.equals(getType(), Type.BOOLEAN)
        && !Objects.equals(getType(), Type.UNKNOWN)
        && !getExcludedScopes().contains(Scope.SORTABLE);
  }

  // returnable() is unchanged, no need to override

  @Override
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean receivable() {
    return !isConstant() && !getExcludedScopes().contains(Scope.RECEIVABLE);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isMultiSource() {
    return !getConcat().isEmpty() || !getCoalesce().isEmpty();
  }

  /**
   * @langEn Reference to an external schema definition. The default resolver will resolve
   *     references to entries in `fragments` e.g. `#/fragments/example`. For additional resolvers
   *     see [Extensions](extensions).
   * @langDe Referenz auf eine externe Schema-Definition. Der Default-Resolver löst Referenzen auf
   *     Einträge in `fragments` auf, z.B. `#/fragments/example`. Für weitere Resolver siehe
   *     [Erweiterungen](extensions).
   * @default null
   */
  Optional<String> getSchema();

  /**
   * @langEn Option to completely ignore this schema object. Main purpose is to ignore parts of
   *     schemas referenced with `schema`.
   * @langDe Option um dieses Schemaobjekt komplett zu ignorieren. Der Hauptzweck ist es Teile von
   *     Schemas zu ignorieren, die mit `schema` referenziert werden.
   * @default false
   */
  @JsonProperty(value = "ignore", access = Access.WRITE_ONLY)
  @Value.Default
  default boolean getIgnore() {
    return false;
  }

  /**
   * @langEn Optional transformations for the property, see
   *     [transformations](../details/transformations.md).
   * @langDe Optionale Transformationen für die Eigenschaft, siehe
   *     [Transformationen](../details/transformations.md).
   * @default []
   */
  List<PropertyTransformation> getTransformations();

  /**
   * @langEn Optional description of schema constraints, especially for JSON schema generation. See
   *     [Constraints](../details/constraints.md).
   * @langDe Optionale Beschreibung von Schema-Einschränkungen, vor allem für die Erzeugung von
   *     JSON-Schemas. Siehe [Constraints](../details/constraints.md).
   * @default `{}`
   */
  @Override
  Optional<SchemaConstraints> getConstraints();

  /**
   * @langEn Option to disable enforcement of counter-clockwise orientation for exterior rings and a
   *     clockwise orientation for interior rings (only for SQL).
   * @langDe Option zum Erzwingen der Orientierung von Polygonen, gegen den Uhrzeigersinn für äußere
   *     Ringe und mit dem Uhrzeigersinn für innere Ringe (nur für SQL).
   * @default `true`
   */
  @Override
  Optional<Boolean> getForcePolygonCCW();

  /**
   * @langEn Option to linearize curve geometries (e.g., CircularString or CurvePolygon) to a Simple
   *     Features geometry. This option only applies to SQL feature providers of dialect PostGIS.
   * @langDe Option zur Linearisierung von Kurvengeometrien (z. B. CircularString oder CurvePolygon)
   *     zu einer Simple-Features-Geometrie. Diese Option gilt nur für SQL-Feature-Anbieter mit
   *     Dialekt PostGIS.
   * @default `false`
   */
  @Override
  Optional<Boolean> getLinearizeCurves();

  /**
   * @langEn Identifies a DATETIME property as a property that contains the timestamp when the
   *     feature was last modified. This information is used in optimistic locking to evaluate the
   *     pre-conditions, if a mutation request includes a `Last-Modified` header.
   * @langDe Kennzeichnet eine DATETIME-Eigenschaft als eine Eigenschaft, die den Zeitstempel
   *     enthält, wann das Feature zuletzt geändert wurde. Diese Information wird beim
   *     optimistischen Sperren verwendet, um die Vorbedingungen zu bewerten, wenn ein CRUD-Request
   *     einen "Last-Modified"-Header enthält.
   * @default see description
   */
  @Override
  Optional<Boolean> getIsLastModified();

  /**
   * @langEn Only for `OBJECT` and `OBJECT_ARRAY`. Object with the property names as keys and schema
   *     objects as values.
   * @langDe Nur bei `OBJECT` und `OBJECT_ARRAY`. Ein Objekt mit einer Eigenschaft pro
   *     Objekteigenschaft. Der Schüssel ist der Name der Objekteigenschaft, der Wert das
   *     Schema-Objekt zu der Objekteigenschaft.
   */
  // behaves exactly like Map<String, FeaturePropertyV2>, but supports mergeable builder
  // deserialization
  // (immutables attributeBuilder does not work with maps yet)
  @JsonProperty("properties")
  @Override
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getPropertyMap();

  /**
   * @langEn If only some of the `properties` are defined in an external `schema`, or if some of the
   *     `properties` should be mapped to a different table, this provides a convenient way to
   *     define these properties alongside the regular properties. The option takes a list of schema
   *     objects, but only `sourcePath`, `schema` and `properties` are considered. For details see
   *     [Mapping Operations](#merge).
   * @langDe Wenn nur einige `properties` in einem externen `schema` definiert sind, oder wenn nur
   *     einige `properties` auf eine andere Tabelle gemappt werden sollen, stellt diese Option
   *     einen komfortablen Weg zur Verfügung, um solche properties zusammen mit den regulären
   *     properties zu definieren. Der Wert ist eine Liste von Schema-Objekten, aber nur
   *     `sourcePath`, `schema` und `properties` werden berücksichtigt. Für Details siehe [Mapping
   *     Operationen](#merge).
   * @default []
   */
  List<PartialObjectSchema> getMerge();

  /**
   * @langEn If the value for a property may come from more than one `sourcePath`, this allows to
   *     choose the first non-null value. This takes a list of value schemas, for details see
   *     [Mapping Operations](#coalesce).
   * @langDe Wenn der Wert für ein Property aus mehr als einem `sourcePath` stammen kann, erlaubt
   *     diese Option den ersten Wert der nicht Null ist zu wählen. Die Option erwartet eine Liste
   *     von Werte-Schemas, für Details siehe [Mapping Operationen](#coalesce).
   * @default []
   */
  List<FeatureSchema> getCoalesce();

  /**
   * @langEn If the values for an array property may come from more than one `sourcePath`, this
   *     allows to concatenate all available values. This takes a list of value or value array
   *     schemas, for details see [Mapping Operations](#concat).
   * @langDe Wenn die Werte für ein Array-Property aus mehr als einem `sourcePath` stammen können,
   *     erlaubt diese Option alle verfügbaren Werte zu konkatenieren. Die Option erwartet eine
   *     Liste von Werte- oder Werte-Array-Schemas, für Details siehe [Mapping
   *     Operationen](#concat).
   * @default []
   */
  List<FeatureSchema> getConcat();

  abstract class Builder
      extends PropertiesSchema.Builder<FeatureSchema, ImmutableFeatureSchema.Builder, FeatureSchema>
      implements PropertiesSchema.BuilderWithName<FeatureSchema, ImmutableFeatureSchema.Builder> {

    public abstract ImmutableFeatureSchema.Builder desiredType(
        @Nullable SchemaBase.Type desiredType);

    @JsonIgnore
    public ImmutableFeatureSchema.Builder type(SchemaBase.Type type) {
      return desiredType(type);
    }

    @JsonIgnore
    public abstract ImmutableFeatureSchema.Builder concat(
        Iterable<? extends FeatureSchema> elements);

    public abstract ImmutableFeatureSchema.Builder addAllConcatBuilders(
        Iterable<ImmutableFeatureSchema.Builder> elements);

    @JsonProperty("concat")
    public ImmutableFeatureSchema.Builder concatBuilders(
        Iterable<ImmutableFeatureSchema.Builder> elements) {
      for (ImmutableFeatureSchema.Builder element : elements) {
        element.name("concat");
      }
      return addAllConcatBuilders(elements);
    }

    @JsonIgnore
    public abstract ImmutableFeatureSchema.Builder coalesce(
        Iterable<? extends FeatureSchema> elements);

    public abstract ImmutableFeatureSchema.Builder addAllCoalesceBuilders(
        Iterable<ImmutableFeatureSchema.Builder> elements);

    @JsonProperty("coalesce")
    public ImmutableFeatureSchema.Builder coalesceBuilders(
        Iterable<ImmutableFeatureSchema.Builder> elements) {
      for (ImmutableFeatureSchema.Builder element : elements) {
        element.name("coalesce");
      }
      return addAllCoalesceBuilders(elements);
    }
  }

  @Override
  default ImmutableFeatureSchema.Builder getBuilder() {
    return new ImmutableFeatureSchema.Builder().from(this);
  }

  @DocIgnore
  Map<String, String> getAdditionalInfo();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  @Override
  default List<FeatureSchema> getProperties() {
    return getPropertyMap().values().stream()
        .map(
            featureSchema -> {
              ImmutableFeatureSchema.Builder builder =
                  new ImmutableFeatureSchema.Builder().from(featureSchema);

              if (getFullPath().size() > featureSchema.getParentPath().size()) {
                builder.parentPath(getFullPath());
              }

              if (featureSchema.getPath().isEmpty()) {
                builder.addPath(featureSchema.getName());
              }

              return builder.build();
            })
        .collect(Collectors.toList());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  @Override
  default boolean isFeature() {
    return isObject()
        && (!getEffectiveSourcePaths().isEmpty()
            && getEffectiveSourcePaths().get(0).startsWith("/"));
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isConstant() {
    return (isValue() && getConstantValue().isPresent())
        || (isObject() && getProperties().stream().allMatch(FeatureSchema::isConstant));
  }

  @Value.Check
  default FeatureSchema primaryGeometry() {
    if (isFeature()
        && getPrimaryGeometry().isPresent()
        && getPrimaryGeometry().filter(FeatureSchema::isPrimaryGeometry).isEmpty()) {
      FeatureSchema primaryGeometry = getPrimaryGeometry().get();
      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(this).propertyMap(new HashMap<>());

      getPropertyMap()
          .forEach(
              (name, property) -> {
                if (property.isSpatial()
                    && Objects.equals(property.getName(), primaryGeometry.getName())) {
                  builder.putPropertyMap(
                      name,
                      new ImmutableFeatureSchema.Builder()
                          .from(property)
                          .role(Role.PRIMARY_GEOMETRY)
                          .build());
                } else {
                  builder.putPropertyMap(name, property);
                }
              });

      return builder.build();
    }

    return this;
  }

  @Value.Check
  default FeatureSchema primaryInstant() {
    if (isFeature()
        && getPrimaryInstant().isPresent()
        && getPrimaryInstant().filter(FeatureSchema::isPrimaryInstant).isEmpty()) {
      FeatureSchema primaryInstant = getPrimaryInstant().get();
      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(this).propertyMap(new HashMap<>());

      getPropertyMap()
          .forEach(
              (name, property) -> {
                if (property.isTemporal()
                    && Objects.equals(property.getName(), primaryInstant.getName())) {
                  builder.putPropertyMap(
                      name,
                      new ImmutableFeatureSchema.Builder()
                          .from(property)
                          .role(Role.PRIMARY_INSTANT)
                          .build());
                } else {
                  builder.putPropertyMap(name, property);
                }
              });

      return builder.build();
    }

    return this;
  }

  @Value.Check
  default FeatureSchema primaryInterval() {
    if (isFeature()
        && getPrimaryInterval().isPresent()
        && getPrimaryInterval()
            .filter(
                interval ->
                    interval.first().isPrimaryIntervalStart()
                        && interval.second().isPrimaryIntervalEnd())
            .isEmpty()) {
      Tuple<FeatureSchema, FeatureSchema> primaryInterval = getPrimaryInterval().get();
      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(this).propertyMap(new HashMap<>());

      getPropertyMap()
          .forEach(
              (name, property) -> {
                if (property.isTemporal()
                    && Objects.equals(property.getName(), primaryInterval.first().getName())) {
                  builder.putPropertyMap(
                      name,
                      new ImmutableFeatureSchema.Builder()
                          .from(property)
                          .role(Role.PRIMARY_INTERVAL_START)
                          .build());
                } else if (property.isTemporal()
                    && Objects.equals(property.getName(), primaryInterval.second().getName())) {
                  builder.putPropertyMap(
                      name,
                      new ImmutableFeatureSchema.Builder()
                          .from(property)
                          .role(Role.PRIMARY_INTERVAL_END)
                          .build());
                } else {
                  builder.putPropertyMap(name, property);
                }
              });

      return builder.build();
    }

    return this;
  }

  @Value.Check
  default void disallowFlattening() {
    Preconditions.checkState(
        getTransformations().isEmpty()
            || getTransformations().stream()
                .noneMatch(transformations -> transformations.getFlatten().isPresent()),
        "The 'flatten' transformation is not allowed in the provider schema. Path: %s.",
        isFeature() ? getName() : getFullPathAsString());
  }

  @Value.Check
  default void checkMappingOperations() {
    Preconditions.checkState(
        getConcat().isEmpty() || isArray(),
        "Concat may only be used with array types. Found: %s. Path: %s.",
        getType(),
        getFullPathAsString());

    Preconditions.checkState(
        getConcat().isEmpty()
            || getConcat().stream().allMatch(s -> s.getTransformations().isEmpty()),
        "Concat items may not contain transformations. Path: %s.",
        getFullPathAsString());

    Preconditions.checkState(
        getConcat().isEmpty()
            || getType() != Type.OBJECT_ARRAY
            || getConcat().stream()
                .allMatch(
                    s ->
                        List.of(Type.STRING, Type.OBJECT, Type.OBJECT_ARRAY).contains(s.getType())),
        "Concat of type OBJECT_ARRAY may only contain items of type OBJECT_ARRAY or OBJECT. Found: %s. Path: %s.",
        getConcat().stream()
            .map(FeatureSchema::getType)
            .filter(t -> !List.of(Type.STRING, Type.OBJECT, Type.OBJECT_ARRAY).contains(t))
            .findFirst()
            .orElse(getType()),
        getFullPathAsString());

    Preconditions.checkState(
        getConcat().isEmpty()
            || getType() != Type.FEATURE_REF_ARRAY
            || getConcat().stream()
                .map(FeatureSchema::getDesiredType)
                .filter(Objects::nonNull)
                .allMatch(type -> List.of(Type.FEATURE_REF, Type.FEATURE_REF_ARRAY).contains(type)),
        "Concat of type FEATURE_REF_ARRAY may only contain items of type FEATURE_REF_ARRAY or FEATURE_REF. Found: %s. Path: %s.",
        getConcat().stream()
            .map(FeatureSchema::getDesiredType)
            .filter(Objects::nonNull)
            .filter(type -> !List.of(Type.FEATURE_REF, Type.FEATURE_REF_ARRAY).contains(type))
            .findFirst()
            .orElse(getType()),
        getFullPathAsString());

    Preconditions.checkState(
        getConcat().isEmpty()
            || getType() != Type.VALUE_ARRAY
            || getConcat().stream()
                .allMatch(
                    s ->
                        List.of(
                                Type.INTEGER,
                                Type.FLOAT,
                                Type.STRING,
                                Type.BOOLEAN,
                                Type.DATE,
                                Type.DATETIME,
                                Type.VALUE_ARRAY,
                                Type.VALUE)
                            .contains(s.getType())),
        "Concat of type VALUE_ARRAY may only contain items of type VALUE_ARRAY, VALUE, INTEGER, FLOAT, STRING, BOOLEAN, DATE or DATETIME. Found: %s. Path: %s.",
        getConcat().stream()
            .map(FeatureSchema::getType)
            .filter(
                t ->
                    !List.of(
                            Type.INTEGER,
                            Type.FLOAT,
                            Type.STRING,
                            Type.BOOLEAN,
                            Type.DATE,
                            Type.DATETIME,
                            Type.VALUE_ARRAY,
                            Type.VALUE)
                        .contains(t))
            .findFirst()
            .orElse(getType()),
        getFullPathAsString());

    Preconditions.checkState(
        getCoalesce().isEmpty()
            || getCoalesce().stream().allMatch(s -> s.getTransformations().isEmpty()),
        "Coalesce items may not contain transformations. Path: %s.",
        getFullPathAsString());

    Preconditions.checkState(
        getCoalesce().isEmpty()
            || getType() != Type.FEATURE_REF
            || getCoalesce().stream()
                .allMatch(s -> List.of(Type.STRING, Type.FEATURE_REF).contains(s.getType())),
        "Coalesce of type FEATURE_REF may only contain items of type FEATURE_REF. Found: %s. Path: %s.",
        getCoalesce().stream()
            .map(FeatureSchema::getType)
            .filter(t -> !List.of(Type.STRING, Type.FEATURE_REF).contains(t))
            .findFirst()
            .orElse(getType()),
        getFullPathAsString());

    Preconditions.checkState(
        getCoalesce().isEmpty()
            || getType() != Type.VALUE
            || getCoalesce().stream()
                .allMatch(
                    s ->
                        List.of(
                                Type.INTEGER,
                                Type.FLOAT,
                                Type.STRING,
                                Type.BOOLEAN,
                                Type.DATE,
                                Type.DATETIME,
                                Type.VALUE_ARRAY,
                                Type.VALUE)
                            .contains(s.getType())),
        "Coalesce of type VALUE may only contain items of type INTEGER, FLOAT, STRING, BOOLEAN, DATE, DATETIME, VALUE or VALUE_ARRAY. Found: %s. Path: %s.",
        getCoalesce().stream()
            .map(FeatureSchema::getType)
            .filter(
                t ->
                    !List.of(
                            Type.INTEGER,
                            Type.FLOAT,
                            Type.STRING,
                            Type.BOOLEAN,
                            Type.DATE,
                            Type.DATETIME,
                            Type.VALUE_ARRAY,
                            Type.VALUE)
                        .contains(t))
            .findFirst()
            .orElse(getType()),
        getFullPathAsString());

    Preconditions.checkState(
        getCoalesce().isEmpty()
            || !List.of(
                    Type.INTEGER, Type.FLOAT, Type.STRING, Type.BOOLEAN, Type.DATE, Type.DATETIME)
                .contains(getType())
            || getCoalesce().stream()
                .allMatch(s -> List.of(Type.STRING, getType()).contains(s.getType())),
        "Coalesce of type %s may only contain items of type %s. Found: %s. Path: %s.",
        getType(),
        getType(),
        getCoalesce().stream()
            .map(FeatureSchema::getType)
            .filter(t -> !List.of(Type.STRING, getType()).contains(t))
            .findFirst()
            .orElse(getType()),
        getFullPathAsString());
  }

  @Value.Check
  default void checkIsQueryable() {
    Preconditions.checkState(
        !queryable() || (!isObject() && !Objects.equals(getType(), Type.UNKNOWN)),
        "A queryable property must not be of type OBJECT, OBJECT_ARRAY or UNKNOWN. Found: %s. Path: %s.",
        getType(),
        getFullPathAsString());
  }

  @Value.Check
  default void checkIsSortable() {
    Preconditions.checkState(
        !sortable()
            || (!isSpatial()
                && !isObject()
                && !isArray()
                && !Objects.equals(getType(), Type.BOOLEAN)
                && !Objects.equals(getType(), Type.UNKNOWN)),
        "A sortable property must be a string, a number or an instant. Found %s. Path: %s.",
        getType(),
        getFullPathAsString());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  @Override
  default List<FeatureSchema> getAllNestedProperties() {
    return Stream.concat(
            getProperties().stream()
                .flatMap(t -> Stream.concat(Stream.of(t), t.getAllNestedProperties().stream())),
            getMerge().stream().flatMap(t -> t.getAllNestedProperties().stream()))
        .collect(Collectors.toList());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<PartialObjectSchema> getAllNestedPartials() {
    return getMerge().stream()
        .flatMap(
            t ->
                Stream.concat(
                    Stream.of(t),
                    t.getAllNestedProperties().stream()
                        .flatMap(prop -> prop.getAllNestedPartials().stream())))
        .collect(Collectors.toList());
  }

  default FeatureSchema accept(FeatureSchemaTransformer visitor, List<FeatureSchema> parents) {
    Function<FeatureSchema, FeatureSchema> visit =
        property ->
            property.accept(
                visitor,
                new ImmutableList.Builder<FeatureSchema>().addAll(parents).add(this).build());

    return visitor.visit(
        this,
        parents,
        getPropertyMap().values().stream().map(visit).collect(Collectors.toList()),
        getMerge().stream()
            .map(
                partial -> {
                  if (partial.getPropertyMap().isEmpty()) {
                    return partial;
                  }
                  return new ImmutablePartialObjectSchema.Builder()
                      .from(partial)
                      .propertyMap(
                          partial.getPropertyMap().entrySet().stream()
                              .map(
                                  entry ->
                                      new SimpleEntry<>(
                                          entry.getKey(),
                                          (FeatureSchema) visit.apply(entry.getValue())))
                              .collect(
                                  ImmutableMap.toImmutableMap(
                                      Map.Entry::getKey, Map.Entry::getValue)))
                      .build();
                })
            .collect(Collectors.toList()));
  }

  default FeatureSchema with(Consumer<ImmutableFeatureSchema.Builder> changes) {
    ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder().from(this);

    changes.accept(builder);

    if (!getConcat().isEmpty()) {
      builder.concat(
          getConcat().stream().map(concat -> apply(concat, changes)).collect(Collectors.toList()));
    }

    if (!getCoalesce().isEmpty()) {
      builder.coalesce(
          getCoalesce().stream()
              .map(coalesce -> apply(coalesce, changes))
              .collect(Collectors.toList()));
    }

    return builder.build();
  }

  static FeatureSchema apply(
      FeatureSchema schema, Consumer<ImmutableFeatureSchema.Builder> changes) {
    ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder().from(schema);

    changes.accept(builder);

    return builder.build();
  }
}
