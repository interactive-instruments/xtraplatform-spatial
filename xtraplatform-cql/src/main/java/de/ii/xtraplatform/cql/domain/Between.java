package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.Optional;

//TODO: not a Binary-/ScalarOperation, either remove extends or allow more operands in Binary-/ScalarOperation
@Value.Immutable
@JsonDeserialize(builder = ImmutableBetween.Builder.class)
public interface Between extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Between> {
    }

    Optional<ScalarLiteral> getLower();

    Optional<ScalarLiteral> getUpper();

    @Value.Check
    @Override
    default void check() {
        Preconditions.checkState(getLower().isPresent() && getUpper().isPresent(), "a BETWEEN operation must have exactly two values: upper and lower");
        Preconditions.checkState(getProperty().isPresent(), "property is missing");
    }

    @Override
    default <U> U accept(CqlVisitor<U> visitor) {
        U property = getProperty().get()
                                  .accept(visitor);
        U lower = getLower().get()
                            .accept(visitor);
        U upper = getUpper().get()
                            .accept(visitor);

        return visitor.visit(this, Lists.newArrayList(property, lower, upper));
    }
}
