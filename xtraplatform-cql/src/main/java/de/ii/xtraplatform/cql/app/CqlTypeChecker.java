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
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.cql.domain.ArrayLiteral;
import de.ii.xtraplatform.cql.domain.Between;
import de.ii.xtraplatform.cql.domain.BinaryArrayOperation;
import de.ii.xtraplatform.cql.domain.BinaryScalarOperation;
import de.ii.xtraplatform.cql.domain.BinarySpatialOperation;
import de.ii.xtraplatform.cql.domain.BinaryTemporalOperation;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.cql.domain.CqlNode;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.Eq;
import de.ii.xtraplatform.cql.domain.Function;
import de.ii.xtraplatform.cql.domain.ImmutableBetween;
import de.ii.xtraplatform.cql.domain.ImmutableEq;
import de.ii.xtraplatform.cql.domain.ImmutableGt;
import de.ii.xtraplatform.cql.domain.ImmutableGte;
import de.ii.xtraplatform.cql.domain.ImmutableIn;
import de.ii.xtraplatform.cql.domain.ImmutableLike;
import de.ii.xtraplatform.cql.domain.ImmutableLt;
import de.ii.xtraplatform.cql.domain.ImmutableLte;
import de.ii.xtraplatform.cql.domain.ImmutableNeq;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.IsNull;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.LogicalOperation;
import de.ii.xtraplatform.cql.domain.Not;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.cql.infra.CqlIncompatibleTypes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CqlTypeChecker extends CqlVisitorBase<Type> {

    private static final List<Type> NUMBER = ImmutableList.of(Type.Integer, Type.Long, Type.Double);
    private static final List<Type> INTEGER = ImmutableList.of(Type.Integer, Type.Long);
    private static final List<Type> TEXT = ImmutableList.of(Type.String);
    private static final List<Type> BOOLEAN = ImmutableList.of(Type.Boolean);
    private static final List<Type> TEMPORAL = ImmutableList.of(Type.LocalDate, Type.Instant, Type.Interval);
    private static final List<Type> INSTANT = ImmutableList.of(Type.LocalDate, Type.Instant);
    private static final List<Type> TIMESTAMP_IN_INTERVAL = ImmutableList.of(Type.Instant, Type.OPEN);
    private static final List<Type> DATE_IN_INTERVAL = ImmutableList.of(Type.LocalDate, Type.OPEN);
    private static final List<Type> SPATIAL = ImmutableList.of(Type.Geometry);
    private static final List<Type> ARRAY = ImmutableList.of(Type.List);
    private static final List<List<Type>> SCALAR = ImmutableList.of(NUMBER, TEXT, BOOLEAN, INSTANT);
    private static final List<List<Type>> SCALAR_ORDERED = ImmutableList.of(NUMBER, TEXT, INSTANT);
    private static final List<List<Type>> SCALAR_ARRAY = ImmutableList.of(ImmutableList.<Type>builder()
        .addAll(NUMBER)
        .addAll(TEXT)
        .addAll(BOOLEAN)
        .addAll(INSTANT)
        .addAll(ARRAY)
        .build());

    private static final Map<Class<?>,List<List<Type>>> COMPATIBILITY_PREDICATES =
        new ImmutableMap.Builder<Class<?>,List<List<Type>>>()
            .put(ImmutableEq.class, SCALAR)
            .put(ImmutableNeq.class, SCALAR)
            .put(ImmutableLt.class, SCALAR_ORDERED)
            .put(ImmutableLte.class, SCALAR_ORDERED)
            .put(ImmutableGt.class, SCALAR_ORDERED)
            .put(ImmutableGte.class, SCALAR_ORDERED)
            .put(ImmutableIn.class, SCALAR_ARRAY)
            .put(ImmutableLike.class, ImmutableList.of(TEXT))
            .put(ImmutableBetween.class, ImmutableList.of(NUMBER))
            .put(BinaryTemporalOperation.class, ImmutableList.of(TEMPORAL))
            .put(BinarySpatialOperation.class, ImmutableList.of(SPATIAL))
            .put(BinaryArrayOperation.class, ImmutableList.of(ARRAY))
            .build();

    private static final Map<String, Set<List<Type>>> COMPATIBILITY_FUNCTION =
        new ImmutableMap.Builder<String,Set<List<Type>>>()
            .put("INTERVAL", ImmutableSet.of(TIMESTAMP_IN_INTERVAL, DATE_IN_INTERVAL))
            .put("CASEI", ImmutableSet.of(TEXT))
            .put("ACCENTI", ImmutableSet.of(TEXT))
            .put("UPPER", ImmutableSet.of(TEXT))
            .put("LOWER", ImmutableSet.of(TEXT))
            .put("POSITION", ImmutableSet.of(INTEGER))
            .build();

    private final Map<String, String> propertyTypes;
    private final Cql cql;

    public CqlTypeChecker(Map<String, String> propertyTypes, Cql cql) {
        this.propertyTypes = propertyTypes;
        this.cql = cql;
    }

    @Override
    public Type visit(LogicalOperation logicalOperation, List<Type> children) {
        check(children, ImmutableList.of(Type.Boolean), logicalOperation);
        return Type.Boolean;
    }

    @Override
    public Type visit(Not not, List<Type> children) {
        check(children, ImmutableList.of(Type.Boolean), not);
        return Type.Boolean;
    }

    @Override
    public Type visit(IsNull isNull, List<Type> children) {
        return Type.Boolean;
    }

    @Override
    public Type visit(BinaryScalarOperation scalarOperation, List<Type> children) {
        checkOperation(scalarOperation, children);
        return Type.Boolean;
    }

    @Override
    public Type visit(In in, List<Type> children) {
        checkOperation(in, children);
        return Type.Boolean;
    }

    @Override
    public Type visit(Like like, List<Type> children) {
        checkOperation(like, children);
        return Type.Boolean;
    }

    @Override
    public Type visit(Between between, List<Type> children) {
        checkOperation(between, children);
        return Type.Boolean;
    }

    @Override
    public Type visit(BinaryTemporalOperation temporalOperation, List<Type> children) {
        checkOperation(temporalOperation, children);
        return Type.Boolean;
    }

    @Override
    public Type visit(BinarySpatialOperation spatialOperation, List<Type> children) {
        checkOperation(spatialOperation, children);
        return Type.Boolean;
    }

    @Override
    public Type visit(BinaryArrayOperation arrayOperation, List<Type> children) {
        checkOperation(arrayOperation, children);
        return Type.Boolean;
    }

    @Override
    public Type visit(Function function, List<Type> children) {
        checkFunction(function, children);
        return Type.valueOf(function.getType().getSimpleName());
    }

    @Override
    public Type visit(Property property, List<Type> children) {
        String schemaType = propertyTypes.get(property.getName());
        if (Objects.nonNull(schemaType))
            switch (schemaType) {
                case "STRING":
                    return Type.String;
                case "INTEGER":
                    return Type.Integer;
                case "FLOAT":
                    return Type.Double;
                case "BOOLEAN":
                    return Type.Boolean;
                case "DATETIME":
                    return Type.Instant;
                case "DATE":
                    return Type.LocalDate;
                case "GEOMETRY":
                    return Type.Geometry;
                case "VALUE_ARRAY":
                case "OBJECT_ARRAY":
                    return Type.List;
            }
        return Type.UNKNOWN;
    }

    @Override
    public Type visit(ScalarLiteral scalarLiteral, List<Type> children) {
        return Type.valueOf(scalarLiteral.getType().getSimpleName());
    }

    @Override
    public Type visit(TemporalLiteral temporalLiteral, List<Type> children) {
        if (temporalLiteral.getType()==Function.class)
            return ((Function)temporalLiteral.getValue()).accept(this);
        return Type.valueOf(temporalLiteral.getType().getSimpleName());

    }

    @Override
    public Type visit(SpatialLiteral spatialLiteral, List<Type> children) {
        return Type.valueOf(spatialLiteral.getType().getSimpleName());
    }

    @Override
    public Type visit(ArrayLiteral arrayLiteral, List<Type> children) {
        return Type.valueOf(arrayLiteral.getType().getSimpleName());
    }

    private void checkOperation(CqlNode node, List<Type> types) {
        final Type firstType = types.get(0);
        if (firstType==Type.UNKNOWN)
            return;
        final List<Type> otherTypes = types.subList(1, types.size());

        List<List<Type>> compatibilityLists = getCompatibilityLists(node.getClass());
        if (compatibilityLists.isEmpty())
            throw new CqlIncompatibleTypes(getText(node), firstType.schemaType(), ImmutableList.of());
        if (compatibilityLists.stream().noneMatch(list -> list.contains(firstType)))
            throw new CqlIncompatibleTypes(getText(node),
                                           firstType.schemaType(),
                                           asSchemaTypes(compatibilityLists.stream()
                                                             .flatMap(Collection::stream)
                                                             .collect(Collectors.toUnmodifiableList())));

        final List<Type> compatibleTypes = getCompatibilityLists(node.getClass())
            .stream()
            .filter(list -> list.contains(firstType))
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toUnmodifiableList());
        final List<Type> expectedTypes = ImmutableList.<Type>builder()
            .add(firstType)
            .addAll(compatibleTypes)
            .build();
        otherTypes.stream()
            .filter(type -> !expectedTypes.contains(type) && !type.equals(Type.UNKNOWN))
            .findFirst()
            .ifPresent(type -> {
                throw new CqlIncompatibleTypes(getText(node), type.schemaType(), asSchemaTypes(expectedTypes));
            });
    }

    private void checkFunction(Function function, List<Type> types) {
        final Set<List<Type>> expectedTypes = Objects.requireNonNullElse(COMPATIBILITY_FUNCTION.get(function.getName()),
                                                                         ImmutableSet.of());
        if (expectedTypes.stream()
            .noneMatch(typeList -> types.stream()
                .allMatch(type -> typeList.contains(type) || type.equals(Type.UNKNOWN)))) {
            throw new CqlIncompatibleTypes(getText(function), asSchemaTypes(types), asSchemaTypes(expectedTypes));
        }
    }

    private List<String> asSchemaTypes(List<Type> types) {
        return types.stream().map(Type::schemaType).distinct().collect(Collectors.toUnmodifiableList());
    }

    private Set<List<String>> asSchemaTypes(Set<List<Type>> types) {
        return types.stream()
            .map(typeList -> typeList.stream()
                .map(Type::schemaType)
                .distinct()
                .collect(Collectors.toUnmodifiableList()))
            .collect(Collectors.toUnmodifiableSet());
    }

    private void check(List<Type> types, List<Type> expectedTypes, CqlNode node) {
        types.stream()
            .filter(type -> !expectedTypes.contains(type))
            .findFirst()
            .ifPresent(type -> {
                throw new CqlIncompatibleTypes(getText(node), type.schemaType(),
                                               expectedTypes.stream().map(Type::schemaType).collect(Collectors.toUnmodifiableList()));
            });
    }

    private String getText(CqlNode node) {
        if (node instanceof Function) {
            return cql.write(Eq.of(ImmutableList.of((Function) node, ScalarLiteral.of("DUMMY"))), Cql.Format.TEXT)
                .replace(" = 'DUMMY'", "");
        }
        return cql.write((Cql2Expression) node, Cql.Format.TEXT);
    }

    List<List<Type>> getCompatibilityLists(Class<?> clazz) {
        if (COMPATIBILITY_PREDICATES.containsKey(clazz)) {
            return COMPATIBILITY_PREDICATES.get(clazz);
        }

        return COMPATIBILITY_PREDICATES.entrySet()
            .stream()
            .filter(entry -> entry.getKey().isAssignableFrom(clazz))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(ImmutableList.of());
    }
}
