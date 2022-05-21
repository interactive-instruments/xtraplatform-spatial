/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

public interface BinaryScalarOperation extends BinaryOperation2<Scalar>, CqlNode {

    abstract class Builder<T extends BinaryScalarOperation> extends Operation.Builder<Scalar, T> {}

}
