package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: not a Binary-/ScalarOperation, either remove extends or allow more operands in Binary-/ScalarOperation
@Value.Immutable
@JsonDeserialize(builder = ImmutableIn.Builder.class)
public interface In extends CqlNode, ScalarOperation {

    static In of(ScalarLiteral... values) {
        return new ImmutableIn.Builder().property("_ID_")
                                        .values(Arrays.asList(values))
                                        .build();
    }

    abstract class Builder extends ScalarOperation.Builder<In> {
    }

    @Value.Check
    @Override
    default void check() {
        int count = getValues().size();
        Preconditions.checkState(count > 0, "an IN operation must have at least one value, found %s", count);
        Preconditions.checkState(getProperty().isPresent(), "property is missing");
    }

    @Value.Default
    default Boolean getNocase() {
        return Boolean.TRUE;
    }

    List<ScalarLiteral> getValues();

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        List<T> children = Stream.concat(Stream.of(getProperty().get()), getValues().stream())
                                .map(value -> value.accept(visitor))
                                .collect(Collectors.toList());


        return visitor.visit(this, children);
    }
}
