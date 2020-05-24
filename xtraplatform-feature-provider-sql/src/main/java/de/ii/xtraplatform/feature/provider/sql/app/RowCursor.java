package de.ii.xtraplatform.feature.provider.sql.app;


import com.google.common.collect.ImmutableList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RowCursor {

    private final Map<List<String>, List<Integer>> rows;

    public RowCursor(List<String> path) {
        this.rows = new LinkedHashMap<>();
        rows.put(path, ImmutableList.of(0));
    }

    public List<Integer> track(List<String> path, List<String> parentPath, Integer currentRow) {
        List<Integer> parentRows = rows.get(parentPath);
        List<Integer> newParentRows = ImmutableList.<Integer>builder().addAll(parentRows)
                                                                      .add(currentRow)
                                                                      .build();
        rows.put(path, newParentRows);

        return newParentRows;
    }

    public List<Integer> get(List<String> path) {
        return rows.get(path);
    }

    public int getCurrent(List<String> path) {
        return rows.get(path)
                   .get(rows.get(path)
                            .size() - 1);
    }

}
