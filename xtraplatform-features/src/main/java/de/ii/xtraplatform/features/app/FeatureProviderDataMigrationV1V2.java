package de.ii.xtraplatform.features.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.event.store.EntityMigration;
import de.ii.xtraplatform.event.store.Identifier;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeaturePropertyV2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.FeatureTypeV2;
import de.ii.xtraplatform.features.domain.ImmutableFeaturePropertyV2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureTypeV2;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class FeatureProviderDataMigrationV1V2 implements EntityMigration<FeatureProviderDataV1, FeatureProviderDataV2> {
    @Override
    public long getSourceVersion() {
        return 1;
    }

    @Override
    public long getTargetVersion() {
        return 2;
    }

    @Override
    public EntityDataBuilder<FeatureProviderDataV1> getDataBuilder() {
        return new ImmutableFeatureProviderDataV1.Builder();
    }

    @Override
    public FeatureProviderDataV2 migrate(FeatureProviderDataV1 entityData) {

        ImmutableFeatureProviderDataV2.Builder builder = new ImmutableFeatureProviderDataV2.Builder()
                .id(entityData.getId())
                .createdAt(entityData.getCreatedAt())
                .lastModified(entityData.getLastModified())
                .providerType(entityData.getProviderType())
                .featureProviderType(entityData.getFeatureProviderType())
                .nativeCrs(entityData.getNativeCrs());

        entityData.getTypes()
                  .values()
                  .forEach(featureType -> {
                      FeatureTypeV2 featureTypeNew = migrateFeatureType(featureType);

                      builder.putTypes(featureTypeNew.getName(), featureTypeNew);

                  });

        return builder.build();
    }

    @Override
    public Map<Identifier, EntityData> getAdditionalEntities(Identifier identifier, FeatureProviderDataV1 entityData) {
        return ImmutableMap.of();
    }

    public FeatureTypeV2 migrateFeatureType(FeatureType featureType) {

        String featureTypePath = getFeatureTypePath(featureType);

        return new ImmutableFeatureTypeV2.Builder()
                .name(featureType.getName())
                .path(featureTypePath)
                .setProperties(migrateProperties(featureType.getProperties(), featureTypePath))
                .build();
    }

    private Map<String, ImmutableFeaturePropertyV2.Builder> migrateProperties(Map<String, FeatureProperty> properties,
                                                                              String featureTypePath) {
        Map<String, ImmutableFeaturePropertyV2.Builder> migrated = new LinkedHashMap<>();

        Map<String, ImmutableFeaturePropertyV2.Builder> objectPropertiesCache = new LinkedHashMap<>();

        properties.entrySet()
                  .stream()
                  .map(entry -> {

                      String[] nameTokens = entry.getKey()
                                                 .split("\\.");
                      FeatureProperty originalProperty = entry.getValue();
                      String path = removePrefix(originalProperty.getPath(), featureTypePath);
                      FeaturePropertyV2.Type type = FeaturePropertyV2.Type.valueOf(originalProperty.getType()
                                                                                                   .toString());
                      Optional<FeaturePropertyV2.Role> role = originalProperty.getRole()
                                                                              .map(originalRole -> FeaturePropertyV2.Role.valueOf(originalRole.toString()));

                      return createPropertyTree(objectPropertiesCache, nameTokens, path, type, role);

                  })
                  .forEach(entry -> migrated.put(entry.getKey(), entry.getValue()));

        return migrated;
    }

    private Map.Entry<String, ImmutableFeaturePropertyV2.Builder> createPropertyTree(
            Map<String, ImmutableFeaturePropertyV2.Builder> objectPropertiesCache,
            String[] nameTokens,
            String path,
            FeaturePropertyV2.Type type,
            Optional<FeaturePropertyV2.Role> role) {

        String name = nameTokens[0];

        if (nameTokens.length == 1) {
            return new AbstractMap.SimpleImmutableEntry<>(clean(name), getValueProperty(name, path, type, role));
        }

        String objectPath = isNamedArray(name)
                ? removeSuffix(path, getArrayName(name))
                : removeSuffix(path, isArray(nameTokens[1]) ? 2 : 1);
        String childPath = removePrefix(path, objectPath);

        ImmutableFeaturePropertyV2.Builder objectProperty = objectPropertiesCache.computeIfAbsent(clean(name), cleanName -> getObjectProperty(name, objectPath));

        Map.Entry<String, ImmutableFeaturePropertyV2.Builder> childTree = createPropertyTree(objectPropertiesCache, Arrays.copyOfRange(nameTokens, 1, nameTokens.length), childPath, type, role);

        objectProperty.putProperties(childTree.getKey(), childTree.getValue());

        return new AbstractMap.SimpleImmutableEntry<>(clean(name), objectProperty);

    }

    private ImmutableFeaturePropertyV2.Builder getValueProperty(String name, String path, FeaturePropertyV2.Type type,
                                                                Optional<FeaturePropertyV2.Role> role) {

        ImmutableFeaturePropertyV2.Builder builder = new ImmutableFeaturePropertyV2.Builder()
                .name(clean(name))
                .path(path)
                .type(type)
                .role(role);

        if (isArray(name)) {
            builder.type(FeaturePropertyV2.Type.VALUE_ARRAY)
                   .valueType(type);
        }

        return builder;
    }

    private ImmutableFeaturePropertyV2.Builder getObjectProperty(String name, String path) {
        return new ImmutableFeaturePropertyV2.Builder()
                .name(clean(name))
                .path(path)
                .type(isArray(name) ? FeaturePropertyV2.Type.OBJECT_ARRAY : FeaturePropertyV2.Type.OBJECT);
    }

    private String getFeatureTypePath(FeatureType featureType) {
        String firstPathElement = featureType.getPropertiesByPath()
                                             .keySet()
                                             .stream()
                                             .map(property -> property.get(0))
                                             .findFirst()
                                             .orElse("");
        return "/" + firstPathElement;
    }

    private String removePrefix(String path, String prefix) {
        return path.substring(prefix.length() + 1);
    }

    private String removeSuffix(String path, int numberOfElements) {
        int slashPos = path.length();
        for (int i = 0; i < numberOfElements; i++) {
            slashPos = path.lastIndexOf("/", slashPos - 1);
        }
        int end = slashPos > -1 ? slashPos : 0;

        return path.substring(0, end);
    }

    private String removeSuffix(String path, String afterElement) {
        int elementPos = path.lastIndexOf("]" + afterElement + "/");
        int end = elementPos > -1 ? path.indexOf("/", elementPos) : 0;

        return path.substring(0, end);
    }

    private int squareBracketStart(String name) {
        return name.indexOf("[");
    }

    private int squareBracketEnd(String name) {
        return name.indexOf("]");
    }

    private boolean isArray(String name) {
        return squareBracketStart(name) > 0;
    }

    private boolean isNamedArray(String name) {
        int start = squareBracketStart(name);
        return start > 0 && squareBracketEnd(name) > start + 1;
    }

    private String getArrayName(String name) {
        if (isNamedArray(name)) {
            return name.substring(squareBracketStart(name) + 1, squareBracketEnd(name));
        }
        return "";
    }

    private String clean(String name) {
        if (isArray(name)) {
            return name.substring(0, squareBracketStart(name));
        }

        return name;
    }
}
