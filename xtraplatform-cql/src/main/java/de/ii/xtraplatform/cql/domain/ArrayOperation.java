/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ArrayOperation.class)
public interface ArrayOperation extends BinaryOperation<ArrayLiteral>, CqlNode {

    @JsonValue
    ArrayOperator getOperator();

    abstract class Builder extends BinaryOperation.Builder<ArrayLiteral, ArrayOperation> {}

}
