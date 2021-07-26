package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTokenTransformerMapping extends FeatureTokenTransformer {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FeatureTokenTransformerMapping.class);

  private ModifiableContext newContext;
  private NestingTracker nestingTracker;

  @Override
  public void onStart(ModifiableContext context) {
    //TODO: slow, precompute, same for original in decoder
    SchemaMapping schemaMapping = SchemaMapping.withTargetPaths(getContext().mapping());
    this.newContext = createContext()
        .setMapping(schemaMapping)
        .setQuery(getContext().query())
        .setMetadata(getContext().metadata());
    this.nestingTracker = new NestingTracker(getDownstream(), newContext, ImmutableList.of());

    getDownstream().onStart(newContext);
  }

  @Override
  public void onEnd(ModifiableContext context) {
    getDownstream().onEnd(newContext);
  }

  @Override
  public void onFeatureStart(ModifiableContext context) {
    getDownstream().onFeatureStart(newContext);
  }

  @Override
  public void onFeatureEnd(ModifiableContext context) {
    while (nestingTracker.isNested()) {
      nestingTracker.close();
    }

    getDownstream().onFeatureEnd(newContext);
  }

  @Override
  public void onObjectStart(ModifiableContext context) {
    if (context.currentSchema()
        .filter(FeatureSchema::isGeometry)
        .isPresent()) {
      handleNesting(context.currentSchema().get(), context.parentSchemas(), context.indexes());

      newContext.pathTracker().track(context.currentSchema().get().getFullPath());
      newContext.setInGeometry(true);
      newContext.setGeometryType(context.geometryType());
      newContext.setGeometryDimension(context.geometryDimension());

      getDownstream().onObjectStart(newContext);
    }
    if (context.currentSchema()
        .filter(FeatureSchema::isObject)
        .isEmpty()) {
      return;
    }

    handleNesting(context.currentSchema().get(), context.parentSchemas(), context.indexes());
  }
  //TODO: geometry arrays
  @Override
  public void onObjectEnd(ModifiableContext context) {
    if (context.currentSchema()
        .filter(FeatureSchema::isGeometry)
        .isPresent()) {
      newContext.setInGeometry(false);
      newContext.setGeometryType(Optional.empty());
      newContext.setGeometryDimension(OptionalInt.empty());
      getDownstream().onObjectEnd(newContext);
    }
  }

  @Override
  public void onArrayStart(ModifiableContext context) {
    if (context.inGeometry()) {
      getDownstream().onArrayStart(newContext);
    }
    if (context.currentSchema()
        .filter(FeatureSchema::isArray)
        .isEmpty()) {
      return;
    }

    handleNesting(context.currentSchema().get(), context.parentSchemas(), context.indexes());
  }

  @Override
  public void onArrayEnd(ModifiableContext context) {
    if (context.inGeometry()) {
      getDownstream().onArrayEnd(newContext);
    }
  }

  @Override
  public void onValue(ModifiableContext context) {

    if (context.currentSchema().isPresent() && !context.inGeometry()) {
      handleNesting(context.currentSchema().get(), context.parentSchemas(), context.indexes());
      newContext.pathTracker().track(context.currentSchema().get().getFullPath());
    }

    newContext.setValue(context.value());
    newContext.setValueType(context.valueType());

    try {
      getDownstream().onValue(newContext);
    } catch (Throwable e) {
      throw e;
    }

  }

  private void handleNesting(FeatureSchema schema, List<FeatureSchema> parentSchemas,
      List<Integer> indexes) {

    while (nestingTracker.isNested() &&
        (nestingTracker.doesNotStartWithPreviousPath(schema.getFullPath()) ||
            (/*nestingTracker.inObject() && nestingTracker.isSamePath(schema.getFullPath()) ||*/
                (nestingTracker.inArray() && nestingTracker.isSamePath(schema.getFullPath())
                    && nestingTracker.hasParentIndexChanged(indexes))))) {
      nestingTracker.close();
    }

    if (nestingTracker.inObject() && newContext.inArray() && nestingTracker
        .doesStartWithPreviousPath(schema.getFullPath()) && nestingTracker
        .hasIndexChanged(indexes)) {
      nestingTracker.closeObject();
      newContext.setIndexes(indexes);
      nestingTracker.openObject();
    }

    if (schema.isArray() && /*nestingTracker.isFirst(indexes) &&*/ !nestingTracker
        .isSamePath(schema.getFullPath())) {
      openParents(parentSchemas, indexes);
      newContext.pathTracker().track(schema.getFullPath());
      nestingTracker.openArray();
    } else if (schema.isObject() && schema.isArray() && nestingTracker.isFirst(indexes)) {
      newContext.pathTracker().track(schema.getFullPath());
      newContext.setIndexes(indexes);
      nestingTracker.openObject();
    } else if (schema.isObject() && !schema.isArray() && !nestingTracker
        .isSamePath(schema.getFullPath())) {
      openParents(parentSchemas, indexes);
      newContext.pathTracker().track(schema.getFullPath());
      nestingTracker.openObject();
    } else if (schema.isValue() && (!schema.isArray() || nestingTracker.isFirst(indexes))) {
      openParents(parentSchemas, indexes);
    }
  }

  private void openParents(List<FeatureSchema> parentSchemas, List<Integer> indexes) {
    if (parentSchemas.isEmpty()) {
      return;
    }

    FeatureSchema parent = parentSchemas.get(0);

    if (parent.getSourcePath().isPresent()) {
      return;
    }

    if (parent.isArray()) {
      handleNesting(parent, parentSchemas.subList(1, parentSchemas.size()), indexes);
    }
    if (parent.isObject()) {
      handleNesting(parent, parentSchemas.subList(1, parentSchemas.size()), indexes);
    }
  }
}
