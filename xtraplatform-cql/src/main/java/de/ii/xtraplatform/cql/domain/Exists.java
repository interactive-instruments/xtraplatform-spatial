package de.ii.xtraplatform.cql.domain;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

//TODO: not a Binary-/ScalarOperation, either remove extends or allow less operands in Binary-/ScalarOperation
@Value.Immutable
@JsonDeserialize(builder = ImmutableExists.Builder.class)
public interface Exists extends ScalarOperation, CqlNode {

    static Exists of(String property) {
        return new ImmutableExists.Builder().property(property)
                                            .build();
    }

    abstract class Builder extends ScalarOperation.Builder<Exists> {
    }

    @Value.Check
    @Override
    default void check() {
        Preconditions.checkState(getProperty().isPresent(), "EXISTS operation must have exactly one operand, found 0");
    }

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        T property = getProperty().get()
                                  .accept(visitor);
        return visitor.visit(this, Lists.newArrayList(property));
    }
}
