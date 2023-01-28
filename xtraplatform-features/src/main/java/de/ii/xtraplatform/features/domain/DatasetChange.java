/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public interface DatasetChange {

  List<String> getFeatureTypes();

  @Value.Derived
  default Instant getModified() {
    return Instant.now().truncatedTo(ChronoUnit.SECONDS);
  }
}
