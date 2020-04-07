/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

}
