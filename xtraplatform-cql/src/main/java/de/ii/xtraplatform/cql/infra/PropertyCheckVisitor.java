package de.ii.xtraplatform.cql.infra;

import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.Property;

import java.util.ArrayList;
import java.util.List;

public class PropertyCheckVisitor extends ObjectToCqlVisitor {

    private final List<String> allowedProperties;
    private final List<String> notAllowedProperties = new ArrayList<>();

    public PropertyCheckVisitor(List<String> allowedProperties) {
        this.allowedProperties = allowedProperties;
    }

    @Override
    public String visitTopLevel(CqlPredicate cqlPredicate) {
        notAllowedProperties.clear();
        return cqlPredicate.getExpressions()
                .get(0)
                .acceptTopLevel(this);
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
