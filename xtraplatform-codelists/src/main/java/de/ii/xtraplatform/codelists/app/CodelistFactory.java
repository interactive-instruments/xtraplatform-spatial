/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.codelists.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelistData;
import de.ii.xtraplatform.store.domain.entities.AbstractEntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CodelistFactory extends AbstractEntityFactory<CodelistData, CodelistEntity> implements EntityFactory {

  @Inject
  public CodelistFactory(CodelistFactoryAssisted codelistFactoryAssisted) {
    super(codelistFactoryAssisted);
  }

  @Override
  public String type() {
    return Codelist.ENTITY_TYPE;
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return CodelistEntity.class;
  }

  @Override
  public EntityDataBuilder<CodelistData> dataBuilder() {
    return new ImmutableCodelistData.Builder();
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return CodelistData.class;
  }

  @AssistedFactory
  public interface CodelistFactoryAssisted extends FactoryAssisted<CodelistData, CodelistEntity> {
    @Override
    CodelistEntity create(CodelistData data);
  }
}
