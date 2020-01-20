package de.ii.xtraplatform.feature.transformer.api;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;

@Value.Immutable
public interface FeaturePropertyTransformerDateFormat extends FeaturePropertyValueTransformer {

    Logger LOGGER = LoggerFactory.getLogger(FeaturePropertyTransformerDateFormat.class);

    String TYPE = "DATE_FORMAT";

    @Override
    default String getType() {
        return TYPE;
    }

    @Override
    default List<FeatureProperty.Type> getSupportedPropertyTypes() {
        return ImmutableList.of(FeatureProperty.Type.DATETIME);
    }

    @Override
    default String transform(String input) {
        try {
            DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getParameter());
            TemporalAccessor ta = parser.parseBest(input, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);

            return formatter.format(ta);
        } catch (Exception e) {
            LOGGER.warn("{} transformation for property '{}' with value '{}' failed: {}", getType(), getPropertyName().orElse(""), input, e.getMessage());
        }

        return input;
    }
}
