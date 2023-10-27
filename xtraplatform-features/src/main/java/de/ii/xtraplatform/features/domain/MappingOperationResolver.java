/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @langEn Mapping operations may be needed when the source and target schema structure diverge too
 *     much.
 *     <p>### Merge
 *     <p>If only some of the `properties` are defined in an external `schema`, or if some of the
 *     `properties` should be mapped to a different table, this provides a convenient way to define
 *     these properties alongside the regular properties.
 *     <p>#### Examples
 *     <p>##### Define only some properties using an external JSON schema
 *     <p><code>
 * ```yaml
 * example:
 *   sourcePath: /main
 *   type: OBJECT
 *   merge:
 *   - properties:
 *       id:
 *         sourcePath: id
 *         type: INTEGER
 *         role: ID
 *   - sourcePath: '[JSON]names'
 *     schema: names.json
 * ```
 * </code>
 *     <p>##### Using columns from a joined table in the main feature
 *     <p><code>
 * ```yaml
 * example:
 *   sourcePath: /main
 *   type: OBJECT
 *   merge:
 *   - properties:
 *       id:
 *         sourcePath: id
 *         type: INTEGER
 *         role: ID
 *   - sourcePath: '[id=id]names'
 *     properties:
 *       name1:
 *         sourcePath: name1
 *         type: STRING
 *       name2:
 *         sourcePath: name2
 *         type: STRING
 * ```
 * </code>
 *     <p>### Coalesce
 *     <p>If the value for a property may come from more than one `sourcePath`, this allows to
 *     choose the first non-null value.
 *     <p>#### Example
 *     <p><code>
 * ```yaml
 * foo:
 *   type: OBJECT
 *   properties:
 *     bar:
 *       type: VALUE
 *       coalesce:
 *       - sourcePath: bar_stringValue
 *         type: STRING
 *       - sourcePath: bar_integerValue
 *         type: INTEGER
 *       - sourcePath: bar_booleanValue
 *         type: BOOLEAN
 * ```
 * </code>
 *     <p>#### Type compatibility
 *     <p>Constraints on the types of inner properties depending on the type of the outer property
 *     are shown in the table below.
 *     <p><code>
 * | Outer type  | Valid inner types  | Remarks |
 * |---|---|---|
 * | `VALUE`  |  `INTEGER`, `FLOAT`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE`  |   |
 * | `INTEGER`  |  `INTEGER` |   |
 * | `FLOAT`  |  `FLOAT` |   |
 * | `STRING`  |  `STRING`  |   |
 * | `BOOLEAN`  |  `BOOLEAN`  |   |
 * | `DATETIME`  |  `DATETIME`  |   |
 * | `DATE`  |  `DATE`  |   |
 * | `OBJECT`  |  `OBJECT`  | Different `objectType` with different schemas can be used  |
 * | `FEATURE_REF `  |  `FEATURE_REF `  | Different `refType` can be used  |
 * </code>
 *     <p>### Concat
 *     <p>If the values for an array property may come from more than one `sourcePath`, this allows
 *     to concatenate all available values.
 *     <p>#### Example
 *     <p><code>
 * ```yaml
 * foo:
 *   type: OBJECT
 *   properties:
 *     bar:
 *       type: FEATURE_REF_ARRAY
 *       concat:
 *       - sourcePath: '[id=foo_fk]baz1/id'
 *         refType: baz1
 *       - sourcePath: '[id=foo_fk]baz2/id'
 *         refType: baz2
 *       - sourcePath: '[id=foo_fk]bazn/id'
 *         refType: bazn
 * ```
 * </code>
 *     <p>#### Type compatibility
 *     <p>Constraints on the types of inner properties depending on the type of the outer property
 *     are shown in the table below.
 *     <p><code>
 * | Outer type  | Valid inner types  | Remarks |
 * |---|---|---|
 * | `VALUE_ARRAY`  |  `VALUE_ARRAY`, `INTEGER`, `FLOAT`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE`  |   |
 * | `OBJECT_ARRAY`  |  `OBJECT_ARRAY`, `OBJECT`  | Different `objectType` with different schemas can be used  |
 * | `FEATURE_REF_ARRAY `  |  `FEATURE_REF_ARRAY`, `FEATURE_REF `  | Different `refType` can be used  |
 * </code>
 * @langDe Mapping Operationen können notwendig sein, wenn die Quell- and Ziel-Schema-Struktur zu
 *     unterschiedlich sind.
 *     <p>### Merge
 *     <p>Wenn nur einige `properties` in einem externen `schema` definiert sind, oder wenn nur
 *     einige `properties` auf eine andere Tabelle gemappt werden sollen, stellt diese Option einen
 *     komfortablen Weg zur Verfügung, um solche properties zusammen mit den regulären properties zu
 *     definieren.
 *     <p>#### Beispiele
 *     <p>##### Einige Properties in einem externen JSON schema definieren
 *     <p><code>
 * ```yaml
 * example:
 *   sourcePath: /main
 *   type: OBJECT
 *   merge:
 *   - properties:
 *       id:
 *         sourcePath: id
 *         type: INTEGER
 *         role: ID
 *   - sourcePath: '[JSON]names'
 *     schema: names.json
 * ```
 * </code>
 *     <p>##### Spalten aus einer gejointen Tabelle im Haupt-Feature verwenden
 *     <p><code>
 * ```yaml
 * example:
 *   sourcePath: /main
 *   type: OBJECT
 *   merge:
 *   - properties:
 *       id:
 *         sourcePath: id
 *         type: INTEGER
 *         role: ID
 *   - sourcePath: '[id=id]names'
 *     properties:
 *       name1:
 *         sourcePath: name1
 *         type: STRING
 *       name2:
 *         sourcePath: name2
 *         type: STRING
 * ```
 * </code>
 *     <p>### Coalesce
 *     <p>Wenn der Wert für ein Property aus mehr als einem `sourcePath` stammen kann, erlaubt diese
 *     Option den ersten Wert der nicht Null ist zu wählen.
 *     <p>#### Beispiel
 *     <p><code>
 * ```yaml
 * foo:
 *   type: OBJECT
 *   properties:
 *     bar:
 *       type: VALUE
 *       coalesce:
 *       - sourcePath: bar_stringValue
 *         type: STRING
 *       - sourcePath: bar_integerValue
 *         type: INTEGER
 *       - sourcePath: bar_booleanValue
 *         type: BOOLEAN
 * ```
 * </code>
 *     <p>#### Typ-Kompabilität
 *     <p>Constraints on the types of inner properties depending on the type of the outer property
 *     are shown in the table below.
 *     <p><code>
 * | Outer type  | Valid inner types  | Remarks |
 * |---|---|---|
 * | `VALUE`  |  `INTEGER`, `FLOAT`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE`  |   |
 * | `INTEGER`  |  `INTEGER` |   |
 * | `FLOAT`  |  `FLOAT` |   |
 * | `STRING`  |  `STRING`  |   |
 * | `BOOLEAN`  |  `BOOLEAN`  |   |
 * | `DATETIME`  |  `DATETIME`  |   |
 * | `DATE`  |  `DATE`  |   |
 * | `OBJECT`  |  `OBJECT`  | Different `objectType` with different schemas can be used  |
 * | `FEATURE_REF `  |  `FEATURE_REF `  | Different `refType` can be used  |
 * </code>
 *     <p>### Concat
 *     <p>Wenn die Werte für ein Array-Property aus mehr als einem `sourcePath` stammen können,
 *     erlaubt diese Option alle verfügbaren Werte zu konkatenieren.
 *     <p>#### Beispiel
 *     <p><code>
 * ```yaml
 * foo:
 *   type: OBJECT
 *   properties:
 *     bar:
 *       type: FEATURE_REF_ARRAY
 *       concat:
 *       - sourcePath: '[id=foo_fk]baz1/id'
 *         refType: baz1
 *       - sourcePath: '[id=foo_fk]baz2/id'
 *         refType: baz2
 *       - sourcePath: '[id=foo_fk]bazn/id'
 *         refType: bazn
 * ```
 * </code>
 *     <p>#### Typ-Kompabilität
 *     <p>Constraints on the types of inner properties depending on the type of the outer property
 *     are shown in the table below.
 *     <p><code>
 * | Outer type  | Valid inner types  | Remarks |
 * |---|---|---|
 * | `VALUE_ARRAY`  |  `VALUE_ARRAY`, `INTEGER`, `FLOAT`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE`  |   |
 * | `OBJECT_ARRAY`  |  `OBJECT_ARRAY`, `OBJECT`  | Different `objectType` with different schemas can be used  |
 * | `FEATURE_REF_ARRAY `  |  `FEATURE_REF_ARRAY`, `FEATURE_REF `  | Different `refType` can be used  |
 * </code>
 */
