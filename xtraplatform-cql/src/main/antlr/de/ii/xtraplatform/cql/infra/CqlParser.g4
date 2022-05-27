parser grammar CqlParser;
options { tokenVocab=CqlLexer; superClass=CqlTextParser.CqlParserCustom; }

/*
#=============================================================================#
# A CQL2 filter is a logically connected boolean expression of one or more
# predicates.
# These rules differ from the CQL2 BNF to clarify the predecence of AND, OR,
# NOT, etc. and reliably parse an CQL2 expression into an abstract
# representation.
#=============================================================================#
*/

cqlFilter : booleanExpression EOF;
booleanExpression : booleanTerm | booleanExpression OR booleanTerm;
booleanTerm : booleanFactor | booleanTerm AND booleanFactor;
booleanFactor : (NOT)? booleanPrimary;
booleanPrimary : predicate
                | booleanLiteral
                | LEFTPAREN booleanExpression RIGHTPAREN;

/*
#=============================================================================#
# Nested filters are an extension to CQL2
#=============================================================================#
*/

nestedCqlFilter: {isNotInsideNestedFilter($ctx)}? booleanExpression;

/*
#=============================================================================#
#  CQL2 supports scalar, spatial, temporal and array predicates.
#=============================================================================#
*/

predicate : comparisonPredicate
            | spatialPredicate
            | temporalPredicate
            | arrayPredicate;

/*
#=============================================================================#
# A comparison predicate evaluates if two scalar expression statisfy the
# specified comparison operator.  The comparion operators include an operator
# to evaluate regular expressions (LIKE), a range evaluation operator and
# an operator to test if a scalar expression is NULL or not.
#=============================================================================#
*/

comparisonPredicate : binaryComparisonPredicate
                        | isLikePredicate
                        | isBetweenPredicate
                        | isInListPredicate
                        | isNullPredicate;

binaryComparisonPredicate : scalarExpression ComparisonOperator scalarExpression;

isLikePredicate :  characterExpression (NOT)? LIKE patternExpression;

characterExpression : characterClause
                    | propertyName
                    | function;

patternExpression : characterClause;

/*
#=============================================================================#
# UPPER() and LOWER() are extensions to CQL2
#=============================================================================#
*/
characterClause : characterLiteral
                | CASEI LEFTPAREN characterExpression RIGHTPAREN
                | ACCENTI LEFTPAREN characterExpression RIGHTPAREN
                | LOWER LEFTPAREN characterExpression RIGHTPAREN
                | UPPER LEFTPAREN characterExpression RIGHTPAREN;

isBetweenPredicate : numericExpression (NOT)? BETWEEN numericExpression AND numericExpression;

numericExpression : numericLiteral
                  | propertyName
                  | function
                  /*
                  | arithmeticExpression*/;

isInListPredicate : scalarExpression (NOT)? IN LEFTPAREN scalarExpression ( COMMA scalarExpression )* RIGHTPAREN;

/*
#=============================================================================#
# This differs from the CQL2 BNF. See scalarExpression for the reasons.
#=============================================================================#
*/

isNullPredicate : isNullOperand IS (NOT)? NULL;

isNullOperand : characterClause
              | numericLiteral
              | booleanLiteral
              | instantLiteral
              | geomLiteral
              | propertyName
              | function;

/*
#=============================================================================#
# A scalar expression is the property name, a character literal, a numeric
# literal, a boolean literal, an instant literal, a function invocation that
# returns a scalar value, or an arithmetic expression.
# This differs from the CQL2 rule. The use of booleanExpression is problematic,
# because everything is a booleanExpression. In addition, the CQL2 rule
# includes propertyName and function in every expression).
#=============================================================================#
*/
scalarExpression : propertyName
                 | characterClause
                 | numericLiteral
                 | booleanLiteral
                 | instantLiteral
                 | function
                 /*
                 | arithmeticExpression*/;

//CHANGE: support compound property names
//CHANGE: support nested filters
propertyName : (Identifier (LEFTSQUAREBRACKET nestedCqlFilter RIGHTSQUAREBRACKET)? PERIOD)* Identifier;

characterLiteral : CharacterStringLiteral;

numericLiteral : NumericLiteral;

booleanLiteral : BooleanLiteral;

/*
#=============================================================================#
# A spatial predicate evaluates if two spatial expressions satisfy the
# specified spatial operator.
#=============================================================================#
*/

spatialPredicate :  SpatialOperator LEFTPAREN geomExpression COMMA geomExpression RIGHTPAREN;

/*
# A geometric expression is a property name of a geometry-valued property,
# a geometric literal (expressed as WKT) or a function that returns a
# geometric value.
*/
geomExpression : propertyName
               | geomLiteral
               | function;

/*
#=============================================================================#
# Definition of GEOMETRIC literals
#
# NOTE: This is basically BNF that define WKT encoding; it would be nice
#       to instead reference some normative BNF for WKT.
#=============================================================================#
*/

