/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.transform.FeatureEventBuffer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class NestingTracker2 {

  private static final Logger LOGGER = LoggerFactory.getLogger(NestingTracker2.class);

  private final FeatureEventBuffer<
          FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      buffer;
  private final FeatureTokenEmitter2<
          FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      downstream;
  // private final ModifiableContext<?, ?> context;
  // private final List<List<String>> mainPaths;
  // private final boolean flattenObjects;
  // private final boolean flattenArrays;
  private final List<String> nestingStack;
  private final List<List<String>> pathStack;
  private final List<List<String>> payloadStack;
  private final List<List<String>> payloadStack2;
  private final List<List<Integer>> indexStack;
  private final List<String> flattened;
  // private final boolean skippable;

  public NestingTracker2(
      FeatureEventBuffer<
              FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
          downstream) {
    this.buffer = downstream;
    this.downstream = downstream.getBuffer();
    // this.context = context;
    // this.mainPaths = mainPaths;
    // this.flattenObjects = flattenObjects;
    // this.flattenArrays = flattenArrays;
    // this.skippable = skippable;
    this.nestingStack = new ArrayList<>();
    this.pathStack = new ArrayList<>();
    this.payloadStack = new ArrayList<>();
    this.payloadStack2 = new ArrayList<>();
    this.indexStack = new ArrayList<>();
    this.flattened = new ArrayList<>();
  }

  public void open(FeatureSchema schema, ModifiableContext<FeatureSchema, SchemaMapping> context) {
    open(schema, context.parentSchemas(), context);
  }

  public void open(
      FeatureSchema schema,
      List<FeatureSchema> parentSchemas,
      ModifiableContext<FeatureSchema, SchemaMapping> context) {
    List<String> payload = context.path();
    SchemaMapping mapping = context.mapping();
    // TODO
    List<Integer> indexes = context.indexes();

    // new higher level property or new object in array???
    while (isNested()
        && (doesNotStartWithPreviousPath(schema.getFullPath())
        /*TODO || (inArray()
        && isSamePath(schema.getFullPath())
        && hasParentIndexChanged(indexes))*/ )) {

      if (inObject()) {
        closeObject(mapping);
      } else if (inArray()) {
        closeArray(mapping);
      }
    }

    // new object in array???
    if (isObjectInArray()
        && isSamePath(schema.getFullPath())
        && (hasIndexChanged(indexes) || hasSourceChanged(payload))) {
      closeObject(mapping);
      // newContext.setIndexes(indexes);
      openObject(schema, payload, indexes, context.geometryType(), mapping);
    } /*else if (newContext.transformed().containsKey("concatNewObject")) {
        newContext.transformed().remove("concatNewObject");
        closeObject();
        openObject(parentSchemas.get(0));
      }*/

    // new array
    if (schema.isArray() && !isSamePath(schema.getFullPath())) {
      openParents(parentSchemas, context);
      // newContext.pathTracker().track(schema.getFullPath());
      openArray(schema, payload, indexes, mapping);
      // first object in array???
    } else if (schema.isObject() && schema.isArray() && isFirst(indexes)) {
      // newContext.pathTracker().track(schema.getFullPath());
      // newContext.setIndexes(indexes);
      openObject(schema, payload, indexes, context.geometryType(), mapping);
      // new object
    } else if (schema.isObject() && !schema.isArray() && !isSamePath(schema.getFullPath())) {
      openParents(parentSchemas, context);
      // newContext.pathTracker().track(schema.getFullPath());
      openObject(schema, payload, indexes, context.geometryType(), mapping);
      // new value or value array
    } else if (schema.isValue() && (!schema.isArray() || isFirst(indexes))) {
      openParents(parentSchemas, context);
    }

    // value array entry
    if (schema.isValue() && schema.isArray()) {
      // newContext.setIndexes(indexes);
    }
  }

  private void openParents(
      List<FeatureSchema> parentSchemas, ModifiableContext<FeatureSchema, SchemaMapping> context) {
    // parent is feature
    if (parentSchemas.size() < 2) {
      return;
    }

    List<String> payload = context.path();
    SchemaMapping mapping = context.mapping();
    FeatureSchema parent = parentSchemas.get(0);
    List<Integer> indexes = context.indexes();

    // parent already handled by onObject/onArray
    if (parent.getSourcePath().isPresent()) {
      return;
    }

    if (parent.isArray()
        && parent.isObject()
        && isObjectInArray()
        && isSamePath(parent.getFullPath())
        && (hasIndexChanged(indexes) || hasSourceChanged(payload))) {
      closeObject(mapping);
      // newContext.setIndexes(indexes);
      openObject(parent, payload, indexes, context.geometryType(), mapping);
    }

    List<Integer> newIndexes = new ArrayList<>(); // newContext.indexes());
    /*List<List<String>> arrays = new ArrayList<>();

    for (int i = parentSchemas.size() - 1; i >= 0; i--) {
      FeatureSchema schema = parentSchemas.get(i);

      if (schema.getType() == Type.OBJECT_ARRAY && schema.getSourcePath().isEmpty()) {
        arrays.add(schema.getFullPath());
        if (!indexedArrays.contains(schema.getFullPath())) {
          indexedArrays.add(schema.getFullPath());
          newIndexes.add(1);
          newContext.setIndexes(newIndexes);
        }
      }
    }

    indexedArrays.removeIf(strings -> !arrays.contains(strings));
    openedArrays.removeIf(strings -> !arrays.contains(strings));*/

    if (parent.isArray()) {
      if (! /*openedArrays*/pathStack.contains(parent.getFullPath())) {
        open(parent, parentSchemas.subList(1, parentSchemas.size()), context);
        if (parent.isObject()) {
          open(parent, parentSchemas.subList(1, parentSchemas.size()), context);
        }
        // openedArrays.add(parent.getFullPath());
      }
    } else if (parent.isObject()) {
      open(parent, parentSchemas.subList(1, parentSchemas.size()), context);
    }
  }

  public void openArray(
      FeatureSchema schema, List<String> payload, List<Integer> indexes, SchemaMapping mapping) {
    /*if (flattenArrays) {
      flattened.add(context.currentSchema().get().getName());
    } else {
      if (!skippable || !context.shouldSkip()) {
        downstream.onArrayStart(context);
      }
    }*/

    if (schema.getSourcePath().isEmpty()) {

      List<String> prev2 = payload.size() > 1 ? payload.subList(0, payload.size() - 1) : payload;

      List<String> sourcePath =
          mapping.getSchemasBySourcePath().entrySet().stream()
              .filter(entry -> entry.getValue().contains(schema))
              .map(Entry::getKey)
              .findFirst()
              .orElseThrow();

      List<Integer> positionsForSourcePath = mapping.getPositionsForSourcePath(sourcePath);
      List<List<Integer>> parentPositionsForSourcePath =
          mapping.getParentPositionsForSourcePath(sourcePath);

      int prev = buffer.current;
      List<Integer> prevEnc = buffer.currentEnclosing;

      buffer.next(positionsForSourcePath.get(0), parentPositionsForSourcePath.get(0));

      downstream.onArrayStart(schema.getFullPath());

      push("A", schema.getFullPath(), prev2, sourcePath, indexes);

      buffer.next(prev, prevEnc);
    } else {
      downstream.onArrayStart(schema.getFullPath());

      push("A", schema.getFullPath(), payload, indexes);
    }
    // context.setInArray(true);
  }

  public void openObject(
      FeatureSchema schema,
      List<String> payload,
      List<Integer> indexes,
      Optional<SimpleFeatureGeometry> geometryType,
      SchemaMapping mapping) {
    /*if (flattenArrays && inArray()) {
      flattened.add(String.valueOf(context.index()));
    } else if (flattenObjects && (flattenArrays || !inArray())) {
      flattened.add(context.currentSchema().get().getName());
    } else {
      if (!skippable || !context.shouldSkip()) {
        downstream.onObjectStart(context);
      }
    }*/
    // TODO: make empty source paths easily retrievable from mapping
    if (schema.getSourcePath().isEmpty()) {

      List<String> prev2 = payload.size() > 1 ? payload.subList(0, payload.size() - 1) : payload;

      List<String> sourcePath =
          mapping.getSchemasBySourcePath().entrySet().stream()
              .filter(entry -> entry.getValue().contains(schema))
              .map(Entry::getKey)
              .findFirst()
              .orElseThrow();

      List<Integer> positionsForSourcePath = mapping.getPositionsForSourcePath(sourcePath);
      List<List<Integer>> parentPositionsForSourcePath =
          mapping.getParentPositionsForSourcePath(sourcePath);

      int prev = buffer.current;
      List<Integer> prevEnc = buffer.currentEnclosing;

      buffer.next(positionsForSourcePath.get(0), parentPositionsForSourcePath.get(0));

      downstream.onObjectStart(schema.getFullPath(), geometryType, OptionalInt.empty());

      push("O", schema.getFullPath(), prev2, sourcePath, indexes);

      buffer.next(prev, prevEnc);
    } else {
      downstream.onObjectStart(schema.getFullPath(), geometryType, OptionalInt.empty());

      push("O", schema.getFullPath(), payload, indexes);
    }
    // context.setInObject(true);
  }

  public void closeObject(SchemaMapping mapping) {
    if (nestingStack.isEmpty() || !Objects.equals(nestingStack.get(nestingStack.size() - 1), "O")) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("No object to close");
      }
      return;
    }

    // context.pathTracker().track(getCurrentNestingPath());

    /*if (flattenArrays && isObjectInArray()) {
      flattened.remove(flattened.size() - 1);
    } else if (flattenObjects && (flattenArrays || !isObjectInArray())) {
      flattened.remove(flattened.size() - 1);
    } else {
      if (!skippable || !context.shouldSkip()) {
        downstream.onObjectEnd(context);
      }
    }*/

    List<String> sourcePath = getCurrentNestingPayload2();

    List<Integer> positionsForSourcePath = mapping.getPositionsForSourcePath(sourcePath);
    List<List<Integer>> parentPositionsForSourcePath =
        mapping.getParentPositionsForSourcePath(sourcePath);

    int prev = buffer.current;
    List<Integer> prevEnc = buffer.currentEnclosing;

    buffer.next(positionsForSourcePath.get(0), parentPositionsForSourcePath.get(0));

    downstream.onObjectEnd(getCurrentNestingPath());

    pop();

    buffer.next(prev, prevEnc);

    /*if (!nestingStack.contains("O")) {
      context.setInObject(false);
    }
    if (!pathStack.isEmpty()) {
      context.pathTracker().track(getCurrentNestingPath());
    } else {
      context.pathTracker().track(ImmutableList.of());
    }*/
  }

  public void closeArray(SchemaMapping mapping) {
    if (nestingStack.isEmpty() || !Objects.equals(nestingStack.get(nestingStack.size() - 1), "A")) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("No array to close");
      }
      return;
    }

    // context.pathTracker().track(getCurrentNestingPath());
    /*if (flattenArrays) {
      flattened.remove(flattened.size() - 1);
    } else {
      if (!skippable || !context.shouldSkip()) {
        downstream.onArrayEnd(context);
      }
    }*/

    List<String> sourcePath = getCurrentNestingPayload2();

    List<Integer> positionsForSourcePath = mapping.getPositionsForSourcePath(sourcePath);
    List<List<Integer>> parentPositionsForSourcePath =
        mapping.getParentPositionsForSourcePath(sourcePath);

    int prev = buffer.current;
    List<Integer> prevEnc = buffer.currentEnclosing;

    buffer.next(positionsForSourcePath.get(0), parentPositionsForSourcePath.get(0));

    downstream.onArrayEnd(getCurrentNestingPath());

    pop();

    buffer.next(prev, prevEnc);

    /*if (!nestingStack.contains("A")) {
      context.setInArray(false);
    }
    if (!pathStack.isEmpty()) {
      context.pathTracker().track(getCurrentNestingPath());
    }*/
  }

  public int arrayDepth() {
    return (int) nestingStack.stream().filter("A"::equals).count();
  }

  /*public void close() {
    if (nestingStack.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("No object or array to close");
      }
      return;
    }
    if (inObject()) {
      closeObject(mapping);
    } else if (inArray()) {
      closeArray();
    }
  }*/

  private void push(String type, List<String> path, List<String> payload, List<Integer> indexes) {
    push(type, path, payload, payload, indexes);
  }

  private void push(
      String type,
      List<String> path,
      List<String> payload,
      List<String> payload2,
      List<Integer> indexes) {
    nestingStack.add(type);
    pathStack.add(path);
    payloadStack.add(payload);
    payloadStack2.add(payload2);
    indexStack.add(new ArrayList<>(indexes));
  }

  private void pop() {
    nestingStack.remove(nestingStack.size() - 1);
    pathStack.remove(pathStack.size() - 1);
    payloadStack.remove(payloadStack.size() - 1);
    payloadStack2.remove(payloadStack2.size() - 1);
    indexStack.remove(indexStack.size() - 1);
  }

  public List<String> getCurrentNestingPath() {
    if (pathStack.isEmpty()) {
      return null;
    }
    return pathStack.get(pathStack.size() - 1);
  }

  public List<String> getCurrentNestingPayload() {
    if (payloadStack.isEmpty()) {
      return null;
    }
    return payloadStack.get(payloadStack.size() - 1);
  }

  public List<String> getCurrentNestingPayload2() {
    if (payloadStack2.isEmpty()) {
      return null;
    }
    return payloadStack2.get(payloadStack2.size() - 1);
  }

  public List<Integer> getCurrentNestingIndexes() {
    if (indexStack.isEmpty()) {
      return null;
    }
    return indexStack.get(indexStack.size() - 1);
  }

  public boolean isNested() {
    return Objects.nonNull(getCurrentNestingPath());
  }

  public boolean inArray() {
    return isNested() && Objects.equals(nestingStack.get(nestingStack.size() - 1), "A");
  }

  public boolean inObject() {
    return isNested() && Objects.equals(nestingStack.get(nestingStack.size() - 1), "O");
  }

  public boolean isObjectInArray() {
    return inObject()
        && nestingStack.size() > 1
        && Objects.equals(nestingStack.get(nestingStack.size() - 2), "A");
  }

  /*public boolean isNotMain(List<String> nextPath) {
    return !mainPaths.contains(nextPath);
  }*/

  public boolean isFirst(List<Integer> indexes) {
    return indexes.size() > 0 && indexes.get(indexes.size() - 1) == 1;
  }

  public boolean isSamePath(List<String> nextPath) {
    return Objects.equals(getCurrentNestingPath(), nextPath);
  }

  public boolean doesNotStartWithPreviousPath(List<String> nextPath) {
    return !startsWith(nextPath, getCurrentNestingPath());
  }

  public boolean doesStartWithPreviousPath(List<String> nextPath) {
    if (Objects.equals(nextPath, getCurrentNestingPath())) {
      return false;
    }
    return startsWith(nextPath, getCurrentNestingPath());
  }

  public boolean hasIndexChanged(List<Integer> nextIndexes) {
    return !startsWith(nextIndexes, getCurrentNestingIndexes());
  }

  /*public boolean hasParentIndexChanged(List<Integer> nextIndexes) {
    return nextIndexes.size() > 1
        && context.indexes().size() >= nextIndexes.size() - 1
        && nextIndexes.get(nextIndexes.size() - 2) > context.indexes().get(nextIndexes.size() - 2);
  }*/

  public boolean hasSourceChanged(List<String> nextSource) {
    List<String> source = getCurrentNestingPayload();
    // List<String> prev = source.size() > 1 ? source.subList(0, source.size() - 1) : source;
    return !startsWith(nextSource, source);
  }

  private static <T> boolean startsWith(List<T> longer, List<T> shorter) {
    if (Objects.isNull(longer) || Objects.isNull(shorter) || longer.size() < shorter.size()) {
      return false;
    }

    return Objects.equals(longer.subList(0, shorter.size()), shorter);
  }

  /*public String getFlattenedPropertyPath(String separator, String name) {
    if (inArray() && !context.indexes().isEmpty()) {
      flattened.add(String.valueOf(context.index()));
    } else {
      flattened.add(name);
    }
    String path = Joiner.on(separator).join(flattened);
    flattened.remove(flattened.size() - 1);
    return path;
  }*/
}
