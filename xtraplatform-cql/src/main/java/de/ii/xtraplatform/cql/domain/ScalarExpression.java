/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface ScalarExpression {

    Optional<Eq> getEq();

    Optional<Lt> getLt();

    Optional<Gt> getGt();

    Optional<Lte> getLte();

    Optional<Gte> getGte();

    Optional<Neq> getNeq();

    Optional<Between> getBetween();

    // getter method for the IN operator was changed to avoid deserialization errors due to ambiguity with getWithin()
    @JsonProperty("in")
    Optional<In> getInOperator();

    Optional<Like> getLike();

    Optional<IsNull> getIsNull();

    Optional<Exists> getExists();

}
