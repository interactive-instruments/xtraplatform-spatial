/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.base.Joiner;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class MultiplicityTracker {
  private static final Joiner JOINER = Joiner.on('.');

  private final Map<List<String>, String> arrayPaths;
  private final Map<String, List<String>> subPaths;
  private final Map<String, Integer> currentMultiplicities;

  public MultiplicityTracker(List<List<String>> arrayPaths) {
    this.arrayPaths =
        arrayPaths.stream()
            .map(path -> new SimpleEntry<>(path, JOINER.join(path)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    this.subPaths =
        this.arrayPaths.values().stream()
            .map(
                path ->
                    new SimpleEntry<>(
                        path,
                        this.arrayPaths.values().stream()
                            .filter(
                                subPath ->
                                    subPath.length() > path.length() && subPath.startsWith(path))
                            .collect(Collectors.toList())))
            .filter(entry -> !entry.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    this.currentMultiplicities = new HashMap<>();
  }

  public void reset() {
    currentMultiplicities.clear();
  }

  public void track(List<String> path) {
    if (!path.isEmpty() && arrayPaths.containsKey(path)) {
      String pathKey = arrayPaths.get(path);
      currentMultiplicities.put(pathKey, currentMultiplicities.getOrDefault(pathKey, 0) + 1);

      if (subPaths.containsKey(pathKey)) {
        for (String subPath : subPaths.get(pathKey)) {
          currentMultiplicities.put(subPath, 0);
        }
      }
    }
  }

  public List<Integer> getMultiplicitiesForPath(List<String> path) {
    List<Integer> indexes = new ArrayList<>();

    for (int i = 0; i < path.size(); i++) {
      List<String> subPath = path.subList(0, i + 1);
      if (arrayPaths.containsKey(subPath)) {
        String pathKey = arrayPaths.get(subPath);
        indexes.add(currentMultiplicities.getOrDefault(pathKey, 1));
      }
    }

    return indexes;
  }
}
