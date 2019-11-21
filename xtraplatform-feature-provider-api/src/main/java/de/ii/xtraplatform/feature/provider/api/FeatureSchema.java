package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.scheduler.api.TaskProgress;

import javax.xml.namespace.QName;
import java.util.Map;

//TODO
public interface FeatureSchema {

    void getSchema(FeatureProviderSchemaConsumer schemaConsumer, Map<String, QName> featureTypes,
                   TaskProgress taskProgress);

}
