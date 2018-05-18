package de.ii.xtraplatform.feature.source.wfs;

import com.google.common.util.concurrent.ListenableFuture;
import de.ii.xtraplatform.feature.query.api.WfsProxyFeatureType;
import de.ii.xtraplatform.feature.query.api.FeatureProvider;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public class FeatureProviderWfs implements FeatureProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    private final WFSAdapter wfsAdapter;
    private final FeatureQueryEncoderWfs queryEncoder;

    public FeatureProviderWfs(final WFSAdapter wfsAdapter, final Map<String, WfsProxyFeatureType> featureTypes) {
        this.wfsAdapter = wfsAdapter;
        queryEncoder = new FeatureQueryEncoderWfs(featureTypes, wfsAdapter.getNsStore());
    }

    @Override
    public Optional<ListenableFuture<HttpEntity>> getFeatureStream(FeatureQuery query) {
            return queryEncoder.encode(query)
                    .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getResponse());
    }

    @Override
    public Optional<ListenableFuture<HttpEntity>> getFeatureCount(FeatureQuery query) {
        return queryEncoder.encode(query, true)
                           .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getResponse());
    }
}
