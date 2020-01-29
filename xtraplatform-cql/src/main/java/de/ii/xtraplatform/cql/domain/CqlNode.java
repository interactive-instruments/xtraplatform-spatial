package de.ii.xtraplatform.cql.domain;

public interface CqlNode {

    default String toCqlText() {
        return "";
    }

    /**
     * This method is used to generate CQL text for expressions on top of the syntax tree.
     * @return CQL text
     */
    default String toCqlTextTopLevel() {
        return toCqlText();
    }

    /**
     * This method is used to generate CQL text for negated expressions, i.e. expressions combined with the operator NOT.
     * @return CQL text
     */
    default String toCqlTextNot() {
        return String.format("NOT (%s)", toCqlText());
    }

}
