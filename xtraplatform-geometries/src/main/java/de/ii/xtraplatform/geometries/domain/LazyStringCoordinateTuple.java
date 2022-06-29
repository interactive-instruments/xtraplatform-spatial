/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import de.ii.xtraplatform.crs.domain.CoordinateTuple;

/**
 * @author zahnen
 */
public class LazyStringCoordinateTuple extends CoordinateTuple {

  private String[] cs;
  private boolean resolved;

  public LazyStringCoordinateTuple() {
    super();
    this.cs = new String[2];
    this.resolved = false;
  }

  public void setX(String x) {
    this.resolved = false;
    this.cs[0] = x;
    this.used[0] = true;
  }

  public void setY(String y) {
    this.resolved = false;
    this.cs[1] = y;
    this.used[1] = true;
  }

  public void appendX(String x) {
    this.cs[0] = this.cs[0].concat(x);
    this.used[0] = true;
  }

  public void appendY(String y) {
    this.cs[1] = this.cs[1].concat(y);
    this.used[1] = true;
  }

  @Override
  public double[] asArray() {
    if (!resolved) {
      resolve();
    }
    return super.asArray();
  }

  @Override
  public double getX() {
    if (!resolved) {
      resolve();
    }
    return super.getX();
  }

  @Override
  public double getY() {
    if (!resolved) {
      resolve();
    }
    return super.getY();
  }

  @Override
  public String getXasString() {
    return cs[0];
  }

  @Override
  public String getYasString() {
    return cs[1];
  }

  private void resolve() {
    if (cs[0] != null) this.c[0] = Double.parseDouble(cs[0]);
    if (cs[1] != null) this.c[1] = Double.parseDouble(cs[1]);
    this.resolved = true;
  }
}
