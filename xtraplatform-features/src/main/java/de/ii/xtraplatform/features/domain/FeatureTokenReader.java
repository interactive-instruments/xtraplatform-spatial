/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public class FeatureTokenReader<
    T extends SchemaBase<T>, U extends SchemaMappingBase<T>, V extends ModifiableContext<T, U>> {

  private final FeatureEventHandler<T, U, V> eventHandler;

  private FeatureTokenType currentType;
  private int contextIndex;
  private V context;
  private List<String> nestingStack;

  // TODO
  public FeatureTokenReader(FeatureEventConsumer eventConsumer) {
    this.eventHandler = null;
  }

  public FeatureTokenReader(FeatureEventHandler<T, U, V> eventHandler, V context) {
    this.eventHandler = eventHandler;
    this.context = context;
    this.nestingStack = new ArrayList<>();
  }

  public void onToken(Object token) {
    if (token instanceof FeatureTokenType) {
      if (Objects.nonNull(currentType)) {
        emitEvent();
      }
      initEvent((FeatureTokenType) token);
    } else {
      readContext(token);
    }

    if (token == FeatureTokenType.INPUT_END) {
      emitEvent();
    }
  }

  private void initEvent(FeatureTokenType token) {
    this.currentType = token;
    this.contextIndex = 0;

    context.pathTracker().track(0);
    context.setGeometryType(Optional.empty());
    context.setGeometryDimension(OptionalInt.empty());
    context.setValueType(Type.UNKNOWN);
    context.setValue(null);

    switch (currentType) {
      case OBJECT:
        this.context.setInObject(true);
        if (inArray()) {
          this.context
              .indexes()
              .set(
                  this.context.indexes().size() - 1,
                  this.context.indexes().get(this.context.indexes().size() - 1) + 1);
        }
        push("O");
        break;
      case ARRAY:
        this.context.setInArray(true);
        this.context.indexes().add(0);
        push("A");
        break;
      case VALUE:
        if (inArray()) {
          this.context
              .indexes()
              .set(
                  this.context.indexes().size() - 1,
                  this.context.indexes().get(this.context.indexes().size() - 1) + 1);
        }
        break;
      case ARRAY_END:
        this.context.indexes().remove(this.context.indexes().size() - 1);
        pop();
        if (!nestingStack.contains("A")) {
          this.context.setInArray(false);
        }
        break;
      case OBJECT_END:
        if (this.context.inGeometry()) {
          this.context.setInGeometry(false);
        }
        pop();
        if (!nestingStack.contains("O")) {
          this.context.setInObject(false);
        }
        break;
    }
  }

  private void readContext(Object context) {
    switch (currentType) {
      case INPUT:
        if (contextIndex == 0 && context instanceof Boolean) {
          this.context.metadata().isSingleFeature((Boolean) context);
        } else if (contextIndex == 0 && context instanceof Long) {
          this.context.metadata().numberReturned((Long) context);
        } else if (contextIndex == 1 && context instanceof Long) {
          this.context.metadata().numberMatched((Long) context);
        }
        break;
      case FEATURE:
        tryReadPath(context);
        break;
      case OBJECT:
        tryReadPath(context);
        if (contextIndex == 1 && context instanceof SimpleFeatureGeometry) {
          this.context.setGeometryType((SimpleFeatureGeometry) context);
          this.context.setInGeometry(true);
        } else if (contextIndex == 2 && context instanceof Integer) {
          this.context.setGeometryDimension((Integer) context);
        }
        break;
      case ARRAY:
        tryReadPath(context);
        break;
      case VALUE:
        tryReadPath(context);
        if (contextIndex == 1 && context instanceof String) {
          this.context.setValue((String) context);
        } else if (contextIndex == 2 && context instanceof SchemaBase.Type) {
          this.context.setValueType((SchemaBase.Type) context);
        }
        break;
      case ARRAY_END:
      case OBJECT_END:
      case FEATURE_END:
      case INPUT_END:
        break;
    }

    this.contextIndex++;
  }

  private void tryReadPath(Object context) {
    if (contextIndex == 0 && context instanceof List) {
      // TODO: too expensive
      this.context.pathTracker().track((List<String>) context);
    }
  }

  private void emitEvent() {
    switch (currentType) {
      case INPUT:
        eventHandler.onStart(context);
        break;
      case FEATURE:
        eventHandler.onFeatureStart(context);
        break;
      case OBJECT:
        eventHandler.onObjectStart(context);
        break;
      case ARRAY:
        eventHandler.onArrayStart(context);
        break;
      case VALUE:
        eventHandler.onValue(context);
        break;
      case ARRAY_END:
        eventHandler.onArrayEnd(context);
        break;
      case OBJECT_END:
        eventHandler.onObjectEnd(context);
        break;
      case FEATURE_END:
        eventHandler.onFeatureEnd(context);
        break;
      case INPUT_END:
        eventHandler.onEnd(context);
        break;
    }
  }

  private boolean inArray() {
    return !nestingStack.isEmpty() && nestingStack.get(nestingStack.size() - 1).equals("A");
  }

  private void push(String type) {
    nestingStack.add(type);
  }

  private void pop() {
    nestingStack.remove(nestingStack.size() - 1);
  }
}
