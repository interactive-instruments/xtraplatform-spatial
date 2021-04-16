/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLike.Builder.class)
public interface Like extends ScalarOperation, CqlNode {

    static Like of(String property, ScalarLiteral scalarLiteral) {
        return new ImmutableLike.Builder().property(property)
                                          .value(scalarLiteral)
                                          .build();
    }

    static Like of(String property, ScalarLiteral scalarLiteral, String wildCard, String singlechar, String escapechar, Boolean nocase) {
        return new ImmutableLike.Builder().property(property)
                                          .value(scalarLiteral)
                                          .wildcard(Optional.ofNullable(wildCard))
                                          .singlechar(Optional.ofNullable(singlechar))
                                          .escapechar(Optional.ofNullable(escapechar))
                                          .nocase(Optional.ofNullable(nocase))
                                          .build();
    }

    static Like of(String property, Property property2) {
        return new ImmutableLike.Builder().property(property)
                .property2(property2)
                .build();
    }

    static Like of(String property, Property property2, String wildCard, String singlechar, String escapechar, Boolean nocase) {
        return new ImmutableLike.Builder().property(property)
                .property2(property2)
                .wildcard(Optional.ofNullable(wildCard))
                .singlechar(Optional.ofNullable(singlechar))
                .escapechar(Optional.ofNullable(escapechar))
                .nocase(Optional.ofNullable(nocase))
                .build();
    }

    static Like ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableLike.Builder().function(function)
                .value(scalarLiteral)
                .build();
    }

    static Like ofFunction(Function function, ScalarLiteral scalarLiteral, String wildCard, String singlechar, String escapechar, Boolean nocase) {
        return new ImmutableLike.Builder().function(function)
                .value(scalarLiteral)
                .wildcard(Optional.ofNullable(wildCard))
                .singlechar(Optional.ofNullable(singlechar))
                .escapechar(Optional.ofNullable(escapechar))
                .nocase(Optional.ofNullable(nocase))
                .build();
    }

    abstract class Builder extends ScalarOperation.Builder<Like> {
    }

    Optional<String> getWildcard();

    Optional<String> getSinglechar();

    Optional<String> getEscapechar();

    Optional<Boolean> getNocase();

}
