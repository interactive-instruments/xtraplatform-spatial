/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.Buildable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
  "type",
  "role",
  "valueType",
  "geometryType",
  "objectType",
  "label",
  "description",
  "unit",
  "transformations",
  "constraints",
  "properties"
})
public interface FeatureSchema extends SchemaBase<FeatureSchema>, Buildable<FeatureSchema> {

  enum Scope {
    QUERIES,
    MUTATIONS
  }

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
   *     [SQL](sql.md#path-syntax) or [WFS](wfs.md#path-syntax).
   * @langDe Der relative Pfad zu diesem Schemaobjekt. Die Pfadsyntax ist je nach Provider-Typ
   *     unterschiedlich ([SQL](sql.md#path-syntax) und [WFS](wfs.md#path-syntax)).
   */
  @JsonAlias("path")
  @Override
  Optional<String> getSourcePath();

  /**
   * @langEn The relative paths for this schema object. The syntax depends on the provider types,
   *     see [SQL](sql.md#path-syntax) or [WFS](wfs.md#path-syntax).
   * @langDe Die relativen Pfade zu diesem Schemaobjekt. Die Pfadsyntax ist je nach Provider-Typ
   *     unterschiedlich ([SQL](sql.md#path-syntax) und [WFS](wfs.md#path-syntax)).
   * @default [sourcePath]
   */
  @JsonMerge(OptBoolean.FALSE)
  @Override
  List<String> getSourcePaths();

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
  @Value.Default
  @Override
  default Type getType() {
    return getPropertyMap().isEmpty() ? Type.STRING : Type.OBJECT;
  }

  /**
   * @langEn `ID` has to be set for the property that should be used as the unique feature id. As a
   *     rule that should be the first property ion the `properties` object. Property names cannot
   *     contain spaces (" ") or slashes ("/"). Set `TYPE` for a property that specifies the type
   *     name of the object.
   * @langDe Kennzeichnet besondere Bedeutungen der Eigenschaft.
   *     <p><code>
   * - `ID` ist bei der Eigenschaft eines Objekts anzugeben, die für die `featureId` in der API zu verwenden ist. Diese Eigenschaft ist typischerweise die erste Eigenschaft im `properties`-Objekt. Erlaubte Zeichen in diesen Eigenschaften sind alle Zeichen bis auf das Leerzeichen (" ") und der Querstrich ("/").
   * - `TYPE` ist optional bei der Eigenschaft eines Objekts anzugeben, die den Namen einer Unterobjektart enthält.
   * - Hat eine Objektart mehrere Geometrieeigenschaften, dann ist `PRIMARY_GEOMETRY` bei der Eigenschaft anzugeben, die für `bbox`-Abfragen verwendet werden soll und die in GeoJSON in `geometry` oder in JSON-FG in `where` kodiert werden soll.
   * - Hat eine Objektart mehrere zeitliche Eigenschaften, dann sollte `PRIMARY_INSTANT` bei der Eigenschaft angegeben werden, die für `datetime`-Abfragen verwendet werden soll, sofern ein Zeitpunkt die zeitliche Ausdehnung der Features beschreibt.
   * - Ist die zeitliche Ausdehnung hingegen ein Zeitintervall, dann sind `PRIMARY_INTERVAL_START` und `PRIMARY_INTERVAL_END` bei den jeweiligen zeitlichen Eigenschaften anzugeben.
   * </code>
   *     <p>
   * @default null
   */
  @Override
  Optional<Role> getRole();

  /**
   * @langEn Only needed when `type` is `VALUE_ARRAY`. Possible values: `FLOAT`, `INTEGER`,
   *     `STRING`, `BOOLEAN`, `DATETIME`, `DATE`
   * @langDe Wird nur benötigt wenn `type` auf `VALUE_ARRAY` gesetzt ist. Mögliche Werte: `FLOAT`,
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
   * @langEn Optional scope for properties that should only be used when either reading (`QUERIES`)
   *     or writing (`MUTATIONS`) features.
   * @langDe Optionaler Geltungsbereich für Eigenschaften die entweder nur beim Lesen (`QUERIES`) *
   *     oder beim Schreiben (`MUTATIONS`) verwendet werden sollen.
   * @default null
   */
  Optional<Scope> getScope();

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
   * @langEn Only for `OBJECT` and `OBJECT_ARRAY`. Object with the property names as keys and schema
   *     objects as values.
   * @langDe Nur bei `OBJECT` und `OBJECT_ARRAY`. Ein Objekt mit einer Eigenschaft pro
   *     Objekteigenschaft. Der Schüssel ist der Name der Objekteigenschaft, der Wert das
   *     Schema-Objekt zu der Objekteigenschaft.
   */
  // behaves exactly like Map<String, FeaturePropertyV2>, but supports mergeable builder
  // deserialization
  // (immutables attributeBuilder does not work with maps yet)
  @JsonProperty(value = "properties")
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getPropertyMap();

  // custom builder to automatically use keys of properties as name
  abstract class Builder implements BuildableBuilder<FeatureSchema> {

    public abstract ImmutableFeatureSchema.Builder putPropertyMap(
        String key, ImmutableFeatureSchema.Builder builder);

    // @JsonMerge
    @JsonProperty(value = "properties")
    public ImmutableFeatureSchema.Builder putProperties2(
        Map<String, ImmutableFeatureSchema.Builder> builderMap) {
      ImmutableFeatureSchema.Builder builder1 = null;
      for (Map.Entry<String, ImmutableFeatureSchema.Builder> entry : builderMap.entrySet()) {
        String key = entry.getKey();
        ImmutableFeatureSchema.Builder builder = entry.getValue();
        builder1 = putPropertyMap(key, builder.name(key));
      }
      return builder1;
      // return putPropertyMap(key, builder.name(key));
    }

    // @JsonProperty(value = "properties")
    // @JsonAnySetter
    public ImmutableFeatureSchema.Builder putProperties2(
        String key, ImmutableFeatureSchema.Builder builder) {
      return putPropertyMap(key, builder.name(key));
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
  default FeatureSchema backwardsCompatibility() {
    // migrate double column syntax to multiple sourcePaths, ignore wfs mappings
    if (!getParentPath().isEmpty()
        && !getParentPath().get(0).contains(":")
        && getSourcePath()
            .filter(path -> path.lastIndexOf(':') > path.lastIndexOf('/'))
            .isPresent()) {
      @Deprecated(since = "3.1.0")
      String path1 = getSourcePath().get().substring(0, getSourcePath().get().lastIndexOf(':'));
      String path2 =
          path1.substring(0, path1.lastIndexOf('/') + 1)
              + getSourcePath().get().substring(getSourcePath().get().lastIndexOf(':') + 1);

      LOGGER.info(
          "The sourcePath '{}' in property '{}' uses a deprecated style that includes a colon to merge two columns. Please use multiple sourcePaths instead, one for each column.",
          getSourcePath().get(),
          getName());

      return new ImmutableFeatureSchema.Builder()
          .from(this)
          .sourcePath(Optional.empty())
          .sourcePaths(ImmutableList.of(path1, path2))
          .addTransformations(
              new ImmutablePropertyTransformation.Builder()
                  .stringFormat(String.format("{{%s}} ||| {{%s}}", path1, path2))
                  .build())
          .build();
    }

    return this;
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
}
