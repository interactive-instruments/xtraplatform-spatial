/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLt.Builder.class)
public interface Lt extends BinaryScalarOperation, CqlNode {

  String TYPE = "<";

  @Override
  @Value.Derived
  default String getOp() {
    return TYPE;
  }

  static Lt of(List<Scalar> operands) {
    return new ImmutableLt.Builder().args(operands).build();
  }

  static Lt of(String property, ScalarLiteral scalarLiteral) {
    return new ImmutableLt.Builder()
        .args(ImmutableList.of(Property.of(property), scalarLiteral))
        .build();
  }

  static Lt of(String property, String property2) {
    return new ImmutableLt.Builder()
        .args(ImmutableList.of(Property.of(property), Property.of(property2)))
        .build();
  }

  static Lt of(Property property, ScalarLiteral scalarLiteral) {
    return new ImmutableLt.Builder().args(ImmutableList.of(property, scalarLiteral)).build();
  }

  static Lt of(Property property, Property property2) {
    return new ImmutableLt.Builder().args(ImmutableList.of(property, property2)).build();
  }

  static Lt ofFunction(Function function, ScalarLiteral scalarLiteral) {
    return new ImmutableLt.Builder().args(ImmutableList.of(function, scalarLiteral)).build();
  }

  abstract class Builder extends BinaryScalarOperation.Builder<Lt> {}
}
