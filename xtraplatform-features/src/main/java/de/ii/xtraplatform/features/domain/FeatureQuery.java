/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.base.domain.ETag;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.features.domain.SchemaBase.Scope;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
public interface FeatureQuery extends TypeQuery, Query {

  @Value.Default
  default boolean returnsSingleFeature() {
    return false;
  }

  List<FeatureQueryExtension> getExtensions();

  @Value.Default
  default Scope getSchemaScope() {
    return SchemaBase.Scope.RETURNABLE;
  }

  Optional<ETag.Type> getETag();

  abstract class Builder {
    public abstract Builder addFilters(Cql2Expression element);

    public ImmutableFeatureQuery.Builder filter(Optional<? extends Cql2Expression> filter) {
      filter.ifPresent(this::addFilters);
      return (ImmutableFeatureQuery.Builder) this;
    }

    public ImmutableFeatureQuery.Builder filter(Cql2Expression filter) {
      this.addFilters(filter);
      return (ImmutableFeatureQuery.Builder) this;
    }
  }
}
