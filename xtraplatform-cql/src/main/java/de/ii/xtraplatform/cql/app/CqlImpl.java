/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlParseException;
import de.ii.xtraplatform.cql.infra.CqlTextParser;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import io.dropwizard.jackson.Jackson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@Provides
@Instantiate
public class CqlImpl implements Cql {

    private final CqlTextParser cqlTextParser;
    private final ObjectMapper cqlJsonMapper;

    public CqlImpl() {
        this.cqlTextParser = new CqlTextParser();

        SimpleModule module = new SimpleModule();
        module.addSerializer(Interval.class, new StdDelegatingSerializer(new IntervalConverter()));
        module.addSerializer(Instant.class, new StdDelegatingSerializer(new InstantConverter()));

        this.cqlJsonMapper = Jackson.newObjectMapper()
                                    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                                    .enable(SerializationFeature.INDENT_OUTPUT)
                                    .registerModule(module);
    }

    @Override
    public CqlFilter read(String cql, Format format) throws CqlParseException {
        switch (format) {

            case TEXT:
                return cqlTextParser.parse(cql, OgcCrs.CRS84);
            case JSON:
                try {
                    return cqlJsonMapper.readValue(cql, CqlFilter.class);
                } catch (IOException e) {
                    throw new CqlParseException(e.getMessage());
                }
        }

        throw new IllegalStateException();
    }

    @Override
    public String write(CqlFilter cql, Format format) {
        switch (format) {

            case TEXT:
                return cql.accept(new CqlToText());
            case JSON:
                try {
                    return cqlJsonMapper.writeValueAsString(cql);
                } catch (IOException e) {
                    throw new IllegalStateException();
                }
        }

        throw new IllegalStateException();
    }

    static class IntervalConverter extends StdConverter<Interval, List<String>> {

        @Override
        public List<String> convert(Interval value) {

            return ImmutableList.of(
                    value.getStart().toString(),
                    value.getEnd().toString()
            );
        }
    }

    static class InstantConverter extends StdConverter<Instant, String> {

        @Override
        public String convert(Instant value) {

            return value.toString();
        }
    }
}
