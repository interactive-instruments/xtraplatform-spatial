package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.provider.api.FeatureCrs;
import de.ii.xtraplatform.feature.provider.api.FeatureExtents;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider2;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderDataV1;
import de.ii.xtraplatform.feature.provider.api.FeatureQueries;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream2;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@EntityComponent
@Entity(entityType = FeatureProvider2.class, dataType = FeatureProviderDataV1.class, type = "providers")
public class FeatureProviderWfs implements FeatureProvider2, FeatureQueries, FeatureExtents, FeatureCrs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    private static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .build());

    private final ActorSystem system;
    private final ActorMaterializer materializer;
    private final CrsTransformerFactory crsTransformerFactory;
    private final WfsConnector connector;
    private final Map<String, FeatureStoreTypeInfo> typeInfos;
    private final FeatureStoreQueryGeneratorWfs queryGenerator;
    private final FeatureQueryTransformerWfs queryTransformer;
    private final FeatureNormalizerWfs featureNormalizer;
    private final ExtentReaderWfs extentReader;

    public FeatureProviderWfs(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Property(name = "data") FeatureProviderDataV1 data,
                              @Property(name = ".connector") WfsConnector wfsConnector) {
        //TODO: starts akka for every instance, move to singleton
        this.system = actorSystemProvider.getActorSystem(context, config);
        this.materializer = ActorMaterializer.create(system);
        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = wfsConnector;
        this.typeInfos = getTypeInfos2(data.getTypes());
        this.queryGenerator = new FeatureStoreQueryGeneratorWfs(new FilterEncoderWfs(data.getNativeCrs()));
        this.queryTransformer = new FeatureQueryTransformerWfs(typeInfos, queryGenerator, false/*TODO data.computeNumberMatched()*/);
        this.featureNormalizer = new FeatureNormalizerWfs(typeInfos, data.getTypes());
        this.extentReader = new ExtentReaderWfs(connector, queryGenerator, data.getNativeCrs());
    }

    private Map<String, FeatureStoreTypeInfo> getTypeInfos2(
            Map<String, FeatureType> types) {
        return null;
    }

    @Override
    public FeatureProviderDataV1 getData() {
        return null;
    }

    @Override
    public FeatureStream2 getFeatureStream2(FeatureQuery query) {
        return null;
    }

    @Override
    public long getFeatureCount(FeatureQuery featureQuery) {
        return 0;
    }

    @Override
    public BoundingBox getSpatialExtent(String typeName) {
        return null;
    }


    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        return false;
    }
}
