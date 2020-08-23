/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.codelists.domain;

import de.ii.xtraplatform.store.domain.entities.PersistentEntity;

/**
 * @author zahnen
 */
public interface Codelist extends PersistentEntity {
    String ENTITY_TYPE = "codelists";

    @Override
    default String getType() {
        return ENTITY_TYPE;
    }

    @Override
    CodelistData getData();

    String getValue(String key);
}
