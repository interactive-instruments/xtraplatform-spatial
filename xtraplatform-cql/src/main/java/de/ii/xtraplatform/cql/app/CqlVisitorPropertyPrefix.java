package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.CqlNode;
import de.ii.xtraplatform.cql.domain.Property;

import java.util.List;

public class CqlVisitorPropertyPrefix extends CqlVisitorCopy {

    private final String prefix;

    public CqlVisitorPropertyPrefix(String prefix) {
        this.prefix = prefix + ".";
    }

    @Override
    public CqlNode visit(Property property, List<CqlNode> children) {
        if (!property.getName().startsWith(prefix)) {
            return Property.of(prefix + property.getName(), property.getNestedFilters());
        }
        return property;
    }
}
