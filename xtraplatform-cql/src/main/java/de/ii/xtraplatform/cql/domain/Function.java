package de.ii.xtraplatform.cql.domain;

import java.util.List;
import java.util.stream.Collectors;

public interface Function extends CqlNode {

    String getName();

    List<Operand> getOperands();

    @Override
    default <U> U accept(CqlVisitor<U> visitor) {

        List<U> operands = getOperands().stream()
                                       .map(operand -> operand.accept(visitor))
                                       .collect(Collectors.toList());

        return visitor.visit(this, operands);
    }
}
