package de.ii.xtraplatform.cql.domain;

public enum ComparisonOperator implements CqlNode {

    EQ("="),
    NEQ("<>"),
    GT(">"),
    GTEQ(">="),
    LT("<"),
    LTEQ("<="),
    LIKE("LIKE");

    private final String cqlText;

    ComparisonOperator(String cqlText) {
        this.cqlText = cqlText;
    }

    public static ComparisonOperator valueOfCqlText(String cqlText) {
        for (ComparisonOperator e : values()) {
            if (e.cqlText.equals(cqlText)) {
                return e;
            }
        }
        return null;
    }


    @Override
    public String toCqlText() {
        return cqlText;
    }
}
