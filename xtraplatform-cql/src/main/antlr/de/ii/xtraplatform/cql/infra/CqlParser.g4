parser grammar CqlParser;
options { tokenVocab=CqlLexer; superClass=CqlTextParser.CqlParserCustom; }

/*
#=============================================================================#
# A CQL2 filter is a logically connected expression of one or more predicates.
#=============================================================================#
*/
cqlFilter : booleanValueExpression EOF;
nestedCqlFilter: {isNotInsideNestedFilter($ctx)}? booleanValueExpression;
booleanValueExpression : booleanTerm | booleanValueExpression OR booleanTerm;
booleanTerm : booleanFactor | booleanTerm AND booleanFactor;
booleanFactor : (NOT)? booleanPrimary;
booleanPrimary : predicate
                | LEFTPAREN booleanValueExpression RIGHTPAREN;

/*
#=============================================================================#
#  CQL2 supports scalar, spatial, temporal and array predicates.
#=============================================================================#
*/

predicate : comparisonPredicate
            | spatialPredicate
            | temporalPredicate
            | arrayPredicate
            | inPredicate;

/*
#=============================================================================#
# A comparison predicate evaluates if two scalar expression statisfy the
# specified comparison operator.  The comparion operators include an operator
# to evaluate regular expressions (LIKE), a range evaluation operator and
# an operator to test if a scalar expression is NULL or not.
#=============================================================================#
*/

comparisonPredicate : binaryComparisonPredicate
                        | propertyIsLikePredicate
                        | propertyIsBetweenPredicate
                        | propertyIsNullPredicate;

binaryComparisonPredicate : scalarExpression ComparisonOperator scalarExpression;

propertyIsLikePredicate :  characterExpression (NOT)? LIKE patternExpression;

characterExpression : characterClause
                    | propertyName
                    | function;

characterClause : characterLiteral
                | CASEI LEFTPAREN characterExpression RIGHTPAREN
                | ACCENTI LEFTPAREN characterExpression RIGHTPAREN
                | LOWER LEFTPAREN characterExpression RIGHTPAREN
                | UPPER LEFTPAREN characterExpression RIGHTPAREN;

propertyIsBetweenPredicate : numericExpression (NOT)? BETWEEN numericExpression AND numericExpression;

numericExpression : numericLiteral
                  | propertyName
                  | function
                  /*| arithmeticExpression*/;

propertyIsNullPredicate : scalarExpression IS (NOT)? NULL;

/*
# A scalar expression is the property name, a chracter literal, a numeric
# literal or a function/method invocation that returns a scalar value.
*/
scalarExpression : characterClause
                 | numericLiteral
                 | booleanLiteral
                 | instantLiteral
                 | propertyName
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
# NOTE: This is just a place holder for a regular expression
#       We want to be able to say stuff like "<prop> LIKE 'Toronto%'" where
#       the '%' character means "match zero or more characters".
*/
patternExpression : characterClause;

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

temporalExpression : propertyName
                   | temporalClause
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

arrayClause: LEFTSQUAREBRACKET arrayElement ( COMMA arrayElement )* RIGHTSQUAREBRACKET;

arrayElement: characterClause | numericLiteral | booleanLiteral | temporalClause | propertyName | arrayClause |function;


/*
#=============================================================================#
# The IN predicate
#=============================================================================#
*/
//CHANGE: optional PropertyName for id filters
//CHANGE: added missing comma
inPredicate : scalarExpression (NOT)? IN LEFTPAREN scalarExpression ( COMMA scalarExpression )* RIGHTPAREN;

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
         | propertyName
         | function
         | arrayExpression
         /*| arithmeticExpression*/;

/*
#=============================================================================#
# An arithemtic expression is an expression composed of an arithmetic
# operand (a property name, a number or a function that returns a number),
# an arithmetic operators (+,-,*,/) and another arithmetic operand.
#=============================================================================#
*/
/* IGNORE FOR NOW
arithmeticExpression : arithmeticOperand arithmeticOperator arithmeticOperand;

arithmeticOperator : plusSign | minusSign | asterisk | solidus;

arithemticOperator : propertyName
                   | numericLiteral
                   | function;
*/
