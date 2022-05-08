/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.util.Optional;

/**
 * @author zahnen
 */
public interface FeatureTypeConfiguration {

    /**
     * @langEn *REQUIRED* API identifier. Allowed characters are (A-Z, a-z), numbers (0-9), underscore and hyphen.
     * @langDe *REQUIRED* Eindeutiger Identifikator der API. Typischerweise identisch mit dem
     * Identifikator des Feature-Providers. Erlaubt sind Buchstaben (A-Z, a-z), Ziffern (0-9),
     * der Unterstrich ("_") und der Bindestrich ("-").
     * @default
     */
    String getId();

    /**
     * @langEn Human readable label.
     * @langDe Eine Bezeichnung der API, z.B. für die Präsentation zu Nutzern.
     * @default The `id`
     */
    String getLabel();

    /**
     * @langEn Human readable description.
     * @langDe Eine Beschreibung des Schemaobjekts, z.B. für die Präsentation zu Nutzern.
     * @default `null`
     */
    Optional<String> getDescription();

}
