package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableList;

import java.util.Optional;

public class SqlRowMeta implements SqlRow {
    private final long count;
    private final long count2;
    private boolean done;
    private boolean done2;
    private boolean noNumberReturned;

    public SqlRowMeta(long count, long count2) {
        this.count = count;
        this.count2 = count2;
    }

    SqlRowMeta(long count2) {
        this.noNumberReturned = true;
        this.count = 0;
        this.count2 = count2;
    }

    @Override
    public Optional<SqlColumn> next() {
        if (!done) {
            this.done = true;
            return noNumberReturned ? Optional.empty() : Optional.of(new SqlColumn(ImmutableList.of(), "numberReturned", Long.toString(count)));
        }
        if (!done2) {
            this.done2 = true;
            return Optional.of(new SqlColumn(ImmutableList.of(), "numberMatched", Long.toString(count2)));
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "META";
    }

    @Override
    public int compareTo(SqlRow slickRowCustom) {
        return 0;
    }
}
