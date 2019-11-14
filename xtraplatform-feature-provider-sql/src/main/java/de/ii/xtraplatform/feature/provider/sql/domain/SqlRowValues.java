package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.stream.alpakka.slick.javadsl.SlickRow;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SqlRowValues implements SqlRow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlRowValues.class);

    //protected final String id;
    protected final String name;
    protected final List<String> path;
    protected final List<List<String>> columnPaths;
    protected final List<String> values;
    protected final List<String> ids;
    protected final List<String> idNames;
    protected int columnCount = 0;
    protected final int priority;

    //TODO: no need for next impl, just return values list
    //TODO: reusable Modifiable
    public SqlRowValues(SlickRow delegate, FeatureStoreAttributesContainer attributesContainer, int priority) {
        //this.id = delegate.nextString();
        this.name = attributesContainer.getName();

        if (name.equals("meta")) {
            this.columnPaths = attributesContainer.getAttributePaths();
            this.path = attributesContainer.getPath();
            this.ids = ImmutableList.of();
            this.idNames = ImmutableList.of();

            this.values = IntStream.range(0, columnPaths.size())
                                   .mapToObj(i -> delegate.nextString())
                                   .collect(Collectors.toList());
            this.priority = priority;
        } else {
            this.columnPaths = attributesContainer.getAttributePaths();
            this.path = attributesContainer.getPath();

            this.ids = IntStream.range(0, attributesContainer.getSortKeys()
                                                             .size())
                                .mapToObj(i -> delegate.nextString())
                                .collect(Collectors.toList());

            this.idNames = attributesContainer.getSortKeys();

            this.values = IntStream.range(0, columnPaths.size())
                                   .mapToObj(i -> delegate.nextString())
                                   .collect(Collectors.toList());
            this.priority = priority;
        }
    }

    @Override
    public int compareTo(SqlRow row) {
        if (row instanceof SqlRowMeta) {
            return 1;
        }

        SqlRowValues valueRow = (SqlRowValues) row;

        int size = 0;
        for (int i = 0; i < idNames.size() && i < valueRow.idNames.size(); i++) {
            if (!Objects.equals(idNames.get(i), valueRow.idNames.get(i))) {
                break;
            }
            size = i + 1;
        }

        int resultIds = compareIdLists(ids.subList(0, size), valueRow.ids.subList(0, size));
        int result = resultIds == 0 ? priority - valueRow.priority : resultIds;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Compare: {}[{}{}] <=> {}[{}{}] -> {}({})", name, idNames, ids, valueRow.name, valueRow.idNames, valueRow.ids, result, resultIds);
        }

        return result;
    }


    @Override
    public List<String> getIds() {
        return ids;
    }

    @Override
    public List<String> getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<SqlColumn> next() {
        if (columnCount >= columnPaths.size()) {
            return Optional.empty();
        }
        /*if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("NEXT: {} {} {} {}", paths.get(columnCount), ids, columnCount, columns.get(columnCount));
        }*/
        return Optional.of(new SqlColumn(columnPaths.get(columnCount), values.get(columnCount++)));
    }

    @Override
    public String toString() {
        return "SlickRowInfo{" +
                "ids='" + ids + '\'' +
                ", name='" + name + '\'' +
                ", values=" + values +
                ", priority=" + priority +
                '}';
    }

    static int compareIdLists(List<String> ids1, List<String> ids2) {
        for (int i = 0; i < ids1.size(); i++) {
            int result = Integer.valueOf(ids1.get(i))
                                .compareTo(Integer.valueOf(ids2.get(i)));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
}
