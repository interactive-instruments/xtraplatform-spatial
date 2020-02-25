package de.ii.xtraplatform.cql.app;

import java.util.LinkedHashMap;
import java.util.Map;

public class CqlFunctionChecker extends CqlVisitorBase<Map<String, Integer>> {

    // keys are function names, values are number of arguments
    private final Map<String, Integer> allowedFunctions;
    private final Map<String, Integer> invalidFunctions = new LinkedHashMap<>();

    public CqlFunctionChecker(Map<String, Integer> allowedFunctions) {
        this.allowedFunctions = allowedFunctions;
    }

    // TODO: see CqlPropertyChecker

}
