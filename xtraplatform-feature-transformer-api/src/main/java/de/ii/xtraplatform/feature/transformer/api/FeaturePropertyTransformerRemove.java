package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public interface FeaturePropertyTransformerRemove extends FeaturePropertySchemaTransformer {

    Logger LOGGER = LoggerFactory.getLogger(FeaturePropertyTransformerRemove.class);

    enum Condition {ALWAYS, OVERVIEW, NEVER;

        @Override
        public String toString() {
            return super.toString();
        }
    }

    String TYPE = "REMOVE";

    @Override
    default String getType() {
        return TYPE;
    }

    boolean isOverview();

    @Override
    default FeatureProperty transform(FeatureProperty input) {
        Condition condition = Condition.NEVER;
        try {
            condition = Condition.valueOf(getParameter().toUpperCase());
        } catch (Throwable e) {
            LOGGER.warn("Skipping {} transformation for property '{}', condition '{}' is not supported. Supported types: {}", getType(), input.getName(), getParameter(), Condition.values());
        }


        if (condition == Condition.ALWAYS || (condition == Condition.OVERVIEW && isOverview())) {
            return null;
        }

        return input;
    }
}
