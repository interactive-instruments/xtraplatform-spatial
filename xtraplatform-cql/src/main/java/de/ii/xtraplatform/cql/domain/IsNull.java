package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

//TODO: not a Binary-/ScalarOperation, either remove extends or allow less operands in Binary-/ScalarOperation
@Value.Immutable
@JsonDeserialize(builder = ImmutableIsNull.Builder.class)
public interface IsNull extends CqlNode, ScalarOperation {

    static IsNull of(String property) {
        return new ImmutableIsNull.Builder().property(property).build();
    }

    abstract class Builder extends ScalarOperation.Builder<IsNull> {
    }

    @Value.Check
    @Override
    default void check() {
        int count = getOperands().size();
        Preconditions.checkState(count == 1, "IS NULL operation must have exactly one operand, found %s", count);
    }

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        return visitor.visit(this, Lists.newArrayList(getOperands().get(0)
                                                                   .accept(visitor)));
    }
}
