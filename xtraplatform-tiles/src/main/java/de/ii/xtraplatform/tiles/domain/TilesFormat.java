/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

public enum TilesFormat {
  MVT("pbf"),
  JPEG("jpg"),
  PNG("png"),
  WebP("webp"),
  TIFF("tiff");

  private final String mbtilesString;

  TilesFormat(String mbtilesString) {
    this.mbtilesString = mbtilesString;
  }

  public String asMbtilesString() {
    return mbtilesString;
  }

  public static TilesFormat of(String value) {
    switch (value) {
      case "mvt":
      case "MVT":
      case "pbf":
      case "PBF":
        return MVT;
      case "jpg":
      case "JPG":
      case "jpeg":
      case "JPEG":
        return JPEG;
      case "png":
      case "PNG":
        return PNG;
      case "webp":
      case "WebP":
      case "WEBP":
        return WebP;
      case "tiff":
      case "TIFF":
        return TIFF;
    }
    return null;
  }
}
