/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import java.net.URI;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@BuildableMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureProviderWfsData.Builder.class)
public interface FeatureProviderWfsData extends FeatureProviderDataV2,
    WithConnectionInfo<ConnectionInfoWfsHttp> {

  @Override
  ConnectionInfoWfsHttp getConnectionInfo();

  // for json ordering
  @Override
  BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getTypes();

  abstract class Builder extends
      FeatureProviderDataV2.Builder<ImmutableFeatureProviderWfsData.Builder> implements
      EntityDataBuilder<FeatureProviderDataV2> {

    public abstract ImmutableFeatureProviderWfsData.Builder connectionInfo(
        ConnectionInfoWfsHttp connectionInfo);

    @Override
    public ImmutableFeatureProviderWfsData.Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .featureProviderType(EntityDataDefaults.PLACEHOLDER)
          .connectionInfo(new ImmutableConnectionInfoWfsHttp.Builder().uri(URI.create("http://www.example.com")).build());
    }
  }

}
