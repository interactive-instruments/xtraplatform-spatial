/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.query.api.FeatureConsumer;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author zahnen
 */
public interface GmlConsumer extends FeatureConsumer {

    void onGmlAttribute(String namespace, String localName, List<String> path, String value, List<Integer> multiplicities) throws Exception;

    void onNamespaceRewrite(QName featureType, String namespace) throws Exception;
}
