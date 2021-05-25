package de.ii.xtraplatform.features.geojson.app;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import de.ii.xtraplatform.features.domain.FeatureEventDecoder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO: how to handle name collisions for id or geometry
public class FeatureEventDecoderGeoJson extends FeatureEventDecoder<byte[]> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEventDecoderGeoJson.class);
  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private final JsonParser parser;
  private final ByteArrayFeeder feeder;
  private final JsonPathTracker pathTracker;

  private boolean started;
  private int depth = -1;
  private int featureDepth = 0;
  private boolean inFeature = false;
  private boolean inProperties = false;
  private boolean inGeometry = false;
  private int lastNameIsArrayDepth = 0;
  private boolean singleFeature;
  private int startArray = 0;
  private int endArray = 0;
  private Optional<SimpleFeatureGeometry> geometryType;

  private OptionalLong numberReturned;
  private OptionalLong numberMatched;

  public FeatureEventDecoderGeoJson() {
    try {
      this.parser = JSON_FACTORY.createNonBlockingByteArrayParser();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
    this.pathTracker = new JsonPathTracker();
    this.numberReturned = OptionalLong.empty();
    this.numberMatched = OptionalLong.empty();
  }


  @Override
  protected void init() {

  }

  @Override
  protected void cleanup() {
    feeder.endOfInput();
  }

  @Override
  public void onPush(byte[] bytes) {
    feedInput(bytes);
  }

  // for unit tests
  void parse(String data) throws Exception {
    byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
    feedInput(dataBytes);
    cleanup();
  }

  private void feedInput(byte[] data) {
    try {
      feeder.feedInput(data, 0, data.length);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    boolean feedMeMore = false;
    while (!feedMeMore) {
      feedMeMore = advanceParser();
    }
  }

  public boolean advanceParser() {

    boolean feedMeMore = false;

    try {
      JsonToken nextToken = parser.nextToken();
      String currentName = parser.currentName();

      //TODO: null is end-of-input
      if (Objects.isNull(nextToken)) {
        return true; // or completestage???
      }

      switch (nextToken) {
        case NOT_AVAILABLE:
          feedMeMore = true;
          break;

        case FIELD_NAME:
          break;

        case START_OBJECT:
          if (Objects.nonNull(currentName) && !started) {
            switch (currentName) {
              case "properties":
              case "geometry":
                startIfNecessary(false);
                break;
            }
          }
          if (Objects.nonNull(currentName) && started) {
            switch (currentName) {
              case "properties":
                inProperties = true;
                featureDepth = depth;
                pathTracker.track(1);
                break;
              case "geometry":
                inGeometry = true;
                pathTracker.track(currentName, 1);
                break;
              default:
                if (inProperties/* || inGeometry*/) {
                  pathTracker.track(currentName);
                }
                break;
            }
            // nested array_object start
          } else if (!pathTracker.asList()
              .isEmpty() && started) {
            getDownstream().onObjectStart(pathTracker.asList(), Optional.empty());
            // feature in collection start
          } else if (depth == featureDepth - 1 && inFeature) {
            //inFeature = false;
            getDownstream().onFeatureStart();
          }

          // nested object start?
          if (Objects.nonNull(currentName) || lastNameIsArrayDepth == 0) {
            depth += 1;
            if (depth > featureDepth && (inProperties /*|| inGeometry*/) && !Objects.equals(currentName, "properties")) {
              getDownstream().onObjectStart(pathTracker.asList(), Optional.empty());
            }
          }
          break;

        case START_ARRAY:
          // start features array
          if (depth == 0 && Objects.nonNull(currentName)) {
            switch (currentName) {
              case "features":
                startIfNecessary(true);
                break;
            }
            // start prop/coord array
          } else if (Objects.nonNull(currentName) && (inProperties || inGeometry)) {
            if (!inGeometry) {
              pathTracker.track(currentName);
            }
            lastNameIsArrayDepth += 1;
            depth += 1;

            getDownstream().onArrayStart(pathTracker.asList());
            // start nested geo array
          } else if (inGeometry) {
            if (endArray > 0) {
              for (int i = 0; i < endArray - 1; i++) {
                getDownstream().onArrayEnd();
              }
              endArray = 0;
            }
            depth += 1;
            startArray++;
          }
          break;

        case END_ARRAY:
          // end features array
          if (depth == 0 && Objects.nonNull(currentName)) {
            switch (currentName) {
              case "features":
                inFeature = false;
                break;
            }
            // end prop/coord array
          } else if (Objects.nonNull(currentName) && (inProperties || inGeometry)) {
            if (endArray > 0) {
              for (int i = 0; i < endArray - 1; i++) {
                getDownstream().onArrayEnd();
              }
              endArray = 0;
            }

            getDownstream().onArrayEnd();

            if (inGeometry)
              pathTracker.track(depth - featureDepth);
            depth -= 1;
            if (inProperties)
              pathTracker.track(depth - featureDepth);
            lastNameIsArrayDepth -= 1;
            // end nested geo array
          } else if (inGeometry) {
            endArray++;
            depth -= 1;
            pathTracker.track(depth - featureDepth + 1);
          }
          break;

        case END_OBJECT:

          // end nested object
          if (Objects.nonNull(currentName) || lastNameIsArrayDepth == 0) {
            if (depth > featureDepth && (inProperties || inGeometry) && !Objects.equals(currentName, "properties")) {
              getDownstream().onObjectEnd();
            }
            // end geo
            if (inGeometry)
              pathTracker.track(depth - featureDepth);

            depth -= 1;
          } else if (lastNameIsArrayDepth > 0) {
            getDownstream().onObjectEnd();
          }

          // end all
          if (depth == -1) {
            if (singleFeature) {
              getDownstream().onFeatureEnd();
            }
            getDownstream().onEnd();
            // end feature in collection
          } else if (depth == featureDepth - 1 && inFeature) {
            //inFeature = false;
            getDownstream().onFeatureEnd();
          } else if (inFeature) {
            //featureConsumer.onPropertyEnd(pathTracker.asList());
          }

          if (Objects.equals(currentName, "properties"))
            inProperties = false;
          if (Objects.equals(currentName, "geometry")) {
            inGeometry = false;
          }
          if (inProperties)
            pathTracker.track(depth - featureDepth);
          break;

        case VALUE_STRING:
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
        case VALUE_TRUE:
        case VALUE_FALSE:
          Type valueType = Type.UNKNOWN;
          switch (nextToken) {
            case VALUE_STRING:
              valueType = Type.STRING;
              break;
            case VALUE_NUMBER_INT:
              valueType = Type.INTEGER;
              break;
            case VALUE_NUMBER_FLOAT:
              valueType = Type.FLOAT;
              break;
            case VALUE_TRUE:
            case VALUE_FALSE:
              valueType = Type.BOOLEAN;
              break;
          }

          // feature or collection prop value
          if (depth == 0 && Objects.nonNull(currentName)) {
            switch (currentName) {
              case "numberReturned":
                numberReturned = OptionalLong.of(parser.getLongValue());
                break;
              case "numberMatched":
                numberMatched = OptionalLong.of(parser.getLongValue());
                break;
              case "type":
                if (!parser.getValueAsString()
                    .equals("Feature")) break;
              case "id":
                startIfNecessary(false);
                if (!currentName.equals("id")) break;

                pathTracker.track(currentName, 1);

                getDownstream().onValue(pathTracker.asList(), parser.getValueAsString(), valueType);
                break;
            }
            // feature id or props or geo value
          } else if (((inProperties || inGeometry)) || (inFeature && Objects.equals(currentName, "id"))) {

            if (Objects.nonNull(currentName)) {
              if (inGeometry && Objects.equals(currentName, "type")) {
                geometryType = Optional.of(GeoJsonGeometryType.forString(parser.getValueAsString()).toSimpleFeatureGeometry());
                getDownstream().onObjectStart(pathTracker.asList(), geometryType);
                break;
              }
              pathTracker.track(currentName);
            }

            if (inGeometry && startArray > 0) {
              for (int i = 0; i < startArray - 1; i++) {
                getDownstream().onArrayStart(pathTracker.asList());
              }
              startArray = 0;
            }

            getDownstream().onValue(pathTracker.asList(), parser.getValueAsString(), valueType);

            // feature id
            if (Objects.equals(currentName, "id"))
              pathTracker.track(1);
              // why reset depth?
            else if (!inGeometry) {
              pathTracker.track(depth - featureDepth);
            } else if (inGeometry)
              pathTracker.track(depth - featureDepth + 1);
          }
          break;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse GeoJSON: " + e.getMessage());
    }

    return feedMeMore;
  }

  private void startIfNecessary(boolean isCollection) {
    if (!started) {
      getDownstream().onStart(numberReturned, numberMatched);
      started = true;
      inFeature = true;
      if (isCollection) {
        featureDepth = 1;
      } else {
        singleFeature = true;
        getDownstream().onFeatureStart();
      }
    }
  }
}
