/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
NOTE: while this works, it is cumbersome and hard to maintain
 a much cleaner solution would use FeatureTokenEmitter and FeatureTokenReader for buffering
 but these are out of sync with Context and have to be adjusted (geoDim, in(Geo|Array|Object))
*/
public class FeatureTokenTransformerSorting extends FeatureTokenTransformer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureTokenTransformerSorting.class);

  private final Map<List<String>, Integer> pathIndex;
  private final Map<List<String>, Integer> rearrange;
  private final Queue<Integer> indexQueue;
  private final Queue<List<String>> pathQueue;
  private final Queue<Integer> schemaIndexQueue;
  private final Queue<List<Integer>> indexesQueue;
  private final Queue<String> valueQueue;
  private final Queue<Optional<SimpleFeatureGeometry>> geoTypeQueue;
  private final Queue<OptionalInt> geoDimQueue;
  private final Queue<Boolean> inGeoQueue;
  private final Queue<Boolean> inArrayQueue;
  private final Queue<Boolean> inObjectQueue;
  private final Queue<FeatureTokenType> tokenQueue;

  private int bufferIndex;
  private List<String> bufferParent;

  public FeatureTokenTransformerSorting() {
    this.pathIndex = new LinkedHashMap<>();
    this.rearrange = new LinkedHashMap<>();
    this.indexQueue = new LinkedList<>();
    this.pathQueue = new LinkedList<>();
    this.schemaIndexQueue = new LinkedList<>();
    this.indexesQueue = new LinkedList<>();
    this.valueQueue = new LinkedList<>();
    this.geoTypeQueue = new LinkedList<>();
    this.geoDimQueue = new LinkedList<>();
    this.inGeoQueue = new LinkedList<>();
    this.inArrayQueue = new LinkedList<>();
    this.inObjectQueue = new LinkedList<>();
    this.tokenQueue = new LinkedList<>();
    this.bufferIndex = 0;
    this.bufferParent = new ArrayList<>();
  }

  @Override
  public void onStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int index = 0;
    Set<List<String>> parents = new HashSet<>();
    List<String> lastParent = null;
    boolean doRearrange = false;

    for (List<String> path : context.mapping().getTargetSchemasByPath().keySet()) {
      if (path.size() > 1) {
        List<String> parent = path.subList(0, path.size() - 1);
        if (!doRearrange
            && !Objects.equals(parent, lastParent)
            && parents.contains(parent)
            && !path.get(path.size() - 1).startsWith("[")) {
          doRearrange = true;
        } else if (doRearrange
            && (!Objects.equals(parent, lastParent) || path.get(path.size() - 1).startsWith("["))) {
          doRearrange = false;
        }

        parents.add(parent);
        lastParent = parent;
        if (doRearrange) {
          rearrange.put(path, index);
        }
      }

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("{}: {}{}", index, doRearrange ? "QUEUE " : "", path);
      }

      pathIndex.put(path, index);
      index++;
    }

    super.onStart(context);
  }

  @Override
  public void onFeatureStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    super.onFeatureStart(context);
  }

  @Override
  public void onFeatureEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (bufferIndex > 0) {
      emptyBuffer(context, Integer.MAX_VALUE);
    }

    super.onFeatureEnd(context);
  }

  @Override
  public void onObjectStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int index = Objects.requireNonNullElse(pathIndex.get(context.path()), -1);

    checkBuffer(context, FeatureTokenType.OBJECT, index, index > bufferIndex);
  }

  @Override
  public void onObjectEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    boolean isBufferParent = startsWith(bufferParent, context.path());
    int triggerIndex = 0;
    if (isBufferParent) {
      triggerIndex = findLastStartsWith(context.path()) + 1;
    }

    checkBuffer(context, FeatureTokenType.OBJECT_END, triggerIndex, isBufferParent);
  }

  @Override
  public void onArrayStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int index = Objects.requireNonNullElse(pathIndex.get(context.path()), -1);

    checkBuffer(context, FeatureTokenType.ARRAY, index, index > bufferIndex);
  }

  @Override
  public void onArrayEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int index = Objects.requireNonNullElse(pathIndex.get(context.path()), -1);

    checkBuffer(context, FeatureTokenType.ARRAY_END, index, false);
  }

  @Override
  public void onValue(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int index = Objects.requireNonNullElse(pathIndex.get(context.path()), -1);

    checkBuffer(context, FeatureTokenType.VALUE, index, index > bufferIndex);
  }

  private void checkBuffer(
      ModifiableContext<FeatureSchema, SchemaMapping> context,
      FeatureTokenType token,
      int triggerIndex,
      boolean doEmptyBuffer) {
    int index = Objects.requireNonNullElse(pathIndex.get(context.path()), -1);
    boolean doRearrange = rearrange.containsKey(context.path());

    if (doRearrange) {
      buffer(context, index, token);
    } else {
      if (bufferIndex > 0 && doEmptyBuffer) {
        emptyBuffer(context, triggerIndex);
      }

      push(context, token);
    }
  }

  private static boolean startsWith(List<String> a, List<String> b) {
    return Objects.equals(a, b)
        || (a.size() > b.size() && Objects.equals(a.subList(0, b.size()), b));
  }

  private int findLastStartsWith(List<String> parent) {
    return pathIndex.entrySet().stream()
        .filter(entry -> startsWith(entry.getKey(), parent))
        .mapToInt(Entry::getValue)
        .max()
        .orElse(-1);
  }

  private void buffer(
      ModifiableContext<FeatureSchema, SchemaMapping> context, int index, FeatureTokenType token) {
    if (bufferIndex <= 0) {
      this.bufferIndex = index;
      this.bufferParent = context.path().subList(0, context.path().size() - 1);
    }

    indexQueue.add(index);
    tokenQueue.add(token);
    pathQueue.add(context.path());
    schemaIndexQueue.add(context.schemaIndex());
    indexesQueue.add(new ArrayList<>(context.indexes()));
    valueQueue.add(context.value());
    geoTypeQueue.add(context.geometryType());
    geoDimQueue.add(context.geometryDimension());
    inGeoQueue.add(context.inGeometry());
    inArrayQueue.add(context.inArray());
    inObjectQueue.add(context.inObject());
  }

  private void emptyBuffer(
      ModifiableContext<FeatureSchema, SchemaMapping> context, int triggerIndex) {

    List<String> path = context.path();
    ArrayList<Integer> indexes = new ArrayList<>(context.indexes());
    String value = context.value();
    Optional<SimpleFeatureGeometry> geometryType = context.geometryType();
    OptionalInt geometryDimension = context.geometryDimension();
    boolean inGeometry = context.inGeometry();
    boolean inArray = context.inArray();
    boolean inObject = context.inObject();

    while (!indexQueue.isEmpty() && indexQueue.peek() < triggerIndex) {
      int index = indexQueue.remove();
      FeatureTokenType token = tokenQueue.remove();

      context.pathTracker().track(pathQueue.remove());
      context.setSchemaIndex(schemaIndexQueue.remove());
      context.setIndexes(indexesQueue.remove());
      context.setValue(valueQueue.remove());
      context.setGeometryType(geoTypeQueue.remove());
      context.setGeometryDimension(geoDimQueue.remove());
      context.setInGeometry(inGeoQueue.remove());
      context.setInArray(inArrayQueue.remove());
      context.setInObject(inObjectQueue.remove());

      push(context, token);
    }

    context.pathTracker().track(path);
    context.setIndexes(indexes);
    context.setValue(value);
    context.setGeometryType(geometryType);
    context.setGeometryDimension(geometryDimension);
    context.setInGeometry(inGeometry);
    context.setInArray(inArray);
    context.setInObject(inObject);

    this.bufferIndex = Objects.requireNonNullElse(indexQueue.peek(), 0);
    this.bufferParent =
        Objects.nonNull(pathQueue.peek()) && pathQueue.peek().size() > 1
            ? pathQueue.peek().subList(0, pathQueue.peek().size() - 1)
            : new ArrayList<>();
  }

  private void push(
      ModifiableContext<FeatureSchema, SchemaMapping> context, FeatureTokenType token) {
    switch (token) {
      case VALUE:
        super.onValue(context);
        break;
      case OBJECT:
        super.onObjectStart(context);
        break;
      case OBJECT_END:
        super.onObjectEnd(context);
        break;
      case ARRAY:
        super.onArrayStart(context);
        break;
      case ARRAY_END:
        super.onArrayEnd(context);
        break;
    }
  }
}
