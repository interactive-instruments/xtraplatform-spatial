/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.stream.ActorMaterializer;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author zahnen
 */
public class SqlFeatureRemover {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFeatureCreator.class);

    private final SlickSession session;
    private final ActorMaterializer materializer;

    public SqlFeatureRemover(SlickSession session, ActorMaterializer materializer) {
        this.session = session;
        this.materializer = materializer;
    }

    public boolean remove(String featureType, String featureId, List<String> additionalQueries) {
        Integer rowsDeleted = Source.from(ImmutableList.of(featureId))
                                    //TODO: derive from SqlPathTree
                                    .via(Slick.flow(session, id -> String.format("DELETE FROM osirisobjekt WHERE id=(SELECT id FROM %s WHERE id=%s)", featureType, id)))
                                    .runWith(Sink.fold(0, (prev, next) -> prev + next),
                                            materializer)
                                    .toCompletableFuture()
                                    .join();

        Source.from(additionalQueries)
              .via(Slick.flow(session, query -> query))
              .runWith(Sink.ignore(),
                      materializer)
              .toCompletableFuture()
              .join();

        return rowsDeleted > 0;
    }
}
