lexer grammar CqlLexer;

/*
#=============================================================================#
# Definition of COMPARISON operators
#=============================================================================#
*/

ComparisonOperator : EQ | NEQ | LT | GT | LTEQ | GTEQ;

LT : '<';

EQ : '=';

GT : '>';

NEQ : LT GT;

GTEQ : GT EQ;

LTEQ : LT EQ;



/*
#=============================================================================#
# Definition of BOOLEAN literals
#=============================================================================#
*/

BooleanLiteral : 'true' | 'false';

/*
#=============================================================================#
# Definition of LOGICAL operators
#=============================================================================#
*/

AND : 'AND';

OR : 'OR';

NOT : 'NOT';

/*
#=============================================================================#
# Definition of COMPARISON operators
#=============================================================================#
*/

LIKE : 'LIKE';

BETWEEN : 'BETWEEN';

IS : 'IS';

NULL: 'NULL';

/*
#=============================================================================#
# Definition of SPATIAL operators
#=============================================================================#
*/

/*
# NOTE: The buffer operators (DWITHIN and BEYOND) are not included because
#       these are outside the scope of a "simple" core for CQL.  These
#       can be added as extensions.
#
*/
SpatialOperator : 'EQUALS' | 'DISJOINT' | 'TOUCHES' | 'WITHIN' | 'OVERLAPS'
                | 'CROSSES' | 'INTERSECTS' | 'CONTAINS';

/*
#=============================================================================#
# Definition of TEMPORAL operators
#=============================================================================#
*/

/*
# only support the ones from original cql
*/
TemporalOperator : 'AFTER' | 'BEFORE' | 'DURING' | 'TEQUALS';

/*
TemporalOperator : 'AFTER' | 'BEFORE' | 'BEGINS' | 'BEGUNBY' | 'TCONTAINS'
                 | 'DURING' | 'ENDEDBY' | 'ENDS' | 'TEQUALS' | 'MEETS'
                 | 'METBY' | 'TOVERLAPS' | 'OVERLAPPEDBY' | 'ANYINTERACTS'
                 | 'TINTERSECTS';
*/

/*
#=============================================================================#
# Definition of EXISTENCE/IN operators
#=============================================================================#
*/

EXISTS : 'EXISTS';

EXIST : 'EXIST';

DOES : 'DOES';

IN: 'IN';

/*
#=============================================================================#
# Definition of geometry types
#=============================================================================#
*/

POINT: 'POINT';

LINESTRING: 'LINESTRING';

POLYGON: 'POLYGON';

MULTIPOINT: 'MULTIPOINT';

MULTILINESTRING: 'MULTILINESTRING';

MULTIPOLYGON: 'MULTIPOLYGON';

GEOMETRYCOLLECTION: 'GEOMETRYCOLLECTION';

ENVELOPE: 'ENVELOPE';




CharacterStringLiteralStart : QUOTE -> more, mode(STR);// (Character)* QUOTE;

//CHANGE: support compound property names
PropertyName : Identifier (PERIOD Identifier)*;

/*CharacterLiteral : CharacterStringLiteral
                 | BitStringLiteral
                 | HexStringLiteral;*/

NumericLiteral : UnsignedNumericLiteral | SignedNumericLiteral;


/*
#=============================================================================#
# Definition of CHARACTER literals
#=============================================================================#
*/



Identifier : IdentifierStart (IdentifierPart)*;

//CHANGE: moved UNDERSCORE | OCTOTHORP | DOLLAR from identifierStart to identifierPart
IdentifierStart : ALPHA;

IdentifierPart : ALPHA | DIGIT | UNDERSCORE | OCTOTHORP | DOLLAR;





BitStringLiteral : 'B' QUOTE (BIT)* QUOTE;

HexStringLiteral : 'X' QUOTE (HEXIT)* QUOTE;

//Character : Str -> mode(STR) ;//ALPHA | DIGIT | SpecialCharacter | QuoteQuote | ' ';

