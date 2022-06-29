/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.codelists.app;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class CodelistEntity extends AbstractPersistentEntity<CodelistData> implements Codelist {

  private static final Logger LOGGER = LoggerFactory.getLogger(CodelistEntity.class);

  @AssistedInject
  public CodelistEntity(@Assisted CodelistData data) {
    super(data);
  }

  @Override
  protected void onStarted() {
    LOGGER.info("Codelist with id '{}' loaded successfully.", getId());
  }

  @Override
  protected void onReloaded() {
    LOGGER.info("Codelist with id '{}' reloaded successfully.", getId());
  }

  @Override
  public String getValue(String key) {

    return Optional.ofNullable(getData().getEntries().get(key))
        .orElse(getData().getFallback().orElse(key));
  }

  @Override
  public CodelistData getData() {
    return super.getData();
  }
}
