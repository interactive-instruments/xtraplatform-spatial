/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableMetadata.Builder.class)
public interface Metadata {

    Optional<String> getVersion();

    Optional<String> getLabel();

    Optional<String> getDescription();

    List<String> getKeywords();

    Optional<String> getFees();

    Optional<String> getAccessConstraints();

    Optional<String> getContactName();

    Optional<String> getContactUrl();

    Optional<String> getContactEmail();

    List<QName> getFeatureTypes();

    Map<QName, String> getFeatureTypesCrs();

    Map<String, BoundingBox> getFeatureTypesBoundingBox();

    Map<String, String> getNamespaces();

}