public class MappingOperationResolver implements TypesResolver {

  @Override
  public boolean needsResolving(FeatureSchema type) {
    return hasMerge(type) || hasConcat(type) || hasCoalesce(type);
  }

  @Override
  public FeatureSchema resolve(FeatureSchema type) {
    FeatureSchema resolved = type;

    if (hasMerge(type)) {
      resolved = resolveMerge(type);
    }

    if (hasConcat(type)) {
      resolved = resolveConcat(type);
    }

    if (hasCoalesce(type)) {
      resolved = resolveCoalesce(type);
    }

    return resolved;
  }

  private FeatureSchema resolveMerge(FeatureSchema type) {
    Map<String, FeatureSchema> props = new LinkedHashMap<>();

    type.getMerge()
        .forEach(
            partial -> {
              if (partial.getSourcePath().isPresent()) {
                partial
                    .getPropertyMap()
                    .forEach(
                        (key, schema) -> {
                          props.put(
                              key,
                              new ImmutableFeatureSchema.Builder()
                                  .from(schema)
                                  .sourcePath(
                                      schema
                                          .getSourcePath()
                                          .map(
                                              sourcePath ->
                                                  String.format(
                                                      "%s/%s",
                                                      partial.getSourcePath().get(), sourcePath)))
                                  .sourcePaths(
                                      schema.getSourcePaths().stream()
                                          .map(
                                              sourcePath ->
                                                  String.format(
                                                      "%s/%s",
                                                      partial.getSourcePath().get(), sourcePath))
                                          .collect(Collectors.toList()))
                                  .build());
                        });
              } else {
                props.putAll(partial.getPropertyMap());
              }
            });

    return new ImmutableFeatureSchema.Builder()
        .from(type)
        .merge(List.of())
        .propertyMap(props)
        .build();
  }

