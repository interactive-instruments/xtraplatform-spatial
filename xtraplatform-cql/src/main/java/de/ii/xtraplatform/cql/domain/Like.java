package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLike.Builder.class)
public interface Like extends ScalarOperation, CqlNode {

    static Like of (String property, String literal, String wildCard) {
        return new ImmutableLike.Builder().property(property).value(ScalarLiteral.of(literal)).build();
    }

    abstract class Builder extends ScalarOperation.Builder<Like> {
    }

    @Value.Auxiliary
    Optional<String> getWildCards();

}
