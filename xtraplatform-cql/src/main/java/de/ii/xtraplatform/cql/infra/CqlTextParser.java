/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.infra;

import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlParseException;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class CqlTextParser {

    private CqlParser.CqlFilterContext parseToTree(String cql) {
        CqlLexer lexer = new CqlLexer(CharStreams.fromString(cql));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        CqlParser parser = new CqlParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        return parser.cqlFilter();
    }

    public CqlFilter parse(String cql, EpsgCrs defaultCrs) throws CqlParseException {
        return parse(cql, new CqlTextVisitor(defaultCrs));
    }

    public CqlFilter parse(String cql, CqlTextVisitor visitor) throws CqlParseException {
        try {
            CqlParser.CqlFilterContext cqlFilterContext = parseToTree(cql);

            return (CqlFilter) visitor.visit(cqlFilterContext);
        } catch (ParseCancellationException e) {
            throw new CqlParseException(e.getMessage());
        }
    }

    public static class ThrowingErrorListener extends BaseErrorListener {

        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

    public static abstract class CqlParserCustom extends Parser {

        public CqlParserCustom(TokenStream input) {
            super(input);
        }

        protected final Boolean isNotInsideNestedFilter(ParserRuleContext ctx) {

            while (ctx.parent != null) {
                ctx = (ParserRuleContext) ctx.parent;

                if (ctx instanceof CqlParser.NestedCqlFilterContext) {
                    return false;
                }
            }

            return true;
        }
    }
}
