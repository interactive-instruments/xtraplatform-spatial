/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.features.domain.SortKey.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slick.jdbc.PositionedResult;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//TODO: extensive unit tests for compareTo
class SqlRowSlick implements SqlRow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlRowSlick.class);

    private final List<Comparable<?>> ids;
    private final List<Comparable<?>> sortKeys;
    private List<String> sortKeyNames;
    private List<SortKey.Direction> sortKeyDirections;
    private final List<Object> values;
    private int priority;
    private FeatureStoreAttributesContainer attributesContainer;

    SqlRowSlick() {
        this.ids = new ArrayList<>(32);
        this.sortKeys = new ArrayList<>(32);
        this.values = new ArrayList<>(128);
    }

    @Override
    public List<Object> getValues() {
        return values;
    }

    @Override
    public String getName() {
        if (Objects.nonNull(attributesContainer)) {
            return attributesContainer.getPath().get(attributesContainer.getPath().size()-1);
        }
        return null;
    }

    @Override
    public List<String> getPath() {
        if (Objects.nonNull(attributesContainer)) {
            return attributesContainer.getPath();
        }
        return ImmutableList.of();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public List<Comparable<?>> getIds() {
        return ids;
    }

    @Override
    public List<Comparable<?>> getSortKeys() {
        return sortKeys;
    }

    @Override
    public List<String> getSortKeyNames() {
        return sortKeyNames;
    }

    @Override
    public List<List<String>> getColumnPaths() {
        if (Objects.nonNull(attributesContainer)) {
            return attributesContainer.getAttributePaths();
        }
        return ImmutableList.of();
    }

    //TODO: use result.nextObject when column type info is supported
    SqlRow read(PositionedResult result, SqlQueryOptions queryOptions) {
        this.priority = queryOptions.getContainerPriority();
        List<Class<?>> columnTypes;

        if (queryOptions.getAttributesContainer()
                        .isPresent()) {
            this.attributesContainer = queryOptions.getAttributesContainer()
                                                   .get();
            this.sortKeyNames = queryOptions.getSortKeys();
            this.sortKeyDirections = queryOptions.getSortDirections();
            columnTypes = queryOptions.getColumnTypes();

            for (int i = 0; i < sortKeyNames.size(); i++) {
                try {
                    Object id = result.nextObject();
                    if (id instanceof Comparable<?>) {
                        sortKeys.add((Comparable<?>)id);
                        if (i >= queryOptions.getCustomSortKeys().size()) {
                            ids.add((Comparable<?>) id);
                        }
                    } else {
                        LOGGER.error("Sort key '{}' has invalid type '{}'.", sortKeyNames.get(i), id.getClass());
                    }
                } catch (Throwable e) {
                    break;
                }
            }

        } else {
            columnTypes = queryOptions.getColumnTypes();
        }

        for (int i = 0; i < columnTypes.size(); i++) {
            try {
                values.add(getValue(result, columnTypes.get(i)));
            } catch (Throwable e) {
                break;
            }
        }

        return this;
    }

    private Object getValue(PositionedResult result, Class<?> type) {
        if (type == BigDecimal.class) return result.nextBigDecimal();
        if (type == Blob.class) return result.nextBlob();
        if (type == Byte.class) return result.nextByte();
        if (type == byte[].class) return result.nextBytes();
        if (type == Clob.class) return result.nextClob();
        if (type == Date.class) return result.nextDate();
        if (type == Double.class) return result.nextDouble();
        if (type == Float.class) return result.nextFloat();
        if (type == Integer.class) return result.nextInt();
        if (type == Long.class) return result.nextLong();
        if (type == Object.class) return result.nextObject();
        if (type == Short.class) return result.nextShort();
        if (type == String.class) return result.nextString();
        if (type == Time.class) return result.nextTime();
        if (type == Timestamp.class) return result.nextTimestamp();

        return result.nextString();
    }

    void clear() {
        this.values.clear();
        this.ids.clear();
        this.sortKeyNames = null;
        this.priority = 0;
        this.attributesContainer = null;
    }

    //TODO: move comparison to SqlRow
    @Override
    public int compareTo(SqlRow otherSqlRow) {
        if (Objects.isNull(attributesContainer)) {
            return -1;
        }

        int commonSortKeys = getNumberOfCommonElements(sortKeyNames, otherSqlRow.getSortKeyNames());
        int resultSortKeys = compareSortKeys(getSortKeys(), otherSqlRow.getSortKeys(), commonSortKeys,
            sortKeyDirections);
        int result = resultSortKeys == 0 ? priority - otherSqlRow.getPriority() : resultSortKeys;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Compare: {}[{}{}] <=> {}[{}{}] -> {}({})", getName(), sortKeyNames, sortKeys, otherSqlRow.getName(), otherSqlRow.getSortKeyNames(), otherSqlRow.getSortKeys(), result, resultSortKeys);
        }

        return result;
    }

    private static int getNumberOfCommonElements(List<String> list1, List<String> list2) {
        int size = 0;
        for (int i = 0; i < list1.size() && i < list2.size(); i++) {
            if (!Objects.equals(list1.get(i), list2.get(i))) {
                break;
            }
            size = i + 1;
        }
        return size;
    }

    private static int compareSortKeys(List<Comparable<?>> ids1, List<Comparable<?>> ids2,
        int numberOfIds,
        List<Direction> idColumnDirections) {
        for (int i = 0; i < numberOfIds; i++) {
            int result = 0;
            Comparable<?> id1 = ids1.get(i);
            int direction = idColumnDirections.get(i) == Direction.DESCENDING ? -1 : 1;

            if (id1 instanceof Integer) {
                result = ((Integer)id1).compareTo((Integer)ids2.get(i));
            } else if (id1 instanceof Long) {
                result = ((Long)id1).compareTo((Long)ids2.get(i));
            } else if (id1 instanceof Short) {
                result = ((Short)id1).compareTo((Short)ids2.get(i));
            } else {
                result = ((String)id1).compareTo((String)ids2.get(i));
            }
            if (result != 0) {
                return result * direction;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "SqlRowSlick{" +
                "ids='" + ids + '\'' +
                ", name='" + getName() + '\'' +
                ", values=" + values +
                ", priority=" + priority +
                '}';
    }

}
