package de.ii.xtraplatform.feature.query.api;

import de.ii.xtraplatform.crs.api.EpsgCrs;

public class FeatureQueryBuilder {
    private String type;
    private EpsgCrs crs;
    private int limit;
    private int offset;
    private String filter;

    public FeatureQueryBuilder type(String type) {
        this.type = type;
        return this;
    }

    public FeatureQueryBuilder crs(EpsgCrs crs) {
        this.crs = crs;
        return this;
    }

    public FeatureQueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public FeatureQueryBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    public FeatureQueryBuilder filter(String filter) {
        this.filter = filter;
        return this;
    }

    public FeatureQuery build() {
        return new FeatureQuery(type, crs, limit, offset, filter);
    }
}