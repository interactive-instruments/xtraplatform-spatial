/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.Function;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CqlFunctionChecker extends CqlVisitorBase<Map<String, Integer>> {

    // keys are function names, values are number of arguments
    private final Map<String, Integer> allowedFunctions;
    private final Map<String, Integer> invalidFunctions = new LinkedHashMap<>();

    public CqlFunctionChecker(Map<String, Integer> allowedFunctions) {
        this.allowedFunctions = allowedFunctions;
    }

    @Override
    public Map<String, Integer> visit(CqlFilter cqlFilter, List<Map<String, Integer>> children) {
        ImmutableMap<String, Integer> result = ImmutableMap.copyOf(invalidFunctions);
        invalidFunctions.clear();
        return result;
    }

    @Override
    public Map<String, Integer> visit(Function function, List<Map<String, Integer>> children) {
        String functionName = function.getName();
        if (!(allowedFunctions.containsKey(functionName) &&
                allowedFunctions.get(functionName).equals(function.getArguments().size()))) {
            invalidFunctions.put(functionName, function.getArguments().size());
        }
        return Maps.newHashMap();
    }

}
