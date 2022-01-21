/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider;


import org.osgi.framework.ServiceReference;

import java.util.Optional;

public interface Registry<T> {

    String ON_ARRIVAL_METHOD = "onArrival";
    String ON_DEPARTURE_METHOD = "onDeparture";
    String FACTORY_FILTER_PREFIX = "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=";
    String FACTORY_FILTER_SUFFIX = "))";

    interface State<U> {

        Optional<U> get(String... identifiers);

        void onArrival(ServiceReference<U> ref);

        void onDeparture(ServiceReference<U> ref);
    }

    Registry.State<T> getRegistryState();

    default void onArrival(ServiceReference<T> ref) {
        getRegistryState().onArrival(ref);
    }

    default void onDeparture(ServiceReference<T> ref) {
        getRegistryState().onDeparture(ref);
    }

}
