/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import javax.ws.rs.core.MediaType;

// TODO: only for byte decoders?
@AutoMultiBind
public interface DecoderFactory {

  @AutoMultiBind
  interface GeometryDecoderFactory {

    MediaType getGeometryMediaType();

    Decoder createGeometryDecoder();
  }

  @AutoMultiBind
  interface FeatureDecoderFactory {

    MediaType getMediaType();

    FeatureDecoder<byte[]> createFeatureDecoder();
  }

  // application/sql, application/x.wkt
  MediaType getMediaType();

  Decoder createDecoder();
}
