/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeatureActionTrigger.Builder.class)
public abstract class FeatureActionTrigger {

  public abstract List<String> getOnDelete();

  public List<String> getOnDelete(String id) {
    return getOnDelete().stream()
        .map(query -> query.replaceAll("\\{\\{id\\}\\}", id))
        .collect(Collectors.toList());
  }
}
