package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Value.Immutable
public interface SqlQueries {

    Optional<String> getMetaQuery();

    Function<SqlRowMeta, Stream<String>> getValueQueries();

    List<FeatureStoreInstanceContainer> getInstanceContainers();
}
