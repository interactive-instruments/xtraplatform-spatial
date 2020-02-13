package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLt.Builder.class)
public interface Lt extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Lt> {
    }

    @Override
    default String toCqlText() {
        return ScalarOperation.super.toCqlText("<");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
