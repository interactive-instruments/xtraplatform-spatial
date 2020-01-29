package de.ii.xtraplatform.cql.domain;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableExists.Builder.class)
public interface Exists extends CqlNode, ScalarOperation {

    abstract class Builder extends ScalarOperation.Builder<Exists> {
    }

    @Value.Check
    @Override
    default void check() {
        int count = getOperands().size();
        Preconditions.checkState(count == 1, "EXISTS operation must have exactly one operand, found %s", count);
    }

    @Override
    default String toCqlText() {
        return String.format("%s EXISTS", getProperty().get().toCqlText());
    }

    @Override
    default String toCqlTextNot() {
        return String.format("%s DOES-NOT-EXIST", getProperty().get().toCqlText());
    }
}
