/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import java.util.Optional;

/**
 * @author zahnen
 */
public abstract class FeatureTypeConfiguration {

    public abstract String getId();

    public abstract String getLabel();

    public abstract Optional<String> getDescription();

}
