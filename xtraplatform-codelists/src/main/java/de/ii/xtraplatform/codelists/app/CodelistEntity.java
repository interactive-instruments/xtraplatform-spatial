/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.codelists.app;

import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import de.ii.xtraplatform.store.domain.entities.EntityComponent;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author zahnen
 */

@EntityComponent
@Entity(type = Codelist.ENTITY_TYPE, dataClass = CodelistData.class)
public class CodelistEntity extends AbstractPersistentEntity<CodelistData> implements Codelist {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodelistEntity.class);

    @Override
    protected void onStart() {
        LOGGER.info("Codelist with id '{}' loaded successfully.", getId());
    }

    @Override
    public String getValue(String key) {

        return Optional.ofNullable(getData().getEntries()
                                            .get(key))
                       .orElse(getData().getFallback()
                                        .orElse(key));
    }

    @Override
    public CodelistData getData() {
        return super.getData();
    }

    @Override
    protected boolean shouldRegister() {
        return true;
    }
}
