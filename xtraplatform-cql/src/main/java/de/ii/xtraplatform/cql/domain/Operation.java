package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import java.util.stream.Collectors;

@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "op")
@JsonSubTypes({
    @Type(value = Eq.class, name = Eq.TYPE),
    @Type(value = Neq.class, name = Neq.TYPE),
    @Type(value = Gt.class, name = Gt.TYPE),
    @Type(value = Gte.class, name = Gte.TYPE),
    @Type(value = Lt.class, name = Lt.TYPE),
    @Type(value = Lte.class, name = Lte.TYPE),
    @Type(value = Like.class, name = Like.TYPE),
    @Type(value = Not.class, name = Not.TYPE),
    @Type(value = And.class, name = And.TYPE),
    @Type(value = Or.class, name = Or.TYPE),
    @Type(value = IsNull.class, name = IsNull.TYPE),
    @Type(value = In.class, name = In.TYPE)
})
public interface Operation<T extends Operand> extends Cql2Predicate, CqlNode {

  String getOp();

  List<T> getArgs();

  abstract class Builder<T extends Operand, U extends Operation<T>> {

    public abstract U build();

    public abstract Builder<T, U> args(Iterable<? extends T> operands);

    public abstract Builder<T, U> addArgs(T operand);
  }

  @Override
  default <U> U accept(CqlVisitor<U> visitor) {
    List<U> operands = getArgs().stream()
        .map(arg -> arg.accept(visitor))
        .collect(Collectors.toList());

    return visitor.visit(this, operands);
  }
}
