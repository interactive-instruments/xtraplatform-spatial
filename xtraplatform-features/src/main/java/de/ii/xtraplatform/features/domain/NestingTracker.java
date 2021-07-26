/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.base.Joiner;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class NestingTracker {

  private static final Logger LOGGER = LoggerFactory.getLogger(NestingTracker.class);

  private final FeatureEventHandler<ModifiableContext> downstream;
  private final ModifiableContext context;
  private final List<String> mainPath;
  private final boolean flattenObjects;
  private final boolean flattenArrays;
  private final List<String> nestingStack;
  private final List<List<String>> pathStack;
  private final List<String> flattened;

  public NestingTracker(FeatureEventHandler<ModifiableContext> downstream,
      ModifiableContext context, List<String> mainPath,
      boolean flattenObjects, boolean flattenArrays) {
    this.downstream = downstream;
    this.context = context;
    this.mainPath = mainPath;
    this.flattenObjects = flattenObjects;
    this.flattenArrays = flattenArrays;
    this.nestingStack = new ArrayList<>();
    this.pathStack = new ArrayList<>();
    this.flattened = new ArrayList<>();
  }

  public void openArray() {
    if (!flattenArrays) {
      downstream.onArrayStart(context);
    } else {
      flattened.add(context.schema().get().getName());
    }
    context.setInArray(true);
    nestingStack.add("A");
    pathStack.add(context.pathTracker().asList());
  }

  public void openObject() {
    if (flattenArrays && inArray()) {
      flattened.add(String.valueOf(context.index()));
    } else if (!flattenObjects || (!flattenArrays && inArray())) {
      downstream.onObjectStart(context);
    } else{
      flattened.add(context.schema().get().getName());
    }
    context.setInObject(true);
    nestingStack.add("O");
    pathStack.add(context.pathTracker().asList());
  }

  public void closeObject() {
    if (nestingStack.isEmpty() || !Objects.equals(nestingStack.get(nestingStack.size() - 1), "O")) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("No object to close");
      }
      return;
    }
    context.pathTracker().track(getCurrentNestingPath());
    if (flattenArrays && isObjectInArray()) {
      flattened.remove(flattened.size()-1);
    } else if (!flattenObjects || (!flattenArrays && isObjectInArray())) {
      downstream.onObjectEnd(context);
    } else {
      flattened.remove(flattened.size()-1);
    }
    nestingStack.remove(nestingStack.size() - 1);
    pathStack.remove(pathStack.size() - 1);
    if (!nestingStack.contains("O")) {
      context.setInObject(false);
    }
    if (!pathStack.isEmpty()) {
      context.pathTracker().track(getCurrentNestingPath());
    }
  }

  public void closeArray() {
    if (nestingStack.isEmpty() || !Objects.equals(nestingStack.get(nestingStack.size() - 1), "A")) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("No array to close");
      }
      return;
    }
    context.pathTracker().track(getCurrentNestingPath());
    if (!flattenArrays) {
      downstream.onArrayEnd(context);
    } else {
      flattened.remove(flattened.size()-1);
    }
    nestingStack.remove(nestingStack.size() - 1);
    pathStack.remove(pathStack.size() - 1);
    if (!nestingStack.contains("A")) {
      context.setInArray(false);
    }
    if (!pathStack.isEmpty()) {
      context.pathTracker().track(getCurrentNestingPath());
    }
  }

  public void close() {
    if (nestingStack.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("No object or array to close");
      }
      return;
    }
    if (Objects.equals(nestingStack.get(nestingStack.size() - 1), "O")) {
      closeObject();
    } else if (Objects.equals(nestingStack.get(nestingStack.size() - 1), "A")) {
      closeArray();
    }
  }

  public List<String> getCurrentNestingPath() {
    if (pathStack.isEmpty()) {
      return null;
    }
    return pathStack.get(pathStack.size() - 1);
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
    return inObject() && nestingStack.size() > 1 && Objects.equals(nestingStack.get(nestingStack.size() - 2), "A");
  }

  public boolean isNotMain(List<String> nextPath) {
    return !Objects.equals(nextPath, mainPath);
  }

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
    return !startsWith(nextIndexes, context.indexes());
  }

  public boolean hasParentIndexChanged(List<Integer> nextIndexes) {
    return nextIndexes.size() > 1 && context.indexes().size() >= nextIndexes.size() - 1 &&
        nextIndexes.get(nextIndexes.size() - 2) > context.indexes().get(nextIndexes.size() - 2);
  }

  private static <T> boolean startsWith(List<T> longer, List<T> shorter) {
    if (Objects.isNull(longer) || Objects.isNull(shorter) || longer.size() < shorter.size()) {
      return false;
    }

    return Objects.equals(longer.subList(0, shorter.size()), shorter);
  }

  public String getFlattenedPropertyPath(String separator, String name) {
    flattened.add(name);
    String path = Joiner.on(separator).join(flattened);
    flattened.remove(flattened.size()-1);
    return path;
  }
}
