/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain

import de.ii.xtraplatform.features.domain.transform.ImplicitMappingResolver
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author zahnen
 */
class SchemaMappingSpec extends Specification {

    @Shared
    MappingOperationResolver mappingOperationResolver
    @Shared
    ImplicitMappingResolver implicitMappingResolver

    def setupSpec() {
        mappingOperationResolver = new MappingOperationResolver()
        implicitMappingResolver = new ImplicitMappingResolver()
    }

    def 'schema mapping: #casename'() {

        when:

        SchemaMapping actual = mapping(schema, mappingOperationResolver, implicitMappingResolver)
        Set<List<String>> expectedSource = FeaturePathFixtures.fromYaml(paths + "-source")
        Set<List<String>> expectedTarget = FeaturePathFixtures.fromYaml(paths + "-target")

        then:

        actual.getSchemasBySourcePath().keySet() == expectedSource
        actual.getSchemasByTargetPath().keySet() == expectedTarget

        where:

        casename                                   | schema                        | paths
        "embedded object with concat and backlink" | "pfs_plan-hatObjekt-embedded" | "pfs_plan-hatObjekt-embedded"

    }

    static SchemaMapping mapping(String featureSchemaName, MappingOperationResolver mappingOperationResolver, ImplicitMappingResolver implicitMappingResolver) {
        def schema = FeatureSchemaFixtures.fromYaml(featureSchemaName)
        def schema2 = schema.accept(implicitMappingResolver, List.of())
        def schema3 = schema2.accept(mappingOperationResolver, List.of())
        return SchemaMapping.of(schema3)
    }
}
