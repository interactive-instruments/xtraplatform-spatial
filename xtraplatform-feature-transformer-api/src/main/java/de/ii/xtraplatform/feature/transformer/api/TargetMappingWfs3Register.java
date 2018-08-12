package de.ii.xtraplatform.feature.transformer.api;

import com.google.common.collect.ImmutableMap;
import de.ii.xsf.dropwizard.api.JacksonSubTypeIds;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Map;

/**
 * @author zahnen
 */
@Component
@Provides
//@Instantiate
public class TargetMappingWfs3Register implements JacksonSubTypeIds {

    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(TargetMappingWfs3.class, "WFS3")
                .put(TargetMappingTestGeneric.class, "GENERIC_PROPERTY")
                .put(TargetMappingTestMicrodata.class, "MICRODATA_PROPERTY")
                .put(TargetMappingTestGeoJson.class, "GEO_JSON_PROPERTY")
                .put(TargetMappingTestMicrodataGeo.class, "MICRODATA_GEOMETRY")
                .put(TargetMappingTestGeoJsonGEO.class, "GEO_JSON_GEOMETRY")

                .build();
    }
}
