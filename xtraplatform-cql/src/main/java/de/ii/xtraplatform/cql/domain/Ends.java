package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEnds.Builder.class)
public interface Ends extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<Ends> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("ENDS");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
