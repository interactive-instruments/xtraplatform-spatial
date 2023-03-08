/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.schemas.domain;

import java.io.File;
import java.net.URI;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;

public class JsonSchemaParser {
  private final SchemaStore schemaStore;

  public JsonSchemaParser() {
    this.schemaStore = new SchemaStore();
  }

  public Schema parse(File file) {
    try {
      return schemaStore.loadSchema(file);
    } catch (SchemaException e) {
      throw new IllegalArgumentException("Could not parse schema", e);
    }
  }

  public Schema parseUri(URI uri) {
    try {
      return schemaStore.loadSchema(uri);
    } catch (SchemaException e) {
      throw new IllegalArgumentException("Could not parse schema", e);
    }
  }
}
