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
# Definition of LIKE operator modifiers
#=============================================================================#
*/

WILDCARD : W I L D C A R D;

SINGLECHAR : S I N G L E C H A R;

ESCAPECHAR : E S C A P E C H A R;

NOCASE : N O C A S E;

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
SpatialOperator : E Q U A L S | D I S J O I N T | T O U C H E S | W I T H I N | O V E R L A P S
                | C R O S S E S | I N T E R S E C T S | C O N T A I N S;

/*
#=============================================================================#
# Definition of TEMPORAL operators
#=============================================================================#
*/

/*
# only support the ones from original cql for now
*/
TemporalOperator : A F T E R | B E F O R E | B E G I N S | B E G U N B Y | T C O N T A I N S | D U R I N G
                 | E N D E D B Y | E N D S | T E Q U A L S | M E E T S | M E T B Y | T O V E R L A P S
                 | O V E R L A P P E D B Y | A N Y I N T E R A C T S;

/*
TemporalOperator : 'AFTER' | 'BEFORE' | 'BEGINS' | 'BEGUNBY' | 'TCONTAINS'
                 | 'DURING' | 'ENDEDBY' | 'ENDS' | 'TEQUALS' | 'MEETS'
                 | 'METBY' | 'TOVERLAPS' | 'OVERLAPPEDBY' | 'ANYINTERACTS'
                 | 'TINTERSECTS';
*/

/*
#=============================================================================#
# Definition of ARRAY operators
#=============================================================================#
*/
ArrayOperator : A E Q U A L S | A C O N T A I N S | C O N T A I N E D B Y | A O V E R L A P S;

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





BitStringLiteral : B QUOTE (BIT)* QUOTE;

HexStringLiteral : X QUOTE (HEXIT)* QUOTE;

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

TemporalLiteral : Instant | Interval;

Instant : FullDate | FullDate 'T' UtcTime;

Interval : (InstantInInterval)? SOLIDUS (InstantInInterval)?;

InstantInInterval : '..' | Instant;

FullDate : DateYear '-' DateMonth '-' DateDay;

DateYear : DIGIT DIGIT DIGIT DIGIT;

DateMonth : DIGIT DIGIT;

DateDay : DIGIT DIGIT;

UtcTime : TimeHour ':' TimeMinute ':' TimeSecond (TimeZoneOffset)?;

TimeZoneOffset : 'Z' | Sign TimeHour ':' TimeMinute;

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
