/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;


import de.ii.xtraplatform.feature.provider.api.FeatureProviderRegistry;
import de.ii.xtraplatform.service.api.AbstractService;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
//@Component
//@Provides
//@Entity(entityType = Service.class, dataType = FeatureTransformerServiceData.class)
// TODO: @Stereotype does not seem to work, maybe test with bnd-ipojo-plugin
// needed to register the ConfigurationHandler when no other properties are set
//@HandlerDeclaration("<properties></properties>")
public abstract class AbstractFeatureTransformerService extends AbstractService<FeatureTransformerServiceData> implements FeatureTransformerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureTransformerService.class);

    @Requires
    private FeatureProviderRegistry featureProviderRegistry;

    protected TransformingFeatureProvider featureProvider;

    public AbstractFeatureTransformerService() {


    }

    //TODO: setData not called without this
    //@Validate
    //void onStart() {
    //    LOGGER.debug("STARTED {} {}", getId(), shouldRegister());
    //}

    /*@Override
    protected ImmutableFeatureTransformerServiceData dataToImmutable(FeatureTransformerServiceData data) {
        final ImmutableFeatureTransformerServiceData serviceData = ImmutableFeatureTransformerServiceData.copyOf(data);

        //TODO
        this.featureProvider = (TransformingFeatureProvider) featureProviderRegistry.createFeatureProvider(serviceData.getFeatureProvider());

        return serviceData;
    }*/

    /*@Override
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
    }*/
}
