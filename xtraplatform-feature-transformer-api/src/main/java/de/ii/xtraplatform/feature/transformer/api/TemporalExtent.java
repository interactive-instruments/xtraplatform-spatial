/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * @author zahnen
 */
public class TemporalExtent {
    private long start;
    private long end;

    // TODO: move to builder, see http://www.developerdave.co.uk/2016/05/using-the-builder-pattern-in-jackson/
    @JsonCreator
    public TemporalExtent(@JsonProperty("start") long start, @JsonProperty("end") long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    @JsonIgnore
    public long getComputedEnd() {
        return end == 0 ? Instant.now().toEpochMilli() : end;
    }

    // TODO: remove setters; immutable objects have to be ignored in DeepUpdater; check usage of @JsonMerge again
    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }

}
