/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.immutables.value.Value;

public interface FeatureEventHandler<T extends SchemaBase<T>, U extends SchemaMappingBase<T>, V extends ModifiableContext<T, U>> {

  interface Context<T extends SchemaBase<T>, U extends SchemaMappingBase<T>> {

    ModifiableCollectionMetadata metadata();

    List<String> path();

    String pathAsString();

    Optional<SimpleFeatureGeometry> geometryType();

    OptionalInt geometryDimension();

    @Nullable
    String value();

    @Nullable
    Type valueType();

    Map<String, String> valueBuffer();

    @Nullable
    T customSchema();

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

    @Value.Lazy
    default long index() {
      return indexes().isEmpty() ? 0 : indexes().get(indexes().size()-1);
    }

    FeatureQuery query();

    U mapping();

    @Value.Default
    default int schemaIndex() {
      return -1;
    }

    Map<String, String> transformed();

    @Value.Default
    default boolean isBuffering() {
      return false;
    }

    Map<String, String> additionalInfo();

    @Value.Lazy
    default Optional<T> schema() {
      return Optional.ofNullable(customSchema()).or(this::currentSchema);
    }

    @Value.Lazy
    default Optional<T> currentSchema() {
      List<String> path = path();

      if (path.isEmpty()) {
        return Optional.ofNullable(mapping().getTargetSchema());
      }

      List<T> targetSchemas = mapping().getTargetSchemas(path);

      if (targetSchemas.isEmpty()) {
        //LOGGER.warn("No mapping found for path {}.", path);

        if (inGeometry()) {
          return mapping().getTargetSchema().getPrimaryGeometry();
        }

        return Optional.empty();
      }

      int schemaIndex = schemaIndex() > -1 ? schemaIndex() : targetSchemas.size() - 1;
      T targetSchema = targetSchemas.get(schemaIndex);

      return Optional.ofNullable(targetSchema);
    }

    @Value.Lazy
    default List<T> parentSchemas() {
      List<String> path = path();

      if (path.isEmpty()) {
        return ImmutableList.of();
      }

      List<List<T>> parentSchemas = mapping().getParentSchemas(path);

      if (parentSchemas.isEmpty()) {
        return ImmutableList.of();
      }

      int schemaIndex = schemaIndex() > -1 ? schemaIndex() : parentSchemas.size() - 1;
      return parentSchemas.get(schemaIndex);
    }

    @Value.Lazy
    default boolean isRequired() {
      return schema().filter(T::isRequired).isPresent();
    }
  }

  interface ModifiableContext<T extends SchemaBase<T>, U extends SchemaMappingBase<T>> extends Context<T, U> {

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
      //when tracking target paths, if present, use path separator from flatten transformation in mapping().getTargetSchema()
      Optional<String> pathSeparator = Optional.ofNullable(mapping())
          .flatMap(u -> u.getPathSeparator());

      FeaturePathTracker pathTracker = pathSeparator.isPresent()
          ? new FeaturePathTracker(pathSeparator.get())
          : new FeaturePathTracker();

      setPathTracker(pathTracker);

      return pathTracker;
    }

    @Value.Lazy
    @Override
    default List<String> path() {
      return pathTracker().asList();
    }

    @Value.Lazy
    @Override
    default String pathAsString() {
      return pathTracker().toStringWithDefaultSeparator();
    }

    @Value.Lazy
    default boolean shouldSkip() {
      return isBuffering()
          || currentSchema().isEmpty()
          || !shouldInclude(currentSchema().get(), parentSchemas(), pathTracker().toString());
    }

    private boolean shouldInclude(T schema,
        List<T> parentSchemas,
        String path) {
      return schema.isId()
          || (schema.isSpatial() && !query().skipGeometry())
          //TODO: enable if projected output needs to be schema valid
          // || isRequired(schema, parentSchemas)
          || (!schema.isId() && !schema.isSpatial() && propertyIsInFields(path));
    }

    default boolean propertyIsInFields(String property) {
      return query().getFields().isEmpty()
          || query().getFields().contains("*")
          || query().getFields().stream()
          .anyMatch(field -> field.startsWith(property));
    }

    default boolean isRequired(T schema, List<T> parentSchemas) {
      return schema.isRequired()
          && (parentSchemas.size() <= 1 || parentSchemas.stream()
          .limit(parentSchemas.size() - 1)
          .allMatch(T::isRequired));
    }

    ModifiableContext<T,U> setMetadata(ModifiableCollectionMetadata collectionMetadata);

    ModifiableContext<T,U> setPathTracker(FeaturePathTracker pathTracker);

    ModifiableContext<T,U> setGeometryType(SimpleFeatureGeometry geometryType);

    ModifiableContext<T,U> setGeometryType(Optional<SimpleFeatureGeometry> geometryType);

    ModifiableContext<T,U> setGeometryDimension(int geometryDimension);

    ModifiableContext<T,U> setGeometryDimension(OptionalInt geometryDimension);

    ModifiableContext<T,U> setValue(String value);

    ModifiableContext<T,U> setValueType(SchemaBase.Type valueType);

    ModifiableContext<T,U> putValueBuffer(String key, String value);

    ModifiableContext<T,U> setCustomSchema(T schema);

    ModifiableContext<T,U> setInGeometry(boolean inGeometry);

    ModifiableContext<T,U> setInObject(boolean inObject);

    ModifiableContext<T,U> setInArray(boolean inArray);

    ModifiableContext<T,U> setIndexes(Iterable<Integer> indexes);

    ModifiableContext<T,U> setQuery(FeatureQuery query);

    ModifiableContext<T,U> setMapping(U mapping);

    ModifiableContext<T,U> setSchemaIndex(int schemaIndex);

    ModifiableContext<T,U> putTransformed(String key, String value);

    ModifiableContext<T,U> setIsBuffering(boolean inArray);

    ModifiableContext<T,U> putAdditionalInfo(String key, String value);
  }

  //T createContext();

  void onStart(V context);

  void onEnd(V context);

  void onFeatureStart(V context);

  void onFeatureEnd(V context);

  void onObjectStart(V context);

  void onObjectEnd(V context);

  void onArrayStart(V context);

  void onArrayEnd(V context);

  void onValue(V context);
}
