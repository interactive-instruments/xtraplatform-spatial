package de.ii.xtraplatform.feature.provider.wfs;

import de.ii.xtraplatform.feature.provider.api.FeatureProviderConnector;
import de.ii.xtraplatform.ogc.api.wfs.WfsOperation;

import java.io.InputStream;

/**
 * @author zahnen
 */
public interface WfsConnector extends FeatureProviderConnector {

    void setQueryEncoder(final FeatureQueryEncoderWfs queryEncoder);

    InputStream runWfsOperation(final WfsOperation wfsOperation);
}
