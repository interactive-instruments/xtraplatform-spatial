package de.ii.xtraplatform.feature.transformer.api;


import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.query.api.FeatureProviderRegistry;
import de.ii.xtraplatform.service.api.AbstractService;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides
@Entity(entityType = Service.class, dataType = FeatureTransformerServiceData.class)
// TODO: @Stereotype does not seem to work, maybe test with bnd-ipojo-plugin
// needed to register the ConfigurationHandler when no other properties are set
@HandlerDeclaration("<properties></properties>")
public class AbstractFeatureTransformerService extends AbstractService<FeatureTransformerServiceData> implements FeatureTransformerService2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureTransformerService.class);

    @Requires
    private FeatureProviderRegistry featureProviderRegistry;

    private TransformingFeatureProvider featureProvider;

    AbstractFeatureTransformerService() {


    }

    //TODO: setData not called without this
    @Validate
    void onStart() {
        LOGGER.debug("STARTED {} {}", getId(), shouldRegister());
    }

    @Override
    protected ImmutableFeatureTransformerServiceData dataToImmutable(FeatureTransformerServiceData data) {
        final ImmutableFeatureTransformerServiceData serviceData = ImmutableFeatureTransformerServiceData.copyOf(data);

        //TODO
        this.featureProvider = (TransformingFeatureProvider) featureProviderRegistry.createFeatureProvider(serviceData.getFeatureProvider());

        return serviceData;
    }

    @Override
    public Optional<FeatureTypeConfiguration> getFeatureTypeByName(String name) {
        return Optional.ofNullable(getData().getFeatureTypes().get(name));
    }

    @Override
    public TransformingFeatureProvider getFeatureProvider() {
        return featureProvider;
    }

    public CrsTransformer getCrsTransformer() {
        return featureProvider.getCrsTransformer();
    }
    public CrsTransformer getBboxTransformer() {
        return featureProvider.getReverseCrsTransformer();
    }
}
