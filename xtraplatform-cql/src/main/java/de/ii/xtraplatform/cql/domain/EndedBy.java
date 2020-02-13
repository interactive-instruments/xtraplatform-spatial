package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEndedBy.Builder.class)
public interface EndedBy extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<EndedBy> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("ENDEDBY");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
