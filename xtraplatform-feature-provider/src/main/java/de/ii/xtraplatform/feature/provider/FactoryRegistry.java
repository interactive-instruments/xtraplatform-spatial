/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider;

import org.apache.felix.ipojo.Factory;

import java.util.Map;

public interface FactoryRegistry<T> extends Registry.State<Factory> {

    boolean ensureTypeExists();

    T createInstance(Map<String, Object> configuration, String... factoryProperties);

    boolean hasInstance(String instanceId);

    T getInstance(String instanceId);

    void disposeInstance(T instance);
}
