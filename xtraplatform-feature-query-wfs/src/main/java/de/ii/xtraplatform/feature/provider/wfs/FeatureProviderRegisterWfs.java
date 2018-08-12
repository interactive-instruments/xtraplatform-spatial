package de.ii.xtraplatform.feature.provider.wfs;

import com.google.common.collect.ImmutableMap;
import de.ii.xsf.dropwizard.api.JacksonSubTypeIds;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Map;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class FeatureProviderRegisterWfs implements JacksonSubTypeIds {
    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(FeatureProviderDataWfs.class, FeatureProviderWfs.PROVIDER_TYPE)
                .build();
    }
}
