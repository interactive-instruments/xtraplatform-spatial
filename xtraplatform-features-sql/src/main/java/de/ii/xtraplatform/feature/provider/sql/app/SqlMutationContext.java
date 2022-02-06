package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.feature.provider.sql.domain.SchemaMappingSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

@Modifiable
@Value.Style(deepImmutablesDetection = true)
public interface SqlMutationContext extends ModifiableContext<SchemaSql, SchemaMappingSql> {

}
