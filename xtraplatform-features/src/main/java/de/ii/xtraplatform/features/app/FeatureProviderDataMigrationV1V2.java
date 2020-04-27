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


        entityData.getTypes().values().forEach(featureType -> {

            String firstElement = featureType.getPropertiesByPath()
                    .keySet()
                    .stream()
                    .map(property -> property.get(0))
                    .findFirst()
                    .get();
            String featureTypePath = "/" + firstElement;
            ImmutableFeatureTypeV2.Builder featureTypeNew = new ImmutableFeatureTypeV2.Builder()
                    .path(featureTypePath);

            String name = null;
            String path = null;
            Map<String, FeaturePropertyV2> newProperties = new LinkedHashMap<>();
            for (Map.Entry<String, FeatureProperty> entry : featureType.getProperties().entrySet()) {

                FeatureProperty fp = entry.getValue();
                String[] nameTokens = entry.getKey().split("\\.");
                FeaturePropertyV2.Role role = fp.getRole().isPresent() ? FeaturePropertyV2.Role.ID : null;
                FeaturePropertyV2.Type type = FeaturePropertyV2.Type.valueOf(fp.getType().toString());

                if (nameTokens.length == 1) {
                    path = fp.getPath().replace(featureTypePath + "/", "");
                    name = entry.getKey();
                    if (nameTokens[0].endsWith("[]")) {
                        featureTypeNew.putProperties(entry.getKey(),
                                getFeaturePropertyV2ValueArray(entry.getKey(), path, type, role));
                    } else {
                        featureTypeNew.putProperties(entry.getKey(), getFeaturePropertyV2(name, path, type, role));
                    }
                } else {
                    String parentName = nameTokens[0].split("(\\[\\w*\\])")[0];
                    FeaturePropertyV2.Type childType = FeaturePropertyV2.Type.valueOf(fp.getType().toString());

                    if (!parentName.equals(name)) {
                        name = parentName;
                        path = fp.getPath().replace(featureTypePath + "/", "").split("(/[^/]+$)")[0];
                        newProperties.clear();
                    }

                    if (nameTokens[0].matches("([\\w]*\\[\\w*\\])")) {
                        type = FeaturePropertyV2.Type.OBJECT_ARRAY;
                    } else {
                        type = FeaturePropertyV2.Type.OBJECT;
                    }

                    if (nameTokens[1].matches("([\\w]*\\[\\w*\\])")) {
                        String childName = nameTokens[1].split("(\\[\\w*\\])")[0];
                        String childPath = fp.getPath().replace(featureTypePath + "/" + path + "/", "");
                        newProperties.put(childName,
                                getFeaturePropertyV2ValueArray(childName, childPath, childType, role));
                    } else {
                        int lastSlashIndex = fp.getPath().lastIndexOf('/');
                        newProperties.put(nameTokens[1],
                                getFeaturePropertyV2(fp.getName(), fp.getPath().substring(lastSlashIndex + 1), childType, role));
                    }
                    featureTypeNew.putProperties(name, getFeaturePropertyV2(name, path, type, role, newProperties));
                }
            }
            builder.putTypes2(featureType.getName(), featureTypeNew);

        });

        return builder.build();
    }

    private FeaturePropertyV2 getFeaturePropertyV2(String name, String path, FeaturePropertyV2.Type type,
                                                   FeaturePropertyV2.Role role) {
        return new ImmutableFeaturePropertyV2.Builder()
                .name(name)
                .path(path)
                .type(type)
                .role(Optional.ofNullable(role))
                .build();
    }

    private FeaturePropertyV2 getFeaturePropertyV2(String name, String path, FeaturePropertyV2.Type type,
                                                   FeaturePropertyV2.Role role, Map<String, FeaturePropertyV2> properties) {
        return new ImmutableFeaturePropertyV2.Builder()
                .from(getFeaturePropertyV2(name, path, type, role))
                .properties(properties)
                .build();
    }

    private FeaturePropertyV2 getFeaturePropertyV2ValueArray(String name, String path, FeaturePropertyV2.Type valueType,
                                                             FeaturePropertyV2.Role role) {
        return new ImmutableFeaturePropertyV2.Builder()
                .name(name)
                .path(path)
                .type(FeaturePropertyV2.Type.VALUE_ARRAY)
                .role(Optional.ofNullable(role))
                .valueType(valueType)
                .build();
    }

    @Override
    public Map<Identifier, EntityData> getAdditionalEntities(Identifier identifier, FeatureProviderDataV1 entityData) {
        return ImmutableMap.of();
    }
}
