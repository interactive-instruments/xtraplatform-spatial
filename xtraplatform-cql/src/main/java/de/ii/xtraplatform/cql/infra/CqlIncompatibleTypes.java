/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.infra;

import java.util.List;

public class CqlIncompatibleTypes extends IllegalArgumentException {
  public CqlIncompatibleTypes(String cqlText, String type, List<String> expectedTypes) {
    super(String.format("Incompatible types in CQL2 filter. Found type '%s' in expression [%s]. Expected types: %s", type, cqlText, String.join(", ", expectedTypes)));
  }
  public CqlIncompatibleTypes(String cqlText, List<String> types, List<String> expectedTypes) {
    super(String.format("Incompatible types in CQL2 filter. Expression [%s] has types: %s. Expected types: %s", cqlText, String.join(", ", types), String.join(", ", expectedTypes)));
  }
}
