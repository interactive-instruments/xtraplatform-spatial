/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class JacksonXmlAnnotation {
  @Retention(RetentionPolicy.RUNTIME)
  public @interface XmlIgnore {}
}