QuoteQuote : QUOTE QUOTE;

/*
# NOTE: This production is supposed to be any alphabetic character from
#       the character set.
#
#       I use the A-Z, a-z range here as placeholders because:
#       (a) I have no idea how to indicate that alpha can be
#           any alphabetic UTF-8 character
#       (b) the validators I am using can only handle ASCII chars
#
*/
ALPHA : [A-Za-z];

DIGIT : [0-9];

/*SpecialCharacter : PERCENT | AMPERSAND | LEFTPAREN | RIGHTPAREN | ASTERISK
                 | PLUS | COMMA | MINUS | PERIOD | SOLIDUS | COLON
                 | SEMICOLON | LT | GT | EQ | QUESTIONMARK | UNDERSCORE
                 | VERTICALBAR | DOUBLEQUOTE ;*/

OCTOTHORP : '#';

DOLLAR : '$';

UNDERSCORE : '_';

DOUBLEQUOTE : '"';

PERCENT : '%';

AMPERSAND : '&';

QUOTE : '\'';

LEFTPAREN : '(';

RIGHTPAREN : ')';

ASTERISK : '*';

PLUS : '+';

COMMA : ',';

MINUS : '-';

PERIOD : '.';

SOLIDUS : '/';

COLON : ':';

SEMICOLON : ';';

QUESTIONMARK : '?';

VERTICALBAR : '|';

BIT : '0' | '1';

HEXIT : DIGIT | 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'a' | 'b' | 'c' | 'd' | 'e' | 'f';

/*
#=============================================================================#
# Definition of NUMERIC literals
#=============================================================================#
*/



UnsignedNumericLiteral : ExactNumericLiteral | ApproximateNumericLiteral;

SignedNumericLiteral : (Sign)? ExactNumericLiteral | ApproximateNumericLiteral;

ExactNumericLiteral : UnsignedInteger  (PERIOD (UnsignedInteger)? )?
                        |  PERIOD UnsignedInteger;

ApproximateNumericLiteral : Mantissa 'E' Exponent;

Mantissa : ExactNumericLiteral;

Exponent : SignedInteger;

SignedInteger : (Sign)? UnsignedInteger;

UnsignedInteger : (DIGIT)+;

Sign : PLUS | MINUS;

/*
#=============================================================================#
# Definition of TEMPORAL literals
#
# NOTE: Is the fact the time zones are supported too complicated for a
#       simple CQL?  Perhaps the "core" of CQL should just support UTC.
#=============================================================================#
*/

//CHANGE: allow intervals with /
TemporalLiteral : FullDate | FullDate 'T' UtcTime | FullDate 'T' UtcTime SOLIDUS FullDate 'T' UtcTime;

FullDate : DateYear '-' DateMonth '-' DateDay;

DateYear : DIGIT DIGIT DIGIT DIGIT;

DateMonth : DIGIT DIGIT;

DateDay : DIGIT DIGIT;

UtcTime : TimeHour ':' TimeMinute ':' TimeSecond (TimeZoneOffset)?;

TimeZoneOffset : 'Z' | Sign TimeHour;

TimeHour : DIGIT DIGIT;

TimeMinute : DIGIT DIGIT;

TimeSecond : DIGIT DIGIT (PERIOD (DIGIT)+)?;


/*
#=============================================================================#
# ANTLR ignore whitespace
#=============================================================================#
*/

WS : [ \t\r\n]+ -> skip;// channel(HIDDEN) ; // skip spaces, tabs, newlines


/*
#=============================================================================#
# ANTLR mode for CharacterStringLiteral with whitespaces
#=============================================================================#
*/

mode STR;

CharacterStringLiteral: '\'' -> mode(DEFAULT_MODE);

QuotedQuote: '\'\'' -> more;

Character : ~['] -> more; // (ALPHA | DIGIT | SpecialCharacter | QuoteQuote | ' ')
