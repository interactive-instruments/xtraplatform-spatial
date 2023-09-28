/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
@AutoModule(single = true, encapsulate = true)
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = DocIgnore.class)
package de.ii.xtraplatform.features.graphql.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import de.ii.xtraplatform.docs.DocIgnore;
import org.immutables.value.Value;
