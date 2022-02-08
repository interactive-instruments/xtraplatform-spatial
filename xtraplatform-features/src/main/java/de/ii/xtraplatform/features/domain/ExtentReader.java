/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import java.util.Optional;
import org.threeten.extra.Interval;

public interface ExtentReader {

    Stream<Optional<BoundingBox>> getExtent(FeatureStoreTypeInfo typeInfo);

  Stream<Optional<Interval>> getTemporalExtent(FeatureStoreTypeInfo typeInfo, String property);

  Stream<Optional<Interval>> getTemporalExtent(FeatureStoreTypeInfo typeInfo, String startProperty,
      String endProperty);
}
