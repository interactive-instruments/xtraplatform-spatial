/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureEventHandlerGeneric.GenericContext;

public abstract class FeatureObjectEncoder<
        T extends PropertyBase<T, FeatureSchema>, U extends FeatureBase<T, FeatureSchema>>
    extends FeatureObjectEncoderBase<FeatureSchema, SchemaMapping, T, U>
    implements FeatureTokenEncoder<ModifiableContext<FeatureSchema, SchemaMapping>> {

  @Override
  public Class<? extends ModifiableContext<FeatureSchema, SchemaMapping>> getContextInterface() {
    return GenericContext.class;
  }

  @Override
  public ModifiableContext<FeatureSchema, SchemaMapping> createContext() {
    return ModifiableGenericContext.create();
  }
}
