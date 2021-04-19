/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.features.domain;

/**
 *
 * @author zahnen
 */
public interface FeatureProviderSchemaConsumer {
    void analyzeFeatureType(String nsUri, String localName);
    void analyzeAttribute(String nsUri, String localName, String type, boolean required);
    void analyzeProperty(String nsUri, String localName, String type, long minOccurs, long maxOccurs,
                         int depth, boolean isParentMultiple, boolean isComplex, boolean isObject);
    boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName);
    default void analyzeNamespace(String uri) {}
    void analyzeFailure(Throwable e);
    void analyzeSuccess();
}
