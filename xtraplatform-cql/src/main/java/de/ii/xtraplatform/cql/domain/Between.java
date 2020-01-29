package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.Optional;

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
    default String toCqlText() {
        return String.format("%s BETWEEN %s AND %s", getProperty().get().toCqlText(),
                getLower().get().toCqlText(), getUpper().get().toCqlText());
    }
}
