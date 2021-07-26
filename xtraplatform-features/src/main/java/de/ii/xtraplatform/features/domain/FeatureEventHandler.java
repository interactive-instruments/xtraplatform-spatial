/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.immutables.value.Value;

public interface FeatureEventHandler<T extends ModifiableContext> {

  interface Context {

    ModifiableCollectionMetadata metadata();

    List<String> path();

    Optional<SimpleFeatureGeometry> geometryType();

    OptionalInt geometryDimension();

    @Nullable
    String value();

    @Nullable
    Type valueType();

    @Nullable
    FeatureSchema customSchema();

    @Value.Default
    default boolean inGeometry() {
      return false;
    }

    @Value.Default
    default boolean inObject() {
      return false;
    }

    @Value.Default
    default boolean inArray() {
      return false;
    }

    List<Integer> indexes();

    @Value.Derived
    default long index() {
      return indexes().isEmpty() ? 0 : indexes().get(0);
    }

    FeatureQuery query();

    SchemaMapping mapping();

    @Value.Default
    default int schemaIndex() {
      return -1;
    }

    @Value.Derived
    @Value.Auxiliary
    default Optional<FeatureSchema> schema() {
      return Optional.ofNullable(customSchema()).or(this::currentSchema);
    }

    @Value.Derived
    @Value.Auxiliary
    default Optional<FeatureSchema> currentSchema() {
      List<String> path = path();

      if (path.isEmpty()) {
        return Optional.ofNullable(mapping().getTargetSchema());
      }

      List<FeatureSchema> targetSchemas = mapping().getTargetSchemas(path);

      if (targetSchemas.isEmpty()) {
        //LOGGER.warn("No mapping found for path {}.", path);
        return Optional.empty();
      }

      int schemaIndex = schemaIndex() > -1 ? schemaIndex() : targetSchemas.size() - 1;
      FeatureSchema targetSchema = targetSchemas.get(schemaIndex);

      return Optional.ofNullable(targetSchema);
    }

    @Value.Derived
    @Value.Auxiliary
    default List<FeatureSchema> parentSchemas() {
      List<String> path = path();

      if (path.isEmpty()) {
        return ImmutableList.of();
      }

      List<List<FeatureSchema>> parentSchemas = mapping().getParentSchemas(path);

      if (parentSchemas.isEmpty()) {
        return ImmutableList.of();
      }

      int schemaIndex = schemaIndex() > -1 ? schemaIndex() : parentSchemas.size() - 1;
      return parentSchemas.get(schemaIndex);
    }
  }

  interface ModifiableContext extends Context {

    //TODO: default values are not cached by Modifiable
    @Value.Default
    default ModifiableCollectionMetadata metadata() {
      ModifiableCollectionMetadata collectionMetadata = ModifiableCollectionMetadata
          .create();

      setMetadata(collectionMetadata);

      return collectionMetadata;
    }

    //TODO: default values are not cached by Modifiable
    @Value.Default
    default FeaturePathTracker pathTracker() {
      FeaturePathTracker pathTracker = new FeaturePathTracker();

      setPathTracker(pathTracker);

      return pathTracker;
    }

    @Value.Derived
    @Override
    default List<String> path() {
      return pathTracker().asList();
    }

    ModifiableContext setMetadata(ModifiableCollectionMetadata collectionMetadata);

    ModifiableContext setPathTracker(FeaturePathTracker pathTracker);

    ModifiableContext setGeometryType(SimpleFeatureGeometry geometryType);

    ModifiableContext setGeometryType(Optional<SimpleFeatureGeometry> geometryType);

    ModifiableContext setGeometryDimension(int geometryDimension);

    ModifiableContext setGeometryDimension(OptionalInt geometryDimension);

    ModifiableContext setValue(String value);

    ModifiableContext setValueType(SchemaBase.Type valueType);

    ModifiableContext setCustomSchema(FeatureSchema schema);

    ModifiableContext setInGeometry(boolean inGeometry);

    ModifiableContext setInObject(boolean inObject);

    ModifiableContext setInArray(boolean inArray);

    ModifiableContext setIndexes(Iterable<Integer> indexes);

    ModifiableContext setQuery(FeatureQuery query);

    ModifiableContext setMapping(SchemaMapping mapping);

    ModifiableContext setSchemaIndex(int schemaIndex);
  }

  //T createContext();

  void onStart(T context);

  void onEnd(T context);

  void onFeatureStart(T context);

  void onFeatureEnd(T context);

  void onObjectStart(T context);

  void onObjectEnd(T context);

  void onArrayStart(T context);

  void onArrayEnd(T context);

  void onValue(T context);
}
