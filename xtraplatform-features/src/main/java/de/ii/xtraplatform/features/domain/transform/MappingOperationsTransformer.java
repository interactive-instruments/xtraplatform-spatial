/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingOperationsTransformer implements ContextTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(MappingOperationsTransformer.class);

  private final Set<String> coalesces;
  private String concatPath;
  private int concatIndex;
  private int concatArrayIndex;
  private int concatItem;
  private String coalescePath;
  private String coalesceIndex;

  public MappingOperationsTransformer() {
    this.coalesces = new HashSet<>();
  }

  @Override
  public void reset() {
    this.concatPath = null;
    this.coalescePath = null;
    coalesces.clear();
  }

  @Override
  public boolean transform(
      ModifiableContext<FeatureSchema, SchemaMapping> context,
      ModifiableContext<FeatureSchema, SchemaMapping> newContext) {
    if (context.schema().isEmpty()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "PATH NOT FOUND {} {}",
            context.pathAsString(),
            Objects.nonNull(context.mapping())
                ? context.mapping().getTargetSchemasByPath().keySet()
                : "{}");
      }
      return false;
    }

    FeatureSchema schema = context.schema().get();

    if (schema.getEffectiveSourcePaths().size() > 1) {
      int index = getIndex(context.path(), schema.getEffectiveSourcePaths());

      if (!schema.getConcat().isEmpty()) {
        return valueConcat(schema, index, context, newContext);
      } else if (!schema.getCoalesce().isEmpty()) {
        return valueCoalesce(schema, index, context);
      }
    }

    if (schema.getAdditionalInfo().containsKey("concatIndex")) {
      return objectConcat(schema, context, newContext);
    } else if (schema.getAdditionalInfo().containsKey("coalesceIndex")) {
      return objectCoalesce(schema, context);
    }

    return true;
  }

  private int getIndex(List<String> path, List<String> sourcePaths) {
    String column = path.get(path.size() - 1);
    for (String sourcePath : sourcePaths) {
      if (String.join("/", path).endsWith(sourcePath)) {
        column = sourcePath;
      }
    }
    return sourcePaths.indexOf(column);
  }

  private boolean valueConcat(
      FeatureSchema schema,
      int index,
      ModifiableContext<FeatureSchema, SchemaMapping> context,
      ModifiableContext<FeatureSchema, SchemaMapping> newContext) {
    if (Objects.nonNull(context.value())) {
      if (!Objects.equals(concatPath, schema.getFullPathAsString())) {
        this.concatPath = schema.getFullPathAsString();
        this.concatItem = 0;
      }

      List<Integer> indexes =
          new ArrayList<>(
              schema.getConcat().get(index).isArray()
                  ? context.indexes().subList(0, Math.max(0, context.indexes().size() - 1))
                  : context.indexes());
      indexes.add(++concatItem);

      context.setIndexes(indexes);
    }
    if (schema.getConcat().size() > index) {
      context.setValueType(
          schema
              .getConcat()
              .get(index)
              .getValueType()
              .orElse(schema.getConcat().get(index).getType()));
      newContext.putValueBuffer(
          "type", schema.getConcat().get(index).getRefType().orElse("UNKNOWN"));
    }

    return true;
  }

  private boolean valueCoalesce(
      FeatureSchema schema, int index, ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (Objects.nonNull(context.value()) && !coalesces.contains(schema.getFullPathAsString())) {
      this.coalesces.add(schema.getFullPathAsString());

      if (schema.getCoalesce().size() > index) {
        context.setValueType(schema.getCoalesce().get(index).getType());
      }
    } else {
      return false;
    }

    return true;
  }

  private boolean objectConcat(
      FeatureSchema schema,
      ModifiableContext<FeatureSchema, SchemaMapping> context,
      ModifiableContext<FeatureSchema, SchemaMapping> newContext) {
    int index = Integer.parseInt(schema.getAdditionalInfo().get("concatIndex"));
    int arrayIndex =
        schema.getAdditionalInfo().containsKey("concatArray")
            ? Math.max(0, (int) context.index() - 1)
            : 0;

    if (!Objects.equals(concatPath, context.parentSchemas().get(0).getFullPathAsString())) {
      this.concatPath = context.parentSchemas().get(0).getFullPathAsString();
      this.concatItem = 0;
      this.concatIndex = -1;
      this.concatArrayIndex = -1;
    }

    if (concatIndex < index || concatArrayIndex < arrayIndex) {
      /*List<Integer> indexes =
          new ArrayList<>(
              newContext.indexes().subList(0, Math.max(0, newContext.indexes().size() - 1)));
      indexes.add(newContext.indexes().get(newContext.indexes().size() - 1) + 1);

      closeObject();
      newContext.setIndexes(indexes);
      FeatureSchema parentSchema = context.parentSchemas().get(0);
      openObject(parentSchema);*/
      /*List<Integer> indexes =
          new ArrayList<>(
              concatItem > 0
                  ? context.indexes().subList(0, Math.max(0, context.indexes().size() - 1))
                  : context.indexes());
      indexes.add(++concatItem);

      LOGGER.debug("INDEXES {} {}", context.indexes(), indexes);
      context.setIndexes(indexes);*/
      if (++concatItem > 1) {
        // newContext.setIndexes(indexes);
        List<Integer> indexes =
            new ArrayList<>(
                newContext.indexes().subList(0, Math.max(0, newContext.indexes().size() - 1)));
        indexes.add(newContext.indexes().get(newContext.indexes().size() - 1) + 1);

        newContext.setIndexes(indexes);
        newContext.putTransformed("concatNewObject", "true");
      }
    }

    this.concatIndex = index;
    this.concatArrayIndex = arrayIndex;

    return true;
  }

  private boolean objectCoalesce(
      FeatureSchema schema, ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int arrayIndex =
        schema.getAdditionalInfo().containsKey("coalesceArray")
            ? Math.max(0, (int) context.index() - 1)
            : 0;
    if (Objects.nonNull(context.value())
        && !Objects.equals(coalescePath, schema.getFullPathAsString())) {
      this.coalescePath = schema.getParentPath().toString();
      this.coalesceIndex = schema.getAdditionalInfo().get("coalesceIndex") + arrayIndex;
    } else if (!Objects.equals(
        schema.getAdditionalInfo().get("coalesceIndex") + arrayIndex, coalesceIndex)) {
      return false;
    }

    return true;
  }
}
