/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

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
//@Value.Modifiable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableMappingStatus.Builder.class)
public abstract class MappingStatus {

    public abstract boolean getEnabled();

    public abstract boolean getSupported();

    @Value.Default
    public boolean getRefined() {
        return false;
    }

    @JsonIgnore
    @Value.Derived
    public boolean getLoading() {
        return getEnabled() && !getSupported() && Objects.isNull(getErrorMessage());
    }

    @Nullable
    public abstract String getErrorMessage();

    @Nullable
    public abstract List<String> getErrorMessageDetails();
}
