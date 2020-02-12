package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.util.ByteString;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@EntityComponent
@Entity(entityType = FeatureProvider2.class, dataType = FeatureProviderDataV1.class, type = "providers", subType = "feature/wfs")
public class FeatureProviderWfs extends AbstractFeatureProvider<ByteString, String, FeatureProviderConnector.QueryOptions> implements FeatureProvider2, FeatureQueries, FeatureCrs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    private static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .build());

    private final CrsTransformerFactory crsTransformerFactory;
    private final WfsConnector connector;
    private FeatureStorePathParserWfs pathParser;
    private FeatureQueryTransformerWfs queryTransformer;
    private FeatureNormalizerWfs featureNormalizer;

    public FeatureProviderWfs(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Property(name = "data") FeatureProviderDataV1 data,
                              @Property(name = ".connector") WfsConnector wfsConnector) {
        super(context, actorSystemProvider);
        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = wfsConnector;
    }

    @Override
    public FeatureProviderDataV1 getData() {
        return super.getData();
    }

    @Override
    protected void onStart() {
        if (!getConnector().isConnected()) {
            Optional<Throwable> connectionError = getConnector().getConnectionError();
            String message = connectionError.map(Throwable::getMessage)
                                            .orElse("unknown reason");
            LOGGER.error("Feature provider with id '{}' could not be started: {}", getId(), message);
            if (connectionError.isPresent() && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", connectionError.get());
            }
        } else {
            this.pathParser = new FeatureStorePathParserWfs(((ConnectionInfoWfsHttp)getData().getConnectionInfo()).getNamespaces());
            this.queryTransformer = new FeatureQueryTransformerWfs(getTypeInfos(), getData().getTypes(), (ConnectionInfoWfsHttp)getData().getConnectionInfo(), getData().getNativeCrs());
            this.featureNormalizer = new FeatureNormalizerWfs(getTypeInfos(), getData().getTypes(), ((ConnectionInfoWfsHttp)getData().getConnectionInfo()).getNamespaces());
        }
    }

    @Override
    protected FeatureStorePathParser getPathParser() {
        return pathParser;
    }

    @Override
    protected FeatureQueryTransformer<String> getQueryTransformer() {
        return queryTransformer;
    }

    @Override
    protected FeatureProviderConnector<ByteString, String, FeatureProviderConnector.QueryOptions> getConnector() {
        return connector;
    }

    @Override
    protected FeatureNormalizer<ByteString> getNormalizer() {
        return featureNormalizer;
    }


    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        return getData().getNativeCrs()
                        .equals(crs) || crsTransformerFactory.isCrsSupported(crs);
    }
}
