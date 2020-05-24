package de.ii.xtraplatform.feature.provider.sql.app


import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaSql
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.FeatureStoreRelation
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelation
import de.ii.xtraplatform.features.domain.PropertyBase
import de.ii.xtraplatform.features.domain.SchemaBase

class SqlInsertsFixtures {

    static final SchemaSql MAIN_SCHEMA =
            new ImmutableSchemaSql.Builder()
                    .name("osirisobjekt")
                    .type(SchemaBase.Type.OBJECT)
                    .primaryKey(Optional.of("id"))
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("id")
                            .type(SchemaBase.Type.STRING)
                            .role(SchemaBase.Role.ID)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("kennung")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("bezeichnung")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("veroeffentlichtam")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("verantwortlichestelle")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("bemerkung")
                            .type(SchemaBase.Type.STRING)
                            .build())

                    .build()

    static final FeatureSql MAIN_FEATURE =
            ModifiableFeatureSql.create()
                    .schema(MAIN_SCHEMA)
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.VALUE)
                            .schema(MAIN_SCHEMA.properties.get(0))
                            .name("id")
                            .value("100"))
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.VALUE)
                            .schema(MAIN_SCHEMA.properties.get(1))
                            .name("kennung")
                            .value("1"))
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.VALUE)
                            .schema(MAIN_SCHEMA.properties.get(2))
                            .name("bezeichnung")
                            .value("2"))
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.VALUE)
                            .schema(MAIN_SCHEMA.properties.get(3))
                            .name("veroeffentlichtam")
                            .value("3"))
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.VALUE)
                            .schema(MAIN_SCHEMA.properties.get(4))
                            .name("verantwortlichestelle")
                            .value("4"))
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.VALUE)
                            .schema(MAIN_SCHEMA.properties.get(5))
                            .name("bemerkung")
                            .value("5"))

    static final String MAIN_EXPECTED =
            "INSERT INTO osirisobjekt (kennung,bezeichnung,veroeffentlichtam,verantwortlichestelle,bemerkung) VALUES ('1','2','3','4','5') RETURNING id;"

    static final String MAIN_WITH_ID_EXPECTED =
            "INSERT INTO osirisobjekt (id,kennung,bezeichnung,veroeffentlichtam,verantwortlichestelle,bemerkung) VALUES ('100','1','2','3','4','5') RETURNING id;"


    static final SchemaSql MERGE_SCHEMA =
            new ImmutableSchemaSql.Builder()
                    .name("fundorttiere")
                    .type(SchemaBase.Type.OBJECT)
                    .primaryKey(Optional.of("id"))
                    .addParentPath("osirisobjekt")
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("tierart")
                            .type(SchemaBase.Type.INTEGER)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("bemerkungtierart")
                            .type(SchemaBase.Type.INTEGER)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("vorkommen")
                            .type(SchemaBase.Type.INTEGER)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("nachweis")
                            .type(SchemaBase.Type.INTEGER)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("erfassungsmethode")
                            .type(SchemaBase.Type.INTEGER)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("bemerkungpopulationsstadium")
                            .type(SchemaBase.Type.INTEGER)
                            .build())
                    .relation(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.ONE_2_ONE)
                            .sourceContainer("osirisobjekt")
                            .sourceField("id")
                            .targetContainer("fundorttiere")
                            .targetField("id")
                            .build())
                    .build()

    static final FeatureSql MERGE_FEATURE =
            ModifiableFeatureSql.create()
                    .schema(MAIN_SCHEMA)
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.OBJECT)
                            .schema(MERGE_SCHEMA)
                            .putIds("osirisobjekt.id", "100")
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.VALUE)
                                    .schema(MERGE_SCHEMA.properties.get(0))
                                    .name("tierart")
                                    .value("16"))
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.VALUE)
                                    .schema(MERGE_SCHEMA.properties.get(1))
                                    .name("bemerkungtierart")
                                    .value("17"))
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.VALUE)
                                    .schema(MERGE_SCHEMA.properties.get(2))
                                    .name("vorkommen")
                                    .value("18"))
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.VALUE)
                                    .schema(MERGE_SCHEMA.properties.get(3))
                                    .name("nachweis")
                                    .value("19"))
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.VALUE)
                                    .schema(MERGE_SCHEMA.properties.get(4))
                                    .name("erfassungsmethode")
                                    .value("20"))
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.VALUE)
                                    .schema(MERGE_SCHEMA.properties.get(5))
                                    .name("bemerkungpopulationsstadium")
                                    .value("21")))

    static final String MERGE_EXPECTED =
            "INSERT INTO fundorttiere (id,tierart,bemerkungtierart,vorkommen,nachweis,erfassungsmethode,bemerkungpopulationsstadium) VALUES (100,16,17,18,19,20,21) RETURNING id;"


    static final SchemaSql MAIN_M_2_N_SCHEMA =
            new ImmutableSchemaSql.Builder()
                    .name("foto")
                    .type(SchemaBase.Type.OBJECT_ARRAY)
                    .primaryKey(Optional.of("id"))
                    .addParentPath("osirisobjekt")
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("fotoverweis")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("hauptfoto")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("aufnahmezeitpunkt")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("bemerkung")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .relation(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.M_2_N)
                            .sourceContainer("osirisobjekt")
                            .sourceField("id")
                            .junctionSource("osirisobjekt_id")
                            .junction("osirisobjekt_2_foto")
                            .junctionTarget("foto_id")
                            .targetContainer("foto")
                            .targetField("id")
                            .build())
                    .build()

    static final FeatureSql MAIN_M_2_N_FEATURE =
            ModifiableFeatureSql.create()
                    .schema(MAIN_SCHEMA)
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.ARRAY)
                            .schema(MAIN_M_2_N_SCHEMA)
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.OBJECT)
                                    .schema(MAIN_M_2_N_SCHEMA)
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("fotoverweis")
                                            .value("6"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("hauptfoto")
                                            .value("7"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("aufnahmezeitpunkt")
                                            .value("8"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("bemerkung")
                                            .value("9"))
                                    .putIds("foto.id", "200"))
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.OBJECT)
                                    .schema(MAIN_M_2_N_SCHEMA)
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("fotoverweis")
                                            .value("60"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("hauptfoto")
                                            .value("70"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("aufnahmezeitpunkt")
                                            .value("80"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("bemerkung")
                                            .value("90"))
                                    .putIds("foto.id", "201")))

    static final List<String> MAIN_M_2_N_EXPECTED = [
            "INSERT INTO foto (fotoverweis,hauptfoto,aufnahmezeitpunkt,bemerkung) VALUES (6,7,8,9) RETURNING id;",
            "INSERT INTO foto (fotoverweis,hauptfoto,aufnahmezeitpunkt,bemerkung) VALUES (60,70,80,90) RETURNING id;"
    ]


    static final SchemaSql MERGE_MERGE_SCHEMA =
            new ImmutableSchemaSql.Builder()
                    .name("artbeobachtung")
                    .type(SchemaBase.Type.OBJECT)
                    .primaryKey(Optional.of("id"))
                    .addParentPath("osirisobjekt", "[id=id]fundorttiere")
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("anzahl")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("begehungsmethode")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("bemerkunginformationsquelle")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("beobachtetam")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("haeufigkeit")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("informationsquelle")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("letzteskartierdatum")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("unschaerfe")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("bemerkungort")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .relation(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.ONE_2_ONE)
                            .sourceContainer("fundorttiere")
                            .sourceField("id")
                            .targetContainer("artbeobachtung")
                            .targetField("id")
                            .build())
                    .build()

    static final FeatureSql MERGE_MERGE_FEATURE =
            ModifiableFeatureSql.create()
                    .schema(MAIN_SCHEMA)
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.OBJECT)
                            .schema(MERGE_SCHEMA)
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.OBJECT)
                                    .schema(MERGE_MERGE_SCHEMA)
                                    .putIds("fundorttiere.id", "100")
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("anzahl")
                                            .value("22"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("begehungsmethode")
                                            .value("23"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("bemerkunginformationsquelle")
                                            .value("24"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("beobachtetam")
                                            .value("25"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("haeufigkeit")
                                            .value("26"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("informationsquelle")
                                            .value("27"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("letzteskartierdatum")
                                            .value("28"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("unschaerfe")
                                            .value("29"))
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.VALUE)
                                            .name("bemerkungort")
                                            .value("30"))))

    static final String MERGE_MERGE_EXPECTED =
            "INSERT INTO artbeobachtung (id,anzahl,begehungsmethode,bemerkunginformationsquelle,beobachtetam,haeufigkeit,informationsquelle,letzteskartierdatum,unschaerfe,bemerkungort) VALUES (100,22,23,24,25,26,27,28,29,30) RETURNING id;"


    static final SchemaSql MERGE_MERGE_ONE_2_ONE_SCHEMA =
            new ImmutableSchemaSql.Builder()
                    .name("geom")
                    .type(SchemaBase.Type.OBJECT)
                    .primaryKey(Optional.of("id"))
                    .addParentPath("osirisobjekt", "[id=id]fundorttiere", "[id=id]artbeobachtung")
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("ST_AsText(ST_ForcePolygonCCW(geom))")
                            .type(SchemaBase.Type.GEOMETRY)
                            .build())
                    .relation(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.ONE_2_ONE)
                            .sourceContainer("artbeobachtung")
                            .sourceField("geom")
                            .sourceSortKey("id")
                            .targetContainer("geom")
                            .targetField("id")
                            .build())
                    .build()

    static final FeatureSql MERGE_MERGE_ONE_2_ONE_FEATURE =
            ModifiableFeatureSql.create()
                    .schema(MAIN_SCHEMA)
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.OBJECT)
                            .schema(MERGE_SCHEMA)
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.OBJECT)
                                    .schema(MERGE_MERGE_SCHEMA)
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.OBJECT)
                                            .schema(MERGE_MERGE_ONE_2_ONE_SCHEMA)
                                            .putIds("artbeobachtung.id", "100")
                                            .putIds("geom.id", "800")
                                            .addNestedProperties(ModifiablePropertySql.create()
                                                    .type(PropertyBase.Type.VALUE)
                                                    .name("ST_AsText(ST_ForcePolygonCCW(geom))")
                                                    .value("31")))))

    static final String MERGE_MERGE_ONE_2_ONE_EXPECTED =
            "INSERT INTO geom (geom) VALUES (ST_ForcePolygonCW(ST_GeomFromText(31,25832))) RETURNING id;"

    static final String MERGE_MERGE_ONE_2_ONE_FOREIGN_KEY_EXPECTED =
            "UPDATE artbeobachtung SET geom=800 WHERE id=100 RETURNING null;"


    static final SchemaSql MERGE_MERGE_M_2_N_SCHEMA =
            new ImmutableSchemaSql.Builder()
                    .name("erfasser")
                    .type(SchemaBase.Type.OBJECT_ARRAY)
                    .primaryKey(Optional.of("id"))
                    .addParentPath("osirisobjekt", "[id=id]fundorttiere", "[id=id]artbeobachtung")
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("name")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .addProperties(new ImmutableSchemaSql.Builder()
                            .name("bemerkung")
                            .type(SchemaBase.Type.STRING)
                            .build())
                    .relation(ImmutableFeatureStoreRelation.builder()
                            .cardinality(FeatureStoreRelation.CARDINALITY.M_2_N)
                            .sourceContainer("artbeobachtung")
                            .sourceField("id")
                            .junctionSource("artbeobachtung_id")
                            .junction("artbeobachtung_2_erfasser")
                            .junctionTarget("erfasser_id")
                            .targetContainer("erfasser")
                            .targetField("id")
                            .build())
                    .build()

    static final FeatureSql MERGE_MERGE_M_2_N_FEATURE =
            ModifiableFeatureSql.create()
                    .schema(MAIN_SCHEMA)
                    .addProperties(ModifiablePropertySql.create()
                            .type(PropertyBase.Type.OBJECT)
                            .schema(MERGE_SCHEMA)
                            .addNestedProperties(ModifiablePropertySql.create()
                                    .type(PropertyBase.Type.OBJECT)
                                    .schema(MERGE_MERGE_SCHEMA)
                                    .addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.ARRAY)
                                            .schema(MERGE_MERGE_M_2_N_SCHEMA)
                                            .addNestedProperties(ModifiablePropertySql.create()
                                                    .type(PropertyBase.Type.OBJECT)
                                                    .schema(MERGE_MERGE_M_2_N_SCHEMA)
                                                    .putIds("artbeobachtung.id", "100")
                                                    .putIds("erfasser.id", "600")
                                                    .addNestedProperties(ModifiablePropertySql.create()
                                                            .type(PropertyBase.Type.VALUE)
                                                            .name("name")
                                                            .value("32"))
                                                    .addNestedProperties(ModifiablePropertySql.create()
                                                            .type(PropertyBase.Type.VALUE)
                                                            .name("bemerkung")
                                                            .value("33"))).addNestedProperties(ModifiablePropertySql.create()
                                            .type(PropertyBase.Type.OBJECT)
                                            .schema(MERGE_MERGE_M_2_N_SCHEMA)
                                            .putIds("artbeobachtung.id", "100")
                                            .putIds("erfasser.id", "700")
                                            .addNestedProperties(ModifiablePropertySql.create()
                                                    .type(PropertyBase.Type.VALUE)
                                                    .name("name")
                                                    .value("34"))
                                            .addNestedProperties(ModifiablePropertySql.create()
                                                    .type(PropertyBase.Type.VALUE)
                                                    .name("bemerkung")
                                                    .value("35"))))))

    static final List<String> MERGE_MERGE_M_2_N_EXPECTED = [
            "INSERT INTO erfasser (name,bemerkung) VALUES (32,33) RETURNING id;",
            "INSERT INTO erfasser (name,bemerkung) VALUES (34,35) RETURNING id;"
    ]

    static final List<String> MERGE_MERGE_M_2_N_JUNCTION_EXPECTED = [
            "INSERT INTO artbeobachtung_2_erfasser (artbeobachtung_id,erfasser_id) VALUES (100,600) RETURNING null;",
            "INSERT INTO artbeobachtung_2_erfasser (artbeobachtung_id,erfasser_id) VALUES (100,700) RETURNING null;"
    ]


    static final SchemaSql MERGE_MERGE_WITH_CHILDREN =
            new ImmutableSchemaSql.Builder()
                    .from(MERGE_MERGE_SCHEMA)
                    .addAllPropertiesBuilders(new ImmutableSchemaSql.Builder()
                            .from(MERGE_MERGE_ONE_2_ONE_SCHEMA))
                    .addAllPropertiesBuilders(new ImmutableSchemaSql.Builder()
                            .from(MERGE_MERGE_M_2_N_SCHEMA))
                    .build()

    static final SchemaSql MERGE_WITH_CHILDREN =
            new ImmutableSchemaSql.Builder()
                    .from(MERGE_SCHEMA)
                    .addProperties(MERGE_MERGE_WITH_CHILDREN)
                    .build()

    static final SchemaSql FULL =
            new ImmutableSchemaSql.Builder()
                    .from(MAIN_SCHEMA)
                    .addProperties(MERGE_WITH_CHILDREN)
                    .addAllPropertiesBuilders(new ImmutableSchemaSql.Builder()
                            .from(MAIN_M_2_N_SCHEMA))
                    .build()

}
