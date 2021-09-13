/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.features.domain.FeatureStoreMultiplicityTracker;
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
public class SqlMultiplicityTracker implements FeatureStoreMultiplicityTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlMultiplicityTracker.class);

    private final Map<String, Object> currentIds;
    private final Map<String, Integer> currentMultiplicities;
    private final Map<String, Set<String>> children;

    public SqlMultiplicityTracker(List<String> multiTables) {
        this.currentIds = new HashMap<>();
        this.currentMultiplicities = new HashMap<>();
        this.children = new LinkedHashMap<>();
        ;

        multiTables.forEach(table -> {
            currentIds.put(table, null);
        });
    }

    @Override
    public void reset() {
        currentMultiplicities.clear();
        children.clear();

        currentIds.keySet()
                  .forEach(table -> {
                      currentIds.put(table, null);
                  });
    }

    @Override
    public void track(List<String> path, List<Comparable<?>> ids) {
        int multiplicityIndex = 0;
        boolean increased = false;
        String increasedMultiplicityKey = null;
        List<String> parentTables = new ArrayList<>();

        for (String table : path) {
            if (currentIds.containsKey(table)) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("TRACKER {} {} {}", table, multiplicityIndex, ids);
                }

                Object id = ids.get(multiplicityIndex + 1);
                if (increased) {
                    currentIds.put(table, id);
                    currentMultiplicities.put(table, 1);
                } else if (currentIds.containsKey(table) && !Objects.equals(id, currentIds.get(table))) {
                    currentIds.put(table, id);
                    currentMultiplicities.put(table, currentMultiplicities.getOrDefault(table, 0) + 1);
                    increased = true;
                    increasedMultiplicityKey = table;
                } else {// if (currentMultiplicities.get(table) == 0) {
                    currentMultiplicities.putIfAbsent(table, 1);
                }

                children.putIfAbsent(table, new HashSet<>());
                if (multiplicityIndex > 0) {
                    parentTables.forEach(parent -> children.get(parent)
                                                           .add(table));
                }
                parentTables.add(table);

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

    @Override
    public List<Integer> getMultiplicitiesForPath(List<String> path) {
        return path.stream()
                   .filter(currentIds::containsKey)
                   .map(table -> currentMultiplicities.getOrDefault(table, 1))
                   .collect(Collectors.toList());
    }
}
