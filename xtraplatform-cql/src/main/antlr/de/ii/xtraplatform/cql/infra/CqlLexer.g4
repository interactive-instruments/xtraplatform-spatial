lexer grammar CqlLexer;

/*
#=============================================================================#
# Enable case-insensitive grammars
#=============================================================================#
*/

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];


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

BooleanLiteral : T R U E | F A L S E;

/*
#=============================================================================#
# Definition of LOGICAL operators
#=============================================================================#
*/

AND : A N D;

OR : O R;

NOT : N O T;

/*
#=============================================================================#
# Definition of COMPARISON operators
#=============================================================================#
*/

LIKE : L I K E;

BETWEEN : B E T W E E N;

IS : I S;

NULL: N U L L;

/*
#=============================================================================#
# Definition of SPATIAL operators
#=============================================================================#
*/

SpatialOperator : S UNDERSCORE I N T E R S E C T S
                | S UNDERSCORE E Q U A L S
                | S UNDERSCORE D I S J O I N T
                | S UNDERSCORE T O U C H E S
                | S UNDERSCORE W I T H I N
                | S UNDERSCORE O V E R L A P S
                | S UNDERSCORE C R O S S E S
                | S UNDERSCORE C O N T A I N S;

/*
#=============================================================================#
# Definition of TEMPORAL operators
#=============================================================================#
*/

TemporalOperator : T UNDERSCORE A F T E R
                 | T UNDERSCORE B E F O R E
                 | T UNDERSCORE C O N T A I N S
                 | T UNDERSCORE D I S J O I N T
                 | T UNDERSCORE D U R I N G
                 | T UNDERSCORE E Q U A L S
                 | T UNDERSCORE F I N I S H E D B Y
                 | T UNDERSCORE F I N I S H E S
                 | T UNDERSCORE I N T E R S E C T S
                 | T UNDERSCORE M E E T S
                 | T UNDERSCORE M E T B Y
                 | T UNDERSCORE O V E R L A P P E D B Y
                 | T UNDERSCORE O V E R L A P S
                 | T UNDERSCORE S T A R T E D B Y
                 | T UNDERSCORE S T A R T S;

/*
#=============================================================================#
# Definition of ARRAY operators
#=============================================================================#
*/
ArrayOperator : A UNDERSCORE E Q U A L S
              | A UNDERSCORE C O N T A I N S
              | A UNDERSCORE C O N T A I N E D B Y
              | A UNDERSCORE O V E R L A P S;

/*
#=============================================================================#
# Definition of EXISTENCE/IN operators
#=============================================================================#
*/

EXISTS : E X I S T S;

EXIST : E X I S T;

DOES : D O E S;

IN: I N;

/*
#=============================================================================#
# Definition of geometry types
#=============================================================================#
*/

POINT: P O I N T;

LINESTRING: L I N E S T R I N G;

POLYGON: P O L Y G O N;

MULTIPOINT: M U L T I P O I N T;

MULTILINESTRING: M U L T I L I N E S T R I N G;

MULTIPOLYGON: M U L T I P O L Y G O N;

GEOMETRYCOLLECTION: G E O M E T R Y C O L L E C T I O N;

ENVELOPE: E N V E L O P E;




CharacterStringLiteralStart : QUOTE -> more, mode(STR);// (Character)* QUOTE;

CASEI: C A S E I;

ACCENTI: A C C E N T I;

/*CharacterLiteral : CharacterStringLiteral
                 | BitStringLiteral
                 | HexStringLiteral;*/

NumericLiteral : UnsignedNumericLiteral | SignedNumericLiteral;


/*
#=============================================================================#
# Definition of CHARACTER literals
#=============================================================================#
*/



Identifier : IdentifierStart (COLON | PERIOD | IdentifierPart)* | DOUBLEQUOTE Identifier DOUBLEQUOTE;

//CHANGE: moved UNDERSCORE | OCTOTHORP | DOLLAR from identifierStart to identifierPart
IdentifierStart : ALPHA;

IdentifierPart : ALPHA | DIGIT | UNDERSCORE | DOLLAR;

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

LEFTSQUAREBRACKET : '[';

RIGHTSQUAREBRACKET : ']';

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

HEXIT : DIGIT | A | B | C | D | E | F;

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

InstantLiteral : DateInstant | TimestampInstant;

DateInstant: DATE LEFTPAREN DateInstantString RIGHTPAREN;

DateInstantString : QUOTE FullDate QUOTE;

TimestampInstant : TIMESTAMP LEFTPAREN TimestampInstantString RIGHTPAREN;

TimestampInstantString : QUOTE FullDate 'T' UtcTime QUOTE;

Instant : FullDate | FullDate 'T' UtcTime | NOW LEFTPAREN RIGHTPAREN;

Interval : INTERVAL LEFTPAREN InstantParameter COMMA InstantParameter RIGHTPAREN;

InstantParameter : Identifier | DateInstantString | TimestampInstantString | QUOTE '..' QUOTE;

FullDate : DateYear '-' DateMonth '-' DateDay;

DateYear : DIGIT DIGIT DIGIT DIGIT;

DateMonth : DIGIT DIGIT;

DateDay : DIGIT DIGIT;

UtcTime : TimeHour ':' TimeMinute ':' TimeSecond Z;

TimeHour : DIGIT DIGIT;

TimeMinute : DIGIT DIGIT;

TimeSecond : DIGIT DIGIT (PERIOD (DIGIT)+)?;

NOW : N O W;

DATE : D A T E;

TIMESTAMP : T I M E S T A M P;

INTERVAL : I N T E R V A L;

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
