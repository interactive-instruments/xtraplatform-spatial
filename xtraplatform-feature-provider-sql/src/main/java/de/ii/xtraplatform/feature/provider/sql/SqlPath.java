package de.ii.xtraplatform.feature.provider.sql;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.OptionalInt;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, attributeBuilderDetection = true)
public interface SqlPath {

    @Nullable
    SqlPath getParent();

    String getTablePath();

    List<String> getColumns();

    @Value.Default
    default boolean isRoot() {
        return false;
    }

    @Value.Default
    default boolean isJunction() {
        return false;
    }

    @Value.Default
    default boolean hasOid() {
        return false;
    }

    OptionalInt getSortPriority();
}