  private FeatureSchema resolveConcat(FeatureSchema type) {
    if (type.getType() == Type.VALUE_ARRAY || type.getType() == Type.FEATURE_REF_ARRAY) {
      String basePath = type.getSourcePath().map(p -> p + "/").orElse("");

      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(type).sourcePath(Optional.empty());

      for (FeatureSchema concat : type.getConcat()) {
        builder.addSourcePaths(basePath + concat.getSourcePath().orElse(""));
      }

      if (type.getType() == Type.FEATURE_REF_ARRAY
          && type.getConcat().stream().anyMatch(s -> s.getType() == Type.STRING)) {
        builder.concat(
            type.getConcat().stream()
                .map(
                    s -> {
                      if (s.getType() == Type.STRING) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(s)
                            .type(type.getType())
                            .build();
                      }
                      return s;
                    })
                .collect(Collectors.toList()));
      }

      return builder.build();
    }

    if (type.getType() == Type.OBJECT_ARRAY) {
      String basePath = type.getSourcePath().map(p -> p + "/").orElse("");

      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(type).sourcePath(Optional.empty());

      for (int i = 0; i < type.getConcat().size(); i++) {
        String basePath2 =
            basePath + type.getConcat().get(i).getSourcePath().map(p -> p + "/").orElse("");

        for (FeatureSchema prop : type.getConcat().get(i).getProperties()) {
          builder.putProperties2(
              i + "_" + prop.getName(),
              new ImmutableFeatureSchema.Builder()
                  .from(prop)
                  .sourcePath(basePath2 + prop.getSourcePath().orElse(""))
                  .path(List.of(i + "_" + prop.getName()))
                  .putAdditionalInfo("concatIndex", String.valueOf(i))
                  .putAdditionalInfo(
                      type.getConcat().get(i).isArray() ? "concatArray" : "concatValue", "true")
                  .transformations(List.of())
                  .addTransformations(
                      new ImmutablePropertyTransformation.Builder().rename(prop.getName()).build())
                  .addAllTransformations(prop.getTransformations()));
        }
      }

      if (type.getConcat().stream().anyMatch(s -> s.getType() == Type.STRING)) {
        builder.concat(
            type.getConcat().stream()
                .map(
                    s -> {
                      if (s.getType() == Type.STRING) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(s)
                            .type(type.getType())
                            .build();
                      }
                      return s;
                    })
                .collect(Collectors.toList()));
      }

