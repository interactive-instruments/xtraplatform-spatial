package de.ii.xtraplatform.feature.provider.wfs.app;

import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;

import java.util.Map;

public class FeatureQueryTransformerWfs implements FeatureQueryTransformer<String> {

    public FeatureQueryTransformerWfs(Map<String, FeatureStoreTypeInfo> typeInfos,
                                      FeatureStoreQueryGeneratorWfs queryGenerator, boolean computeNumberMatched) {

    }

    //TODO
    @Override
    public String transformQuery(FeatureQuery featureQuery) {

        return "https://www.wfs.nrw.de/geobasis/wfs_nw_alkis_vereinfacht?SERVICE=WFS&VERSION=2.0.0&REQUEST=GetFeature&TYPENAMES=ave:Flurstueck&NAMESPACES=xmlns(ave,http://repository.gdi-de.org/schemas/adv/produkt/alkis-vereinfacht/2.0)&COUNT=10";
    }

}
