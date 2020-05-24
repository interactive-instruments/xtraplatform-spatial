package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.PropertyBase;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface PropertySql extends PropertyBase<PropertySql, SchemaSql>, ObjectSql {

}
