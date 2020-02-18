package de.ii.xtraplatform.cql.infra;

import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.Property;

import java.util.ArrayList;
import java.util.List;

public class CqlPropertyChecker extends CqlToText {

    private final List<String> allowedProperties;
    private final List<String> notAllowedProperties = new ArrayList<>();

    public CqlPropertyChecker(List<String> allowedProperties) {
        this.allowedProperties = allowedProperties;
    }

    @Override
    public String visit(CqlFilter cqlFilter) {
        notAllowedProperties.clear();
        return cqlFilter.getExpressions()
                        .get(0)
                        .accept(this);
    }

    @Override
    public String visit(Property property) {
        String propertyName = property.getName();
        if (!allowedProperties.contains(propertyName)) {
            notAllowedProperties.add(propertyName);
        }
        return propertyName;
    }

    public List<String> getNotAllowedProperties() {
        return notAllowedProperties;
    }

}
