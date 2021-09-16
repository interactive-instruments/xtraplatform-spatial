/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.util.ArrayList;
import java.util.List;

public class FeatureTokenTransformerRemoveEmptyOptionals extends FeatureTokenTransformer {


  private final List<String> nestingStack;
  private final List<FeatureSchema> schemaStack;
  private final List<FeatureSchema> customSchemaStack;

  public FeatureTokenTransformerRemoveEmptyOptionals() {
    this.nestingStack = new ArrayList<>();
    this.schemaStack = new ArrayList<>();
    this.customSchemaStack = new ArrayList<>();
  }

  @Override
  public void onObjectStart(ModifiableContext context) {
    if (context.inGeometry() ) {
      openIfNecessary(context);
      super.onObjectStart(context);
      return;
    }

    FeatureSchema schema = context.schema().get();

    if (schema.isRequired() && schemaStack.isEmpty()) {
      getDownstream().onObjectStart(context);
    } else {
        nestingStack.add("O");
        schemaStack.add(schema);
        customSchemaStack.add(context.customSchema());
    }
  }

  @Override
  public void onObjectEnd(ModifiableContext context) {
    if (context.inGeometry()) {
      super.onObjectEnd(context);
      return;
    }

    if (schemaStack.isEmpty()) {
      getDownstream().onObjectEnd(context);
    } else {
      nestingStack.remove(nestingStack.size()-1);
      schemaStack.remove(schemaStack.size()-1);
      customSchemaStack.remove(customSchemaStack.size()-1);
    }
  }

  @Override
  public void onArrayStart(ModifiableContext context) {
    if (context.inGeometry()) {
      openIfNecessary(context);
      super.onArrayStart(context);
      return;
    }

    FeatureSchema schema = context.schema().get();

    if (schema.isRequired() && schemaStack.isEmpty()) {
      getDownstream().onArrayStart(context);
    } else {
      nestingStack.add("A");
      schemaStack.add(schema);
      customSchemaStack.add(context.customSchema());
    }
  }

  @Override
  public void onArrayEnd(ModifiableContext context) {
    if (context.inGeometry()) {
      super.onArrayEnd(context);
      return;
    }

    if (schemaStack.isEmpty()) {
      getDownstream().onArrayEnd(context);
    } else {
      nestingStack.remove(nestingStack.size()-1);
      schemaStack.remove(schemaStack.size()-1);
      customSchemaStack.remove(customSchemaStack.size()-1);
    }
  }

  @Override
  public void onValue(ModifiableContext context) {

    openIfNecessary(context);

    super.onValue(context);
  }

  private void openIfNecessary(ModifiableContext context) {
    if (!schemaStack.isEmpty()) {
      List<String> previousPath = context.path();
      FeatureSchema previousCustomSchema = context.customSchema();
      for (int i = 0; i < schemaStack.size(); i++) {
        FeatureSchema schema = schemaStack.get(i);
        FeatureSchema customSchema = customSchemaStack.get(i);
        context.pathTracker().track(schema.getFullPath());
        context.setCustomSchema(customSchema);

        if (nestingStack.get(i).equals("A")) {
          getDownstream().onArrayStart(context);
          context.setInArray(true);
        } else if (nestingStack.get(i).equals("O")) {
          getDownstream().onObjectStart(context);
          context.setInObject(true);
        }
      }
      nestingStack.clear();
      schemaStack.clear();
      customSchemaStack.clear();
      context.pathTracker().track(previousPath);
      context.setCustomSchema(previousCustomSchema);
    }
  }
}
