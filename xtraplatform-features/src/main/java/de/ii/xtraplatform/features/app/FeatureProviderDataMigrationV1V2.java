package de.ii.xtraplatform.features.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.event.store.EntityMigration;
import de.ii.xtraplatform.event.store.Identifier;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureTypeV2;

import java.util.Map;

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

        //TODO: transform flat properties structure from entityData into new nested structure

        entityData.getTypes().values().forEach(featureType -> {

            ImmutableFeatureTypeV2.Builder featureTypeNew = new ImmutableFeatureTypeV2.Builder()
                    .path("");

            builder.putTypes2(featureType.getName(), featureTypeNew);
        });

        return builder.build();
    }

    @Override
    public Map<Identifier, EntityData> getAdditionalEntities(Identifier identifier, FeatureProviderDataV1 entityData) {
        return ImmutableMap.of();
    }
}
