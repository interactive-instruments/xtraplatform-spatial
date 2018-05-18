package de.ii.xtraplatform.ogc.api.wfs.client;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.ogc.api.wfs.client.GetFeature.RESULT_TYPE;

import java.util.ArrayList;
import java.util.List;

public class GetFeatureBuilder {
    private final List<WFSQuery2> queries;
    private Integer count;
    private Integer startIndex;
    private boolean hitsOnly;

    public GetFeatureBuilder() {
        this.queries = new ArrayList<>();
    }

    public GetFeatureBuilder query(WFSQuery2 query) {
        this.queries.add(query);
        return this;
    }

    public GetFeatureBuilder count(Integer count) {
        this.count = count;
        return this;
    }

    public GetFeatureBuilder startIndex(Integer startIndex) {
        this.startIndex = startIndex;
        return this;
    }

    public GetFeatureBuilder hitsOnly() {
        this.hitsOnly = true;
        return this;
    }

    public GetFeature build() {
        return new GetFeature(ImmutableList.copyOf(queries), count, startIndex, hitsOnly ? RESULT_TYPE.HITS : RESULT_TYPE.RESULT);
    }
}