package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
public interface SchemaSql extends SchemaBase<SchemaSql> {

    @Override
    @Value.Derived
    default List<String> getPath() {
        return getRelation().map(FeatureStoreRelation::asPath)
                            .orElse(ImmutableList.of(getName()));
    }

    Optional<FeatureStoreRelation> getRelation();

    //TODO
    Optional<Object> getTarget();

    //TODO
    @Value.Default
    default Optional<String> getPrimaryKey() {
        return Optional.of("id");
    }
}
