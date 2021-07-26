/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

//TODO: is there any real use case for this?
public class FeatureTokenReader<T extends ModifiableContext> {

  private final FeatureEventHandler<T> eventHandler;

  private FeatureTokenType currentType;
  private int contextIndex;
  private T context;

  //TODO
  public FeatureTokenReader(FeatureEventConsumer eventConsumer) {
    this.eventHandler = null;
  }

  public FeatureTokenReader(FeatureEventHandler<T> eventHandler, T context) {
    this.eventHandler = eventHandler;
    this.context = context;
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
    context.setValueType(Type.UNKNOWN);
    context.setValue(null);
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
      //TODO: too expensive
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

}
