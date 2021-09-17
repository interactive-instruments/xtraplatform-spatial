/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRelation;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class AliasGenerator {

  public List<String> getAliases(SchemaSql schema) {
    char alias = 'A';

    if (schema.getRelation().isEmpty()) {
      return ImmutableList.of(String.valueOf(alias));
    }

    ImmutableList.Builder<String> aliases = new ImmutableList.Builder<>();

    for (SqlRelation relation : schema.getRelation()) {
      aliases.add(String.valueOf(alias++));
      if (relation.isM2N()) {
        aliases.add(String.valueOf(alias++));
      }
    }

    aliases.add(String.valueOf(alias++));

    return aliases.build();
  }

  public List<String> getAliases(SchemaSql schema, int level) {
    if (level > 0) {
      String prefix = IntStream.range(0, level)
          .mapToObj(i -> "A")
          .collect(Collectors.joining());

      return getAliases(schema)
          .stream()
          .map(s -> prefix + s)
          .collect(Collectors.toList());
    }

    return getAliases(schema);
  }
}
