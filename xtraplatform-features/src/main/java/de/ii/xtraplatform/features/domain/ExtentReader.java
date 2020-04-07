package de.ii.xtraplatform.features.domain;

import akka.stream.javadsl.RunnableGraph;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ExtentReader {
    RunnableGraph<CompletionStage<Optional<BoundingBox>>> getExtent(FeatureStoreTypeInfo typeInfo);
}