geomLiteral: point
             | linestring
             | polygon
             | multiPoint
             | multiLinestring
             | multiPolygon
             | geometryCollection
             | envelope;

point : POINT LEFTPAREN coordinate RIGHTPAREN;

linestring : LINESTRING linestringDef;

linestringDef: LEFTPAREN coordinate (COMMA coordinate)* RIGHTPAREN;

polygon : POLYGON polygonDef;

polygonDef : LEFTPAREN linestringDef (COMMA linestringDef)* RIGHTPAREN;

multiPoint : MULTIPOINT LEFTPAREN coordinate (COMMA coordinate)* RIGHTPAREN;

multiLinestring : MULTILINESTRING LEFTPAREN linestringDef (COMMA linestringDef)* RIGHTPAREN;

multiPolygon : MULTIPOLYGON LEFTPAREN polygonDef (COMMA polygonDef)* RIGHTPAREN;

geometryCollection : GEOMETRYCOLLECTION LEFTPAREN geomLiteral (COMMA geomLiteral)* RIGHTPAREN;

envelope: ENVELOPE LEFTPAREN westBoundLon COMMA southBoundLat COMMA (minElev COMMA)? eastBoundLon  COMMA northBoundLat (COMMA maxElev)? RIGHTPAREN;

coordinate : xCoord yCoord (zCoord)?;

xCoord : NumericLiteral;

yCoord : NumericLiteral;

zCoord : NumericLiteral;

westBoundLon : NumericLiteral;

eastBoundLon : NumericLiteral;

northBoundLat : NumericLiteral;

southBoundLat : NumericLiteral;

minElev : NumericLiteral;

maxElev : NumericLiteral;


/*
#=============================================================================#
# A temporal predicate evaluates if two temporal expressions satisfy the
# specified temporal operator.
#=============================================================================#
*/
temporalPredicate : TemporalOperator LEFTPAREN temporalExpression COMMA temporalExpression RIGHTPAREN;

temporalExpression : temporalClause
                   | propertyName
                   | function;

temporalClause: instantLiteral | interval;

instantLiteral: DATE LEFTPAREN DateString RIGHTPAREN
              | TIMESTAMP LEFTPAREN TimestampString RIGHTPAREN
              | NOW LEFTPAREN RIGHTPAREN;

interval: INTERVAL LEFTPAREN intervalParameter COMMA intervalParameter RIGHTPAREN;

intervalParameter: propertyName
                 | DateString
                 | TimestampString
                 | DotDotString
                 | NOW LEFTPAREN RIGHTPAREN
                 | function;

/*
#=============================================================================#
# An array predicate evaluates if two array expressions statisfy the
# specified comparison operator.  The comparion operators include equality,
# not equal, less than, greater than, less than or equal, greater than or equal,
# superset, subset and overlap operators.
#=============================================================================#
*/

arrayPredicate: ArrayOperator LEFTPAREN arrayExpression COMMA arrayExpression RIGHTPAREN;

arrayExpression: propertyName | arrayClause | function;

arrayClause: LEFTSQUAREBRACKET RIGHTSQUAREBRACKET
           | LEFTSQUAREBRACKET arrayElement ( COMMA arrayElement )* RIGHTSQUAREBRACKET;

/*
#=============================================================================#
# This differs from the CQL2 BNF. See scalarExpression for the reasons.
#=============================================================================#
*/

arrayElement: characterClause
            | numericLiteral
            | booleanLiteral
            | temporalClause
            | arrayClause
            | propertyName
            | function
            /*
            | arithmeticExpression*/;

/*
#=============================================================================#
# Definition of a FUNCTION
#=============================================================================#
*/

function : Identifier argumentList;

argumentList : LEFTPAREN (positionalArgument)?  RIGHTPAREN;

positionalArgument : argument ( COMMA argument )*;

argument : characterClause
         | numericLiteral
         | booleanLiteral
         | geomLiteral
         | temporalClause
         | arrayClause
         | propertyName
         | function
         /*
         | arithmeticExpression*/;

/*
#=============================================================================#
# An arithemtic expression is an expression composed of an arithmetic
# operand (a property name, a number or a function that returns a number),
# an arithmetic operators (+,-,*,/) and another arithmetic operand.
#=============================================================================#
*/
/* IGNORE FOR NOW
arithmeticExpression : arithmeticExpression PLUS arithmeticTerm
                     | arithmeticExpression MINUS arithmeticTerm
                     | arithmeticTerm;
arithmeticTerm : arithmeticTerm ASTERISK powerTerm
               | arithmeticTerm SOLIDUS powerTerm
               | powerTerm;
powerTerm : arithmeticFactor CARET powerTerm
          | arithmeticFactor;
arithmeticFactor : LEFTPAREN arithmeticExpression RIGHTPAREN
                 | arithmeticOperand;
arithmeticOperand : numericLiteral
                  | propertyName
                  | function;
*/
