package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WithTransformationsApplied implements
    SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private final PropertyTransformations additionalTransformations;

  public WithTransformationsApplied() {
    this(new LinkedHashMap<>());
  }

  public WithTransformationsApplied(
      Map<String, PropertyTransformation> additionalTransformations) {
    this.additionalTransformations = () -> additionalTransformations.entrySet()
    .stream()
    .map(entry -> new SimpleEntry<>(entry.getKey(), ImmutableList.of(entry.getValue())))
    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public FeatureSchema visit(FeatureSchema schema, List<FeatureSchema> parents,
      List<FeatureSchema> visitedProperties) {

    if (parents.isEmpty()) {
      Optional<PropertyTransformation> flatten = getFeatureTransformations(schema)
          .filter(transformation -> transformation.getFlatten().isPresent());

      if (flatten.isPresent()) {
        String separator = flatten.get().getFlatten().get();

        /*Map<String, FeatureSchema> flatProperties = schema.getAllNestedProperties()
            .stream()
            .filter(FeatureSchema::isValue)
            .map(property -> new SimpleEntry<>(String.join(separator, property.getFullPath()), new ImmutableFeatureSchema.Builder()
                .from(property)
                .name(String.join(separator, property.getFullPath()))
                .build()))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));*/

        Map<String, FeatureSchema> flatProperties = flattenProperties(schema.getProperties(), null, separator)
            .stream()
            .map(property -> new SimpleEntry<>(property.getName(), property))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

        return new ImmutableFeatureSchema.Builder()
            .from(schema)
            .propertyMap(flatProperties)
            .build();
      }
    }

    return schema;
  }

  private List<FeatureSchema> flattenProperties(List<FeatureSchema> properties, String parent, String separator) {
    String prefix = Objects.nonNull(parent)
        ? parent + separator
        : "";

    return properties.stream()
        .flatMap(property -> property.isObject()
            ? flattenProperties(property.getProperties(), flatName(property, prefix), separator).stream()
            : Stream.of(flattenProperty(property, prefix)))
        .collect(Collectors.toList());
  }

  private FeatureSchema flattenProperty(FeatureSchema property, String prefix) {
    return new ImmutableFeatureSchema.Builder()
        .from(property)
        .type(property.getValueType().orElse(property.getType()))
        .valueType(Optional.empty())
        .name(flatName(property, prefix))
        .build();
  }

  private String flatName(FeatureSchema property, String prefix) {
    return prefix + property.getName() + (property.isArray() ? "[]" : "");
  }

  private Optional<PropertyTransformation> getFeatureTransformations(FeatureSchema schema) {
    PropertyTransformations schemaTransformations = () -> ImmutableMap.of(PropertyTransformations.WILDCARD, schema.getTransformations());

    List<PropertyTransformation> featureTransformations = additionalTransformations
        .mergeInto(schemaTransformations)
        .getTransformations()
        .get(PropertyTransformations.WILDCARD);

    return Optional.ofNullable(featureTransformations).filter(list -> !list.isEmpty()).map(list -> list.get(0));
  }
}
