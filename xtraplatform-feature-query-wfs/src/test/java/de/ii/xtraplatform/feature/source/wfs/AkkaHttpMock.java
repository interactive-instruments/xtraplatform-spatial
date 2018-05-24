/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.source.wfs;

import akka.NotUsed;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.akka.http.AkkaHttp;

/**
 * @author zahnen
 */
public class AkkaHttpMock extends AkkaHttp {
    private final ActorMaterializer materializer;
    private final ByteString byteString;

    public AkkaHttpMock(ActorMaterializer materializer, ByteString byteString) {
        this.materializer = materializer;
        this.byteString = byteString;
    }

    @Override
    public ActorMaterializer getMaterializer() {
        return materializer;
    }

    @Override
    public Source<ByteString, NotUsed> get(String url) {
        return Source.from(ImmutableList.of(byteString));
    }
}
