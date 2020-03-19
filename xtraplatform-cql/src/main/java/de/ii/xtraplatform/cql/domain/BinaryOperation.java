package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

public interface BinaryOperation<T extends Literal> extends CqlNode {

    Optional<Property> getProperty();

    Optional<T> getValue();

    Optional<Function> getFunction();

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
                getFunction(),
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

        public abstract Builder<T,U> function(Function function);

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
        public void addOperand(Operand literal) {
            if (literal instanceof Property) {
                property((Property) literal);
            } else if (literal instanceof Literal) {
                value((T)literal);
            } else if (literal instanceof Function) {
                function((Function) literal);
            }
        }
    }

    @Override
    default <U> U accept(CqlVisitor<U> visitor) {
        U operand1 = getOperands().get(0)
                                .accept(visitor);
        U operand2 = getOperands().get(1)
                                  .accept(visitor);

        return visitor.visit(this, Lists.newArrayList(operand1, operand2));
    }
}
