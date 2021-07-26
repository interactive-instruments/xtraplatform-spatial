package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.base.Splitter;
import de.ii.xtraplatform.dropwizard.domain.LambdaWithException;
import de.ii.xtraplatform.features.domain.FeatureEventHandler;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Objects;

public class GeometryDecoderWkt {

  private final FeatureEventHandler<ModifiableContext> handler;
  private final ModifiableContext context;

  public GeometryDecoderWkt(
      FeatureEventHandler<ModifiableContext> handler,
      ModifiableContext context) {
    this.handler = handler;
    this.context = context;
  }

  public void decode(String text) throws IOException {
    StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(text));
    final int char128 = 128;
    final int skip32 = 32;
    final int char255 = 255;
    // set tokenizer to NOT parse numbers
    tokenizer.resetSyntax();
    tokenizer.wordChars('a', 'z');
    tokenizer.wordChars('A', 'Z');
    tokenizer.wordChars(char128 + skip32, char255);
    tokenizer.wordChars('0', '9');
    tokenizer.wordChars('-', '-');
    tokenizer.wordChars('+', '+');
    tokenizer.wordChars('.', '.');
    tokenizer.wordChars(',', ',');
    tokenizer.wordChars(' ', ' ');
    //tokenizer.whitespaceChars(0, ' ');
    //tokenizer.commentChar('#');

    String type = getNextWord(tokenizer);
    int dimension = type.contains(" Z ") ? 3 : 2;
    type = type.replace(" Z ", "");
    SimpleFeatureGeometry geometryType = SimpleFeatureGeometryFromToWkt.fromString(type)
        .toSimpleFeatureGeometry();

    if (!geometryType.isValid()) {
      return;
    }

    context.setGeometryType(geometryType);
    context.setGeometryDimension(dimension);
    handler.onObjectStart(context);
    context.setInGeometry(true);
    //featureTransformer.onGeometryStart(geometry, geometryType, dimension);

    switch (geometryType) {
      case POINT:
        readCoordinates(tokenizer);
        break;
      case MULTI_POINT:
        readMultiPointText(tokenizer);
        break;
      case LINE_STRING:
        readCoordinates(tokenizer);
        break;
      case MULTI_LINE_STRING:
        readPolygonText(tokenizer);
        break;
      case POLYGON:
        readPolygonText(tokenizer);
        break;
      case MULTI_POLYGON:
        readMultiPolygonText(tokenizer);
        break;
      case GEOMETRY_COLLECTION:
        break;
    }

    context.setInGeometry(false);
    handler.onObjectEnd(context);
  }

  private static final Splitter COMMA_SPLITTER = Splitter.on(',')
      .omitEmptyStrings()
      .trimResults();
  private static final Splitter BLANK_SPLITTER = Splitter.on(' ')
      .omitEmptyStrings()
      .trimResults();
  private static final String EMPTY = "EMPTY";
  private static final String COMMA = ",";
  private static final String L_PAREN = "(";
  private static final String R_PAREN = ")";

  private String getNextWord(StreamTokenizer tokenizer) throws IOException {
    int type = tokenizer.nextToken();
    String value;
    switch (type) {
      case '(':
        value = L_PAREN;
        break;
      case ')':
        value = R_PAREN;
        break;
      case ',':
        value = COMMA;
        break;
      case StreamTokenizer.TT_WORD:
        String word = tokenizer.sval;
        if (word.equalsIgnoreCase(EMPTY)) {
          value = EMPTY;
          break;
        }
        value = word;
        break;
      default:
        // parseError("word", tokenizer);
        value = null;
        break;
    }
    return value;
  }

  // TODO: might use nested parentheses
  private void readMultiPointText(StreamTokenizer tokenizer) throws IOException {
    String nextToken = getNextEmptyOrOpener(tokenizer);
    if (!Objects.equals(nextToken, EMPTY)) {
      nextToken = getNextWord(tokenizer);
      handler.onArrayStart(context);
      COMMA_SPLITTER.splitToList(nextToken)
          .forEach(LambdaWithException.consumerMayThrow(point -> {
            context.setValueType(Type.STRING);
            context.setValue(point);
            handler.onValue(context);
          }));
      handler.onArrayEnd(context);
      nextToken = getNextCloserOrComma(tokenizer);
    }
  }

  private void readMultiPolygonText(StreamTokenizer tokenizer) throws IOException {
    String nextToken = getNextEmptyOrOpener(tokenizer);
    if (Objects.equals(nextToken, EMPTY)) {
      return;
    }
    handler.onArrayStart(context);
    readPolygonText(tokenizer);
    //handler.onArrayEnd(context);
    nextToken = getNextCloserOrComma(tokenizer);
    while (Objects.equals(nextToken, COMMA)) {
      //handler.onArrayStart(context);
      readPolygonText(tokenizer);

      nextToken = getNextCloserOrComma(tokenizer);
    }
    handler.onArrayEnd(context);
  }

  private void readPolygonText(StreamTokenizer tokenizer) throws IOException {
    String nextToken = getNextEmptyOrOpener(tokenizer);
    if (Objects.equals(nextToken, EMPTY)) {
      return;
    }
    handler.onArrayStart(context);
    readCoordinates(tokenizer);
    //handler.onArrayEnd(context);
    nextToken = getNextCloserOrComma(tokenizer);
    while (Objects.equals(nextToken, COMMA)) {
      //handler.onArrayStart(context);
      readCoordinates(tokenizer);

      nextToken = getNextCloserOrComma(tokenizer);
    }
    handler.onArrayEnd(context);
  }

  private void readCoordinates(StreamTokenizer tokenizer) throws IOException {
    String nextToken = getNextEmptyOrOpener(tokenizer);
    if (!Objects.equals(nextToken, EMPTY)) {
      nextToken = getNextWord(tokenizer);
      context.setValueType(Type.STRING);
      context.setValue(nextToken);
      handler.onValue(context);
      nextToken = getNextCloserOrComma(tokenizer);
    }
  }

  private String getNextEmptyOrOpener(StreamTokenizer tokenizer)
      throws IOException {
    String nextWord = getNextWord(tokenizer);
    if (EMPTY.equals(nextWord) || L_PAREN.equals(nextWord)) {
      return nextWord;
    }
    //parseError(EMPTY + " or " + L_PAREN, tokenizer);
    return null;
  }

  private String getNextCloserOrComma(StreamTokenizer tokenizer)
      throws IOException {
    String nextWord = getNextWord(tokenizer);
    if (COMMA.equals(nextWord) || R_PAREN.equals(nextWord)) {
      return nextWord;
    }
    //parseError(COMMA + " or " + R_PAREN, tokenizer);
    return null;
  }
}
