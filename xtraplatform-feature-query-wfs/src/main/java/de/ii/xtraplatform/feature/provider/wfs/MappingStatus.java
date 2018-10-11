package de.ii.xtraplatform.feature.provider.wfs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableMappingStatus.class)
public abstract class MappingStatus {

    @Value.Default
    public boolean getEnabled() {
        return true;
    }

    @JsonIgnore
    @Value.Derived
    public boolean getLoading() {
        return getEnabled() && !getSupported() && Objects.isNull(getErrorMessage());
    }

    @Value.Default
    public boolean getSupported() {
        return false;
    }

    @Nullable
    public abstract String getErrorMessage();

    @Nullable
    public abstract List<String> getErrorMessageDetails();
}
