/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author zahnen
 */
public class FeaturePathTracker {

  private static final Joiner DEFAULT_JOINER = Joiner.on('.').skipNulls();

  private final Joiner joiner;
  private final List<String> localPath;

  public FeaturePathTracker() {
    this.localPath = new ArrayList<>(64);
    this.joiner = DEFAULT_JOINER;
  }

  public FeaturePathTracker(String separator) {
    this.localPath = new ArrayList<>(64);
    this.joiner = Joiner.on(separator).skipNulls();
  }

  public void track(int depth) {
    shorten(depth);
  }

  public void track(String localName, int depth) {
    if (depth < 0) {
      return;
    }
    shorten(depth);

    track(localName);
  }

  private void shorten(final int depth) {
    if (depth < 0) {
      return;
    }
    if (depth < localPath.size()) {
      localPath.subList(depth, localPath.size()).clear();
    }
  }

  public void track(String localName) {
    localPath.add(localName);
  }

  public void track(List<String> path) {
    localPath.clear();
    localPath.addAll(path);
  }

  @Override
  public String toString() {
    if (localPath.isEmpty()) return "";
    return joiner.join(localPath);
  }

  public String toStringWithDefaultSeparator() {
    if (localPath.isEmpty()) return "";
    return DEFAULT_JOINER.join(localPath);
  }

  public List<String> asList() {
    if (localPath.isEmpty()) return ImmutableList.of();
    return ImmutableList.copyOf(localPath);
  }

  public boolean containedIn(List<String> path) {
    if (path.size() < localPath.size()) {
      return false;
    }

    return Objects.equals(path.subList(0, localPath.size()), localPath);
  }
}
