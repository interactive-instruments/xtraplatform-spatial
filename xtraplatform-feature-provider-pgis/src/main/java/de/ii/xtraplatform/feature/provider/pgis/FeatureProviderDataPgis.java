package de.ii.xtraplatform.feature.provider.pgis;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ImmutableFeatureProviderDataPgis.class)
public abstract class FeatureProviderDataPgis extends FeatureProviderDataTransformer {

    @Value.Derived
    @Override
    public String getProviderType() {
        return FeatureProviderPgis.PROVIDER_TYPE;
    }

    public abstract ConnectionInfo getConnectionInfo();

    @Value.Default
    public boolean computeNumberMatched() {
        return true;
    }

    @Override
    public boolean isFeatureTypeEnabled(String featureType) {
        //TODO: better way to detect mapping for feature type
        // returns only enabled mappings
        final Optional<TargetMapping> mapping = getMappings()
                .get(featureType)
                .findMappings("/" + featureType, TargetMapping.BASE_TYPE);
        return /*!service.getServiceProperties()
                           .getMappingStatus()
                           .isEnabled() ||*/ mapping.isPresent();
    }
}
