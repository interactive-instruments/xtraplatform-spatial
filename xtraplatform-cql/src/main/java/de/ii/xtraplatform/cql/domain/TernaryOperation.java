/**
 * Copyright 2022 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

public interface TernaryOperation<T extends Operand> extends Operation<T> {

  @Value.Check
  default void check() {
    int count = getArgs().size();
    Preconditions.checkState(count == 3,
        "a ternary operation must have exactly three operands, found %s", count);
  }

}
