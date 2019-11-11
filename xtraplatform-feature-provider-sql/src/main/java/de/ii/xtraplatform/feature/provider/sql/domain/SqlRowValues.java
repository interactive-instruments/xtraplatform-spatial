package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.stream.alpakka.slick.javadsl.SlickRow;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SqlRowValues implements SqlRow {
    //protected final String id;
    protected final String name;
    protected final List<String> path;
    protected final List<List<String>> paths;
    protected final List<String> columns;
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
        this.paths = attributesContainer.getAttributePaths();
        this.path = paths.get(0)
                         .subList(0, paths.get(0)
                                          .size() - 1);
        this.columns = attributesContainer.getAttributes();

        this.ids = IntStream.range(0, attributesContainer.getSortKeys()
                                                         .size())
                            .mapToObj(i -> delegate.nextString())
                            .collect(Collectors.toList());

        this.idNames = attributesContainer.getSortKeys();

        this.values = IntStream.range(0, columns.size())
                               .mapToObj(i -> delegate.nextString())
                               .collect(Collectors.toList());
        this.priority = priority;
    }

    @Override
    public int compareTo(SqlRow rowCustom) {
        SqlRowValues row = (SqlRowValues) rowCustom;
        int size = 0;
        for (int i = 0; i < idNames.size() && i < row.idNames.size(); i++) {
            if (!idNames.get(i)
                        .equals(row.idNames.get(i))) {
                break;
            }
            size = i + 1;
        }
        //int size = Math.min(ids.size(), row.ids.size());
        //if (ids.size() != row.ids.size() && size > 1)
        //    size = size -1;
        int result = compareIdLists(ids.subList(0, size), row.ids.subList(0, size));


        return result == 0 ? priority - row.priority : result;
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
        if (columnCount >= columns.size()) {
            return Optional.empty();
        }
        /*if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("NEXT: {} {} {} {}", paths.get(columnCount), ids, columnCount, columns.get(columnCount));
        }*/
        return Optional.of(new SqlColumn(paths.get(columnCount), columns.get(columnCount), values.get(columnCount++)));
    }

    @Override
    public String toString() {
        return "SlickRowInfo{" +
                "id='" + ids + '\'' +
                "name='" + name + '\'' +
                ", values=" + values +
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
