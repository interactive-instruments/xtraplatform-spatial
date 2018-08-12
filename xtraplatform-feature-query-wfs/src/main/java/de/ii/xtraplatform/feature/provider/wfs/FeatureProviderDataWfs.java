package de.ii.xtraplatform.feature.provider.wfs;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ImmutableFeatureProviderDataWfs.class)
public abstract class FeatureProviderDataWfs extends FeatureProviderDataTransformer {

    @Value.Derived
    @Override
    public String getProviderType() {
        return FeatureProviderWfs.PROVIDER_TYPE;
    }

    public abstract ConnectionInfo getConnectionInfo();

    @Override
    public boolean isFeatureTypeEnabled(String featureType) {
        //TODO: better way to detect mapping for feature type
        FeatureTypeMapping featureTypeMapping = getMappings().get(featureType);
        if (featureTypeMapping != null) {
            SourcePathMapping firstMapping = featureTypeMapping.getMappings()
                                                       .values()
                                                       .iterator()
                                                       .next();
            return firstMapping.getMappingForType(TargetMapping.BASE_TYPE).isEnabled();
        }
        return false;
    }
}
