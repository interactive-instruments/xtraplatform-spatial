package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLike.Builder.class)
public interface Like extends ScalarOperation, CqlNode {

    abstract class Builder extends ScalarOperation.Builder<Like> {
    }

    @Value.Auxiliary
    Optional<String> getWildCards();

    @Override
    default String toCqlText() {
        return ScalarOperation.super.toCqlText("LIKE");
    }

    @Override
    default String toCqlTextNot() {
        return ScalarOperation.super.toCqlText("NOT LIKE");
    }

}
