/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableVectorLayer.class)
@JsonPropertyOrder({"id", "fields", "description", "maxzoom", "minzoom"})
public interface VectorLayer {

  @JsonProperty("id")
  String getId();

  @JsonProperty("fields")
  Map<String, String> getFields();

  @JsonProperty("description")
  Optional<String> getDescription();

  @JsonProperty("geometry_type")
  Optional<String> getGeometryType();

  @JsonProperty("maxzoom")
  Optional<Number> getMaxzoom();

  @JsonProperty("minzoom")
  Optional<Number> getMinzoom();

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  static VectorLayer of(FeatureSchema featureSchema, Optional<MinMax> minMax) {
    String geometryType =
        VectorLayer.getGeometryTypeAsString(
            featureSchema
                .getPrimaryGeometry()
                .flatMap(FeatureSchema::getGeometryType)
                .orElse(SimpleFeatureGeometry.ANY));

    Map<String, String> properties =
        featureSchema.getProperties().stream()
            .filter(prop -> !prop.isSpatial())
            .map(
                prop ->
                    new SimpleEntry<>(prop.getName(), VectorLayer.getTypeAsString(prop.getType())))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    return ImmutableVectorLayer.builder()
        .id(featureSchema.getName())
        .description(featureSchema.getDescription().orElse(""))
        .fields(properties)
        .geometryType(geometryType)
        .minzoom(minMax.map(MinMax::getMin))
        .maxzoom(minMax.map(MinMax::getMax))
        .build();
  }

  @JsonIgnore
  @Value.Lazy
  default FeatureSchema toFeatureSchema() {
    ImmutableFeatureSchema geometry =
        new Builder()
            .name("geometry")
            .type(Type.GEOMETRY)
            .geometryType(getGeometryTypeFromString(getGeometryType().orElse("")))
            .build();

    Map<String, FeatureSchema> properties =
        getFields().entrySet().stream()
            .map(
                (entry) -> {
                  ImmutableFeatureSchema property =
                      new Builder()
                          .name(entry.getKey())
                          .type(getTypeFromString(entry.getValue()))
                          .build();
                  return new SimpleEntry<>(entry.getKey(), property);
                })
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    return new ImmutableFeatureSchema.Builder()
        .name(getId())
        .type(Type.OBJECT)
        .description(getDescription())
        .putPropertyMap(geometry.getName(), geometry)
        .putAllPropertyMap(properties)
        .build();
  }

  static String getTypeAsString(SchemaBase.Type type) {
    switch (type) {
      case INTEGER:
        return "Integer";
      case FLOAT:
        return "Number";
      case BOOLEAN:
        return "Boolean";
      case DATETIME:
      case STRING:
      default:
        return "String";
    }
  }

  static SchemaBase.Type getTypeFromString(String type) {
    switch (type) {
      case "Number":
        return Type.FLOAT;
      case "Boolean":
        return Type.BOOLEAN;
      case "String":
      default:
        return Type.STRING;
    }
  }

  static String getGeometryTypeAsString(SimpleFeatureGeometry geometryType) {
    switch (geometryType) {
      case POINT:
      case MULTI_POINT:
        return "points";
      case LINE_STRING:
      case MULTI_LINE_STRING:
        return "lines";
      case POLYGON:
      case MULTI_POLYGON:
        return "polygons";
      case GEOMETRY_COLLECTION:
      case ANY:
      case NONE:
      default:
        return "unknown";
    }
  }

  static SimpleFeatureGeometry getGeometryTypeFromString(String geometryType) {
    switch (geometryType) {
      case "points":
        return SimpleFeatureGeometry.MULTI_POINT;
      case "lines":
        return SimpleFeatureGeometry.MULTI_LINE_STRING;
      case "polygons":
        return SimpleFeatureGeometry.MULTI_POLYGON;
      case "unknown":
      default:
        return SimpleFeatureGeometry.ANY;
    }
  }
}
