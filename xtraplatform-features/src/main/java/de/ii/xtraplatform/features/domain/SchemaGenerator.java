/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface SchemaGenerator extends Closeable {

  Map<String, List<String>> analyze();

  List<FeatureSchema> generate(
      Map<String, List<String>> types, Consumer<Map<String, List<String>>> tracker);
}
