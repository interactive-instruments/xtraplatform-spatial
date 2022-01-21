/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain

import de.ii.xtraplatform.crs.domain.EpsgCrs

class FeatureProviderDataFixtures {

    static final ConnectionInfo connectionInfo = new ConnectionInfo() {
        @Override
        Optional<String> getConnectionUri() {
            return Optional.empty()
        }

        @Override
        String getConnectorType() {
            return "NONE"
        }
    }

    static final FeatureProviderDataV1 ONEO_V1 = new ImmutableFeatureProviderDataV1.Builder()
            .id("oneo")
            .createdAt(1586271491161)
            .lastModified(1586271491161)
            .providerType("FEATURE")
            .featureProviderType("SQL")
    .connectionInfo(connectionInfo)
            .nativeCrs(EpsgCrs.of(25832))
            .putTypes2("fundorttiere", new ImmutableFeatureType.Builder()
                    .putProperties2("id", new ImmutableFeatureProperty.Builder()
                            .path("/fundorttiere/[id=id]osirisobjekt/id")
                            .type(FeatureProperty.Type.STRING)
                            .role(FeatureProperty.Role.ID)
                    )
                    .putProperties2("foto[foto].fotoverweis", new ImmutableFeatureProperty.Builder()
                            .path("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto/fotoverweis")
                            .type(FeatureProperty.Type.STRING)
                    )
                    .putProperties2("raumreferenz[raumreferenz].ortsangabe[ortsangaben].kreisSchluessel", new ImmutableFeatureProperty.Builder()
                            .path("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/kreisschluessel")
                            .type(FeatureProperty.Type.STRING)
                    )
                    .putProperties2("raumreferenz[raumreferenz].ortsangabe[ortsangaben].flurstuecksKennzeichen[ortsangaben_flurstueckskennzeichen]", new ImmutableFeatureProperty.Builder()
                            .path("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben/[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen")
                            .type(FeatureProperty.Type.STRING)
                    )
                    .putProperties2("raumreferenz[raumreferenz].datumAbgleich", new ImmutableFeatureProperty.Builder()
                            .path("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/datumabgleich")
                            .type(FeatureProperty.Type.STRING)
                    )
                    .putProperties2("raumreferenz[raumreferenz].fachreferenz[raumreferenz_2_fachreferenz]", new ImmutableFeatureProperty.Builder()
                            .path("/fundorttiere/[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz/[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id")
                            .type(FeatureProperty.Type.STRING)
                    )
            )
            .build()



    static final FeatureProviderDataV2 ONEO_V2 = new ImmutableFeatureProviderCommonData.Builder()
            .id("oneo")
            .createdAt(1586271491161)
            .lastModified(1586271491161)
            .providerType("FEATURE")
            .featureProviderType("SQL")
            .connectionInfo(connectionInfo)
            .nativeCrs(EpsgCrs.of(25832))
            .putTypes2("fundorttiere", new ImmutableFeatureSchema.Builder()
            .sourcePath("/fundorttiere")
                    .putProperties2("id", new ImmutableFeatureSchema.Builder()
                            .sourcePath("[id=id]osirisobjekt/id")
                            .type(SchemaBase.Type.STRING)
                            .role(SchemaBase.Role.ID)
                    )
                    .putProperties2("foto", new ImmutableFeatureSchema.Builder()
                            .sourcePath("[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_foto/[foto_id=id]foto")
                            .type(SchemaBase.Type.OBJECT_ARRAY)
                            .putProperties2("fotoverweis", new ImmutableFeatureSchema.Builder()
                                    .sourcePath("fotoverweis")
                                    .type(SchemaBase.Type.STRING)
                            )
                    )
                    .putProperties2("raumreferenz", new ImmutableFeatureSchema.Builder()
                            .sourcePath("[id=id]osirisobjekt/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz")
                            .type(SchemaBase.Type.OBJECT_ARRAY)
                            .putProperties2("ortsangabe", new ImmutableFeatureSchema.Builder()
                                    .sourcePath("[id=raumreferenz_id]raumreferenz_2_ortsangabe/[ortsangabe_id=id]ortsangaben")
                                    .type(SchemaBase.Type.OBJECT_ARRAY)
                                    .putProperties2("kreisSchluessel", new ImmutableFeatureSchema.Builder()
                                            .sourcePath("kreisschluessel")
                                            .type(SchemaBase.Type.STRING)
                                    )
                                    .putProperties2("flurstuecksKennzeichen", new ImmutableFeatureSchema.Builder()
                                            .sourcePath("[id=ortsangaben_id]ortsangaben_flurstueckskennzeichen/flurstueckskennzeichen")
                                            .type(SchemaBase.Type.VALUE_ARRAY)
                                            .valueType(SchemaBase.Type.STRING)
                                    )
                            )
                            .putProperties2("datumAbgleich", new ImmutableFeatureSchema.Builder()
                                    .sourcePath("datumabgleich")
                                    .type(SchemaBase.Type.STRING)
                            )
                            .putProperties2("fachreferenz", new ImmutableFeatureSchema.Builder()
                                    .sourcePath("[id=raumreferenz_id]raumreferenz_2_fachreferenz/objektart:fachreferenz_id")
                                    .type(SchemaBase.Type.VALUE_ARRAY)
                                    .valueType(SchemaBase.Type.STRING)
                            )
                    )
            )
            .build()
}
