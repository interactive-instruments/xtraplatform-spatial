package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.PropertyBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class FeatureObjectTransformerBase<T extends SchemaBase<T>, U extends SchemaMappingBase<T>, V extends PropertyBase<V, T>, W extends FeatureBase<V, T>> extends
    FeatureTokenTransformerBase<T, U, ModifiableContext<T, U>> {
  private W currentFeature;
  private V currentObjectOrArray;

  public abstract W createFeature();

  public abstract V createProperty();

  public abstract void onFeature(W feature);

  @Override
  public void onStart(ModifiableContext<T,U> context) {

  }

  @Override
  public void onEnd(ModifiableContext<T,U> context) {

  }

  @Override
  public final void onFeatureStart(ModifiableContext<T,U> context) {
    if (context.schema().isEmpty()) {
      return;
    }

    this.currentFeature = createFeature();
    currentFeature.schema(context.schema().get());

    currentFeature.collectionMetadata(context.metadata());

    this.currentObjectOrArray = null;
  }

  @Override
  public final void onFeatureEnd(ModifiableContext<T,U> context) {
    onFeature(currentFeature);

    this.currentFeature = null;
    this.currentObjectOrArray = null;
  }

  //TODO: if no matching name is found in schema, when inGeometry use primary
  @Override
  public final void onObjectStart(ModifiableContext<T,U> context) {
    if (context.schema().isEmpty()) {
      return;
    }

    this.currentObjectOrArray = createProperty(PropertyBase.Type.OBJECT, context.path(), context.schema().get(), context.geometryType().orElse(null));
  }

  @Override
  public final void onObjectEnd(ModifiableContext<T,U> context) {
    this.currentObjectOrArray = getCurrentParent();
  }

  @Override
  public final void onArrayStart(ModifiableContext<T,U> context) {
    if (context.schema().isEmpty()) {
      return;
    }

    this.currentObjectOrArray = createProperty(PropertyBase.Type.ARRAY, context.path(), context.schema().get());
  }

  @Override
  public final void onArrayEnd(ModifiableContext<T,U> context) {
    this.currentObjectOrArray = getCurrentParent();
  }

  @Override
  public final void onValue(ModifiableContext<T,U> context) {
    if (context.schema().isEmpty() || Objects.isNull(context.value())) {
      return;
    }

    createProperty(PropertyBase.Type.VALUE, context.path(), context.schema().get(), context.value(), context.transformed());
  }

  private V createProperty(Property.Type type, List<String> path, T schema) {
    return createProperty(type, path, schema, null, null, ImmutableMap.of());
  }

  private V createProperty(Property.Type type, List<String> path, T schema, SimpleFeatureGeometry geometryType) {
    return createProperty(type, path, schema, null, geometryType, ImmutableMap.of());
  }

  private V createProperty(Type type, List<String> path, T schema,
      String value, Map<String, String> transformed) {
    return createProperty(type, path, schema, value, null, transformed);
  }

  private V createProperty(Type type, List<String> path, T schema,
      String value, SimpleFeatureGeometry geometryType, Map<String, String> transformed) {

    /*return currentFeature.getProperties()
        .stream()
        .filter(t -> t.getType() == type && t.getSchema().isPresent() && Objects
            .equals(t.getSchema().get(), schema) && !t.getSchema().get().isGeometry())
        .findFirst()
        .orElseGet(() -> {*/
    V property = createProperty();
    property.type(type)
        .schema(schema)
        .propertyPath(path)
        .value(value)
        .geometryType(Optional.ofNullable(geometryType))
        .transformed(transformed);

    if (Objects.nonNull(currentObjectOrArray)) {
      property.parent(currentObjectOrArray);
      currentObjectOrArray.addNestedProperties(property);
    } else {
      currentFeature.addProperties(property);
    }

    return property;
    //});
  }

  private V getCurrentParent() {
    return Objects.nonNull(currentObjectOrArray) && currentObjectOrArray.getParent()
        .isPresent()
        ? currentObjectOrArray.getParent()
        .get()
        : null;
  }
}
