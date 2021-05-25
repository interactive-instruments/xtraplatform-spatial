package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public class FeatureTokenReader {

  private final FeatureEventConsumer eventConsumer;

  private FeatureTokenType currentType;
  private int contextIndex;

  private Long numberReturned;
  private Long numberMatched;

  private List<String> path;
  private String value;
  private SchemaBase.Type valueType;
  private Optional<SimpleFeatureGeometry> geometryType;

  public FeatureTokenReader(FeatureEventConsumer eventConsumer) {
    this.eventConsumer = eventConsumer;
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
  }

  private void initEvent(FeatureTokenType token) {
    this.currentType = token;
    this.contextIndex = 0;
    this.path = ImmutableList.of();
    this.geometryType = Optional.empty();
    this.valueType = Type.UNKNOWN;
    this.value = null;
  }

  private void readContext(Object context) {
    switch (currentType) {
      case INPUT:
        if (contextIndex == 0 && context instanceof Long) {
          this.numberReturned = (Long) context;
        } else if (contextIndex == 1 && context instanceof Long) {
          this.numberMatched = (Long) context;
        }
        break;
      case FEATURE:
        break;
      case OBJECT:
        tryReadPath(context);
        if (contextIndex == 1 && context instanceof SimpleFeatureGeometry) {
          this.geometryType = Optional.of((SimpleFeatureGeometry) context);
        }
        break;
      case ARRAY:
        tryReadPath(context);
        break;
      case VALUE:
        tryReadPath(context);
        if (contextIndex == 1 && context instanceof String) {
          this.value = (String) context;
        } else if (contextIndex == 2 && context instanceof SchemaBase.Type) {
          this.valueType = (SchemaBase.Type) context;
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
      this.path = (List<String>) context;
    }
  }

  private void emitEvent() {
    switch (currentType) {
      case INPUT:
        eventConsumer.onStart(Objects.nonNull(numberReturned) ? OptionalLong.of(numberReturned)
                : OptionalLong.empty(),
            Objects.nonNull(numberMatched) ? OptionalLong.of(numberMatched) : OptionalLong.empty());
        break;
      case FEATURE:
        eventConsumer.onFeatureStart();
        break;
      case OBJECT:
        eventConsumer.onObjectStart(path, geometryType);
        break;
      case ARRAY:
        eventConsumer.onArrayStart(path);
        break;
      case VALUE:
        eventConsumer.onValue(path, value, valueType);
        break;
      case ARRAY_END:
        eventConsumer.onArrayEnd();
        break;
      case OBJECT_END:
        eventConsumer.onObjectEnd();
        break;
      case FEATURE_END:
        eventConsumer.onFeatureEnd();
        break;
      case INPUT_END:
        eventConsumer.onEnd();
        break;
    }
  }

}
