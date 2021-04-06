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

    static Like of(String property, ScalarLiteral scalarLiteral, String wildCard, String singlechar, String escapechar, String nocase) {
        return new ImmutableLike.Builder().property(property)
                                          .value(scalarLiteral)
                                          .wildCards(wildCard)
//                                          .singlechars(singlechar)
//                                          .escapechars(escapechar)
//                                          .nocases(nocase)
                                          .build();
    }

    static Like ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableLike.Builder().function(function)
                .value(scalarLiteral)
                .build();
    }

    static Like ofFunction(Function function, ScalarLiteral scalarLiteral, String wildCard, String singlechar, String escapechar, String nocase) {
        return new ImmutableLike.Builder().function(function)
                .value(scalarLiteral)
                .wildCards(wildCard)
//                .singlechars(singlechar)
//                .escapechars(escapechar)
//                .nocases(nocase)
                .build();
    }

    abstract class Builder extends ScalarOperation.Builder<Like> {
    }

    @Value.Auxiliary
    Optional<String> getWildCards();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default String getWildCard() {
        return getWildCards().orElse("%");
    }

//    @Value.Auxiliary
//    Optional<String> getSinglechars();

//    @JsonIgnore
//    @Value.Derived
//    @Value.Auxiliary
//    default String getSinglechar() {
//        return getSinglechars().orElse("_");
//    }

//    @Value.Auxiliary
//    Optional<String> getEscapechars();
//
//    @JsonIgnore
//    @Value.Derived
//    @Value.Auxiliary
//    default String getEscapechar() {
//        return getEscapechars().orElse("\\");
//    }
//
//    @Value.Auxiliary
//    Optional<String> getNocases();
//
//    @JsonIgnore
//    @Value.Derived
//    @Value.Auxiliary
//    default String getNocase() {
//        return getNocases().orElse("Boolean.TRUE");
//    }

}
