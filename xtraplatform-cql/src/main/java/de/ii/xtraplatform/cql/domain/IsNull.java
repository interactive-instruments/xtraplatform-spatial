package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableIsNull.Builder.class)
public interface IsNull extends CqlNode, ScalarOperation {

    abstract class Builder extends ScalarOperation.Builder<IsNull> {
    }

    @Value.Check
    @Override
    default void check() {
        int count = getOperands().size();
        Preconditions.checkState(count == 1, "IS NULL operation must have exactly one operand, found %s", count);
    }

    @Override
    default String toCqlText() {
        return String.format("%s IS NULL", getProperty().get().toCqlText());
    }

    @Override
    default String toCqlTextNot() {
        return String.format("%s IS NOT NULL", getProperty().get().toCqlText());
    }
}
