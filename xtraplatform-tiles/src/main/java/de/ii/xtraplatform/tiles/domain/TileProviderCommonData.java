/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import java.util.Objects;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTileProviderCommonData.Builder.class)
public interface TileProviderCommonData extends TileProviderData {

  @Override
  default TileProviderData mergeInto(TileProviderData source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderCommonData)) return this;

    TileProviderCommonData src = (TileProviderCommonData) source;

    ImmutableTileProviderCommonData.Builder builder =
        new ImmutableTileProviderCommonData.Builder().from(src).from(this);

    /*List<String> tileEncodings =
        Objects.nonNull(src.getTileEncodings())
            ? Lists.newArrayList(src.getTileEncodings())
            : Lists.newArrayList();
    getTileEncodings()
        .forEach(
            tileEncoding -> {
              if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
              }
            });
    builder.tileEncodings(tileEncodings);*/

    return builder.build();
  }

  abstract class Builder implements EntityDataBuilder<TileProviderCommonData> {}
}
