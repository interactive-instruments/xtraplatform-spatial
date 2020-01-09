package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

public interface BinaryOperation<T extends Literal> {

    Optional<Property> getProperty();

    Optional<T> getValue();

    @Value.Check
    default void check() {
        int count = getOperands().size();

        Preconditions.checkState(count == 2, "a binary operation must have exactly two operands, found %s", count);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<Operand> getOperands() {
        return ImmutableList.of(
                getProperty(),
                getValue()
        )
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(ImmutableList.toImmutableList());
    }


    abstract class Builder<T extends Literal, U extends BinaryOperation<T>> {
        public abstract Builder<T,U> property(Property property);

        public abstract Builder<T,U> value(T literal);

        public abstract U build();

        public Builder<T,U> operand1(Operand operand1) {
            addOperand(operand1);
            return this;
        }

        public Builder<T,U> operand2(Operand operand2) {
            addOperand(operand2);
            return this;
        }

        @SuppressWarnings("unchecked")
        private void addOperand(Operand literal) {
            if (literal instanceof Property) {
                property((Property) literal);
            } else if (literal instanceof Literal) {
                value((T)literal);
            }
        }
    }

    default String toCqlText(String operator) {
        return String.format("%s %s %s", getOperands().get(0)
                                                      .toCqlText(), operator, getOperands().get(1)
                                                                                           .toCqlText());
    }
}
