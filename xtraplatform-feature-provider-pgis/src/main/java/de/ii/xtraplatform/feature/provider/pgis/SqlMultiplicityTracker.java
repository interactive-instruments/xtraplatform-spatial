/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class SqlMultiplicityTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlMultiplicityTracker.class);

    private final Map<String, String> currentIds;
    private final Map<String, Integer> currentMultiplicities;
    private final Map<String, Set<String>> children;

    public SqlMultiplicityTracker(Set<String> multiTables) {
        this.currentIds = new HashMap<>();
        this.currentMultiplicities = new HashMap<>();
        this.children = new LinkedHashMap<>();
        ;

        multiTables.forEach(table -> {
            currentIds.put(table, null);
        });
    }

    public void reset() {
        currentMultiplicities.clear();
        children.clear();

        currentIds.keySet()
                  .forEach(table -> {
                      currentIds.put(table, null);
                  });
    }

    //TODO test [..., reset all children (ortsangaben -> ortsangaben_flurstueckskennzeichen), ...]

    public void track(List<String> path, List<String> ids) {
        int multiplicityIndex = 0;
        boolean increased = false;
        String increasedMultiplicityKey = null;
        List<String> parentTables = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            String element = path.get(i);
            String table = element.substring(element.indexOf("]") + 1);

            if (currentIds.containsKey(table)) {
                LOGGER.debug("TRACKER {} {} {}", element, multiplicityIndex, ids);
                String id = ids.get(multiplicityIndex + 1);
                if (increased) {
                    currentIds.put(table, id);
                    currentMultiplicities.put(element, 1);
                } else if (currentIds.containsKey(table) && !Objects.equals(id, currentIds.get(table))) {
                    currentIds.put(table, id);
                    currentMultiplicities.put(element, currentMultiplicities.getOrDefault(element, 0) + 1);
                    increased = true;
                    increasedMultiplicityKey = element;
                } else {// if (currentMultiplicities.get(table) == 0) {
                    currentMultiplicities.putIfAbsent(element, 1);
                }

                children.putIfAbsent(element, new HashSet<>());
                if (multiplicityIndex > 0) {
                    parentTables.forEach(parent -> children.get(parent)
                                                           .add(element));
                }
                parentTables.add(element);

                multiplicityIndex++;
            }
        }

        //reset children that are not on the current path
        if (increased) {
            children.get(increasedMultiplicityKey)
                    .forEach(child -> {
                        if (!parentTables.contains(child))
                            currentMultiplicities.remove(child);
                    });
        }
    }

    public List<Integer> getMultiplicitiesForPath(List<String> path) {
        return path.stream()
                   //.map(element -> element.substring(element.indexOf("]") + 1))
                   .filter(element -> currentIds.containsKey(element.substring(element.indexOf("]") + 1)))
                   .map(element -> currentMultiplicities.getOrDefault(element, 1))
                   .collect(Collectors.toList());
    }
}
