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

        ImmutableFeatureTypeV2.Builder featureTypeNew = new ImmutableFeatureTypeV2.Builder()
                .name(featureType.getName())
                .path(featureTypePath);

        Map<String, ImmutableFeaturePropertyV2.Builder> newProperties = new LinkedHashMap<>();

        for (Map.Entry<String, FeatureProperty> entry : featureType.getProperties()
                                                                   .entrySet()) {

            String[] nameTokens = entry.getKey()
                                       .split("\\.");
            FeatureProperty originalProperty = entry.getValue();
            String name = nameTokens[0];
            String path = originalProperty.getPath()
                                          .replace(featureTypePath + "/", "");
            FeaturePropertyV2.Type type = FeaturePropertyV2.Type.valueOf(originalProperty.getType()
                                                                                         .toString());
            Optional<FeaturePropertyV2.Role> role = originalProperty.getRole().map(originalRole -> FeaturePropertyV2.Role.valueOf(originalRole.toString()));

            if (nameTokens.length == 1) {
                newProperties.put(clean(name), getValueProperty(name, path, type, role));

                continue;
            }

            getNestedProperties(newProperties, nameTokens, path, type, role);

        }

        newProperties.forEach(featureTypeNew::putProperties);

        return featureTypeNew.build();
    }

    private void getNestedProperties(Map<String, ImmutableFeaturePropertyV2.Builder> parentProperties,
                                     String[] nameTokens,
                                     String path,
                                     FeaturePropertyV2.Type type,
                                     Optional<FeaturePropertyV2.Role> role) {

        String parentName = nameTokens[0];
        String parentPath = getFirstElements(path, isArray(nameTokens[1]) ? 2 : 1);

        if (!parentProperties.containsKey(clean(parentName))) {
            parentProperties.put(clean(parentName), getObjectProperty(parentName, parentPath));
        }

        String childName = nameTokens[1];
        String childPath = path.replace(parentPath + "/", "");

        parentProperties.get(clean(parentName))
                        .putProperties(clean(childName), getValueProperty(childName, childPath, type, role));
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

        ImmutableFeaturePropertyV2.Builder builder = new ImmutableFeaturePropertyV2.Builder()
                .name(clean(name))
                .path(path)
                .type(isArray(name) ? FeaturePropertyV2.Type.OBJECT_ARRAY : FeaturePropertyV2.Type.OBJECT);


        return builder;
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

    private String getFirstElements(String path, int remainingElementCount) {
        int slashPos = path.length();
        for (int i = 0; i < remainingElementCount; i++) {
            slashPos = path.lastIndexOf("/", slashPos - 1);
        }
        int end = slashPos > -1 ? slashPos : 0;

        return path.substring(0, end);
    }

    private int squareBracketPos(String name) {
        return name.indexOf("[");
    }

    private boolean isArray(String name) {
        return squareBracketPos(name) > 0;
    }

    private String clean(String name) {
        if (isArray(name)) {
            return name.substring(0, squareBracketPos(name));
        }

        return name;
    }
}
