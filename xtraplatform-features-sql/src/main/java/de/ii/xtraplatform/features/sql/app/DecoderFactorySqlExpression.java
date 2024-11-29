/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.features.domain.Decoder;
import de.ii.xtraplatform.features.domain.DecoderFactory;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class DecoderFactorySqlExpression implements DecoderFactory {

  public static final MediaType MEDIA_TYPE =
      MediaType.valueOf("application/vnd.ldproxy.sql-expression");
  public static final String CONNECTOR_STRING = "EXPRESSION";
  private static final Pattern SQL_FLAG = Pattern.compile("\\{sql=(?<SQL>.+?)\\}");

  private final AtomicInteger expressionCounter = new AtomicInteger(0);

  @Inject
  public DecoderFactorySqlExpression() {}

  @Override
  public MediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public Optional<String> getConnectorString() {
    return Optional.of(CONNECTOR_STRING);
  }

  @Override
  public Decoder createDecoder() {
    return Decoder.noop(CONNECTOR_STRING);
  }

  @Override
  public Tuple<String, String> parseSourcePath(
      String path, String column, String flags, String connectorSpec) {
    Matcher matcher = SQL_FLAG.matcher(flags);

    if (matcher.find()) {
      return Tuple.of(
          String.format("SQL__%s", expressionCounter.incrementAndGet()), matcher.group("SQL"));
    }

    return DecoderFactory.super.parseSourcePath(path, column, flags, connectorSpec);
  }
}
