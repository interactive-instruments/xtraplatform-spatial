package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLike.Builder.class)
public interface Like extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Like> {
    }

    @Value.Default
    default String getWildCards() {
        return "%";
    }

    @Override
    default String toCqlText() {
        return ScalarOperation.super.toCqlText("LIKE");
    }

    @Override
    default String toCqlTextNot() {
        return ScalarOperation.super.toCqlText("NOT LIKE");
    }

}
