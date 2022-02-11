/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.cql.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CqlTypeChecker extends CqlVisitorBase<List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CqlTypeChecker.class);

    private static final String UNKNOWN = "UNKNOWN";

    private static final List<List<String>> SCALARS = ImmutableList.of(
        ImmutableList.of("String", UNKNOWN),
        ImmutableList.of("Boolean", UNKNOWN),
        ImmutableList.of("LocalDate", UNKNOWN),
        ImmutableList.of("Instant", UNKNOWN),
        ImmutableList.of("Integer", "Long", "Double", UNKNOWN)
    );

    private static final List<List<String>> SCALARS_ORDERED = ImmutableList.of(
        ImmutableList.of("String", UNKNOWN),
        ImmutableList.of("LocalDate", UNKNOWN),
        ImmutableList.of("Instant", UNKNOWN),
        ImmutableList.of("Integer", "Long", "Double", UNKNOWN)
    );

    private static final List<List<String>> NUMBERS = ImmutableList.of(
        ImmutableList.of("Integer", "Long", "Double", UNKNOWN)
    );

    private static final List<List<String>> TEXTS = ImmutableList.of(
        ImmutableList.of("String", UNKNOWN)
    );

    private static final List<List<String>> TEMPORALS = ImmutableList.of(
        ImmutableList.of("LocalDate", "Instant", "Interval", UNKNOWN)
    );

    private static final List<List<String>> TEMPORALS_INTERVAL = ImmutableList.of(
        ImmutableList.of("LocalDate", "Instant", "Interval", "OPEN", UNKNOWN)
    );

    private static final List<List<String>> SPATIALS = ImmutableList.of(
        // TODO geometry types, some of the operations only accept a subset or certain combinations of the geometry types
        ImmutableList.of("Geometry", UNKNOWN)
    );

    private static final List<List<String>> ARRAYS = ImmutableList.of(
        ImmutableList.of("List", UNKNOWN)
    );

    private static final Map<Class<?>,List<List<String>>> COMPATIBILITY =
        new ImmutableMap.Builder<Class<?>,List<List<String>>>()
            .put(ImmutableEq.class, SCALARS)
            .put(ImmutableNeq.class, SCALARS)
            .put(ImmutableLt.class, SCALARS_ORDERED)
            .put(ImmutableLte.class, SCALARS_ORDERED)
            .put(ImmutableGt.class, SCALARS_ORDERED)
            .put(ImmutableGte.class, SCALARS_ORDERED)
            .put(ImmutableLike.class, TEXTS)
            .put(ImmutableIn.class, SCALARS)
            .put(ImmutableBetween.class, NUMBERS)
            .put(ImmutableTemporalOperation.class, TEMPORALS)
            .put(ImmutableSpatialOperation.class, SPATIALS)
            .put(ImmutableArrayOperation.class, ARRAYS)
            .build();

    private final Map<String, String> propertyTypes;
    private final List<String> invalidPredicates = new ArrayList<>();
    private final Cql cql;

    public CqlTypeChecker(Map<String, String> propertyTypes, Cql cql) {
        this.propertyTypes = propertyTypes;
        this.cql = cql;
    }

    @Override
    public List<String> visit(CqlFilter cqlFilter, List<List<String>> children) {
        ImmutableList<String> result = ImmutableList.copyOf(invalidPredicates);
        invalidPredicates.clear();
        return result;
    }

    @Override
    public List<String> visit(BinaryScalarOperation scalarOperation, List<List<String>> children) {
        checkBinaryOperation(scalarOperation);
        return Lists.newArrayList();
    }

    @Override
    public List<String> visit(In in, List<List<String>> children) {
        final List<List<String>> compatibilityList = COMPATIBILITY.get(in.getClass());
        final String type1 = getType(in.getValue().orElse(null));
        in.getList().stream()
            .map(this::getType)
            .forEach(type2 -> checkOperation(compatibilityList, type1, type2, in));
        return Lists.newArrayList();
    }

    @Override
    public List<String> visit(Like like, List<List<String>> children) {
        final List<List<String>> compatibilityList = COMPATIBILITY.get(like.getClass());
        String type1 = getType(like.getOperands().get(0));
        String type2 = getType(like.getOperands().get(1));
        checkOperation(compatibilityList, type1, type2, like);
        return Lists.newArrayList();
    }

    @Override
    public List<String> visit(Between between, List<List<String>> children) {
        final List<List<String>> compatibilityList = COMPATIBILITY.get(between.getClass());
        final String type1 = getType(between.getValue().orElse(null));
        final String type2 = getType(between.getLower().orElse(null));
        final String type3 = getType(between.getUpper().orElse(null));
        checkOperation(compatibilityList, type1, type2, between);
        checkOperation(compatibilityList, type1, type3, between);
        return Lists.newArrayList();
    }

    @Override
    public List<String> visit(TemporalOperation temporalOperation, List<List<String>> children) {
        checkBinaryOperation(temporalOperation);
        return Lists.newArrayList();
    }

    @Override
    public List<String> visit(SpatialOperation spatialOperation, List<List<String>> children) {
        checkBinaryOperation(spatialOperation);
        return Lists.newArrayList();
    }

    @Override
    public List<String> visit(ArrayOperation arrayOperation, List<List<String>> children) {
        checkBinaryOperation(arrayOperation);
        return Lists.newArrayList();
    }

    @Override
    public List<String> visit(Function function, List<List<String>> children) {
        if (function.isInterval()) {
            checkFunction(TEMPORALS_INTERVAL,
                          function.getArguments().stream()
                              .map(this::getType)
                              .collect(Collectors.toUnmodifiableList()),
                          function);
        } else if (function.isUpper() || function.isLower() || function.isCasei() || function.isAccenti()) {
            checkFunction(TEXTS,
                          function.getArguments().stream()
                              .map(this::getType)
                              .collect(Collectors.toUnmodifiableList()),
                          function);
        }
        return Lists.newArrayList();
    }

    private void checkBinaryOperation(BinaryOperation<?> predicate) {
        final List<List<String>> compatibilityList = COMPATIBILITY.get(predicate.getClass());
        String type1 = getType(predicate.getOperands().get(0));
        String type2 = getType(predicate.getOperands().get(1));
        checkOperation(compatibilityList, type1, type2, predicate);
    }

    private void checkOperation(List<List<String>> compatibilityList, String type1, String type2, CqlNode node) {
        if (Objects.nonNull(compatibilityList) &&
            compatibilityList.stream()
                .noneMatch(compatibleTypes -> compatibleTypes.contains(type1) && compatibleTypes.contains(type2))) {
            String predicateText = cql.write(CqlFilter.of(node), Cql.Format.TEXT) + "; types: " + type1 + " / " + type2;
            if (!invalidPredicates.contains(predicateText))
                invalidPredicates.add(predicateText);
        }
    }

    private void checkFunction(List<List<String>> compatibilityList, List<String> types, Function function) {
        if (Objects.nonNull(compatibilityList) &&
            compatibilityList.stream()
                .noneMatch(compatibleTypes -> compatibleTypes.containsAll(types))) {
            String functionText = cql.write(CqlFilter.of(Eq.ofFunction(function,ScalarLiteral.of("DUMMY"))), Cql.Format.TEXT)
                .replace(" = 'DUMMY'", "") + "; types: " + String.join(" / ", types);
            if (!invalidPredicates.contains(functionText))
                invalidPredicates.add(functionText);
        }
    }

    private String getType(Operand operand) {
        if (Objects.isNull(operand)) {
            return UNKNOWN;
        } else if (operand instanceof Function) {
            return ((Function) operand).getType().getSimpleName();
        } else if (operand instanceof SpatialLiteral) {
            return "Geometry";
        } else if (operand instanceof Literal) {
            return ((Literal)operand).getType().getSimpleName();
        } else if (operand instanceof Property) {
            String schemaType = propertyTypes.get(((Property)operand).getName());
            if (Objects.nonNull(schemaType))
                switch (schemaType) {
                    case "STRING":
                        return "String";
                    case "INTEGER":
                        return "Integer";
                    case "FLOAT":
                        return "Double";
                    case "BOOLEAN":
                        return "Boolean";
                    case "DATETIME":
                        return "Instant";
                    case "DATE":
                        return "LocalDate";
                    case "GEOMETRY":
                        return "Geometry";
                    case "OBJECT":
                        return "Object";
                    case "VALUE_ARRAY":
                    case "OBJECT_ARRAY":
                        return "List";
                }
        }
        return UNKNOWN;
    }
}
