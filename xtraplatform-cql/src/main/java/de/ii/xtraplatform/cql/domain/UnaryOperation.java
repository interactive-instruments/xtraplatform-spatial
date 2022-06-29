/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

public interface UnaryOperation<T extends Operand> extends Operation<T> {

  @Value.Check
  default void check() {
    int count = getArgs().size();
    Preconditions.checkState(
        count == 1, "an unary operation must have exactly one operand, found %s", count);
  }
}
