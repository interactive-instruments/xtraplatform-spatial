package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;


@Value.Immutable
@JsonDeserialize(builder = ImmutableIn.Builder.class)
public interface In extends CqlNode, ScalarOperation {

    abstract class Builder extends ScalarOperation.Builder<In> {
    }

    @Value.Check
    @Override
    default void check() {
        int count = getValues().size();
        Preconditions.checkState(count > 0, "an IN operation must have at least one value, found %s", count);
        Preconditions.checkState(getProperty().isPresent(), "property is missing");
    }

    @Value.Default
    default Boolean getNocase() {
        return Boolean.TRUE;
    }

    List<ScalarLiteral> getValues();

    @Override
    default String toCqlText() {
        return String.format("%s IN (%s)", getProperty().get().toCqlText(),
                getValues()
                        .stream()
                        .map(Literal::toCqlText)
                        .collect(Collectors.joining(", ")));
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
