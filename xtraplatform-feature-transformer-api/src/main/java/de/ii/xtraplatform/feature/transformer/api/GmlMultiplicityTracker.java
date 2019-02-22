/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class GmlMultiplicityTracker {
    private final Map<String, Integer> currentMultiplicities;

    public GmlMultiplicityTracker() {
        this.currentMultiplicities = new HashMap<>();
    }

    public void reset() {
        currentMultiplicities.clear();
    }

    //TODO multilevel, mark multiple properties in mapping and pass multi-elements to constructor, see SqlMultiplicityMapping
    public void track(List<String> path) {
        if (!path.isEmpty()) {
            currentMultiplicities.put(path.get(0), currentMultiplicities.getOrDefault(path.get(0), 0) + 1);
        }
    }

    public List<Integer> getMultiplicitiesForPath(List<String> path) {
        return path.stream()
                   .limit(1)
                   .map(element -> currentMultiplicities.getOrDefault(element, 1))
                   .collect(Collectors.toList());
    }
}