      return builder.build();
    }

    return type;
  }

  private FeatureSchema resolveCoalesce(FeatureSchema type) {
    if (type.isValue() && !type.isArray()) {
      String basePath = type.getSourcePath().map(p -> p + "/").orElse("");

      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(type).sourcePath(Optional.empty());

      for (FeatureSchema coalesce : type.getCoalesce()) {
        builder.addSourcePaths(basePath + coalesce.getSourcePath().orElse(""));
      }

      if (type.getType() == Type.FEATURE_REF
          && type.getCoalesce().stream().anyMatch(s -> s.getType() == Type.STRING)) {
        builder.coalesce(
            type.getCoalesce().stream()
                .map(
                    s -> {
                      if (s.getType() == Type.STRING) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(s)
                            .type(type.getType())
                            .build();
                      }
                      return s;
                    })
                .collect(Collectors.toList()));
      }

      return builder.build();
    }

    if (type.isObject() && !type.isArray()) {
      String basePath = type.getSourcePath().map(p -> p + "/").orElse("");

      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(type).sourcePath(Optional.empty());

      for (int i = 0; i < type.getCoalesce().size(); i++) {
        String basePath2 =
            basePath + type.getCoalesce().get(i).getSourcePath().map(p -> p + "/").orElse("");

        for (FeatureSchema prop : type.getCoalesce().get(i).getProperties()) {
          builder.putProperties2(
              i + "_" + prop.getName(),
              new ImmutableFeatureSchema.Builder()
                  .from(prop)
                  .sourcePath(basePath2 + prop.getSourcePath().orElse(""))
                  .path(List.of(i + "_" + prop.getName()))
                  .putAdditionalInfo("coalesceIndex", String.valueOf(i))
                  .putAdditionalInfo(
                      type.getCoalesce().get(i).isArray() ? "coalesceArray" : "coalesceValue",
                      "true")
                  .transformations(List.of())
                  .addTransformations(
                      new ImmutablePropertyTransformation.Builder().rename(prop.getName()).build())
                  .addAllTransformations(prop.getTransformations()));
        }
      }

      if (type.getCoalesce().stream().anyMatch(s -> s.getType() == Type.STRING)) {
        builder.coalesce(
            type.getCoalesce().stream()
                .map(
                    s -> {
                      if (s.getType() == Type.STRING) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(s)
                            .type(type.getType())
                            .build();
                      }
                      return s;
                    })
                .collect(Collectors.toList()));
      }

      return builder.build();
    }

    return type;
  }

  private static boolean hasMerge(FeatureSchema type) {
    return !type.getMerge().isEmpty();
  }

  private static boolean hasConcat(FeatureSchema type) {
    return !type.getConcat().isEmpty();
  }

  private static boolean hasCoalesce(FeatureSchema type) {
    return !type.getCoalesce().isEmpty();
  }
}
