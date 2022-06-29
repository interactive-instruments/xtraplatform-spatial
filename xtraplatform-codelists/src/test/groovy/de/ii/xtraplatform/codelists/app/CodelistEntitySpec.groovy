package de.ii.xtraplatform.codelists.app

import com.google.common.collect.ImmutableMap
import dagger.internal.MapBuilder
import de.ii.xtraplatform.codelists.domain.Codelist
import de.ii.xtraplatform.codelists.domain.CodelistData
import de.ii.xtraplatform.codelists.domain.ImmutableCodelistData
import spock.lang.Shared
import spock.lang.Specification


class CodelistEntitySpec extends Specification{

    @Shared
    CodelistFactory codelistFactory

    def setupSpec() {
        codelistFactory = new CodelistFactory(new CodelistFactory.CodelistFactoryAssisted() {
            @Override
            CodelistEntity create(CodelistData data) {
                return new CodelistEntity(data)
            }

        })
    }

    def "Test onStarted"(){
        when:
        CodelistEntity codeListEntity =  codelistFactory.createInstance( new ImmutableCodelistData.Builder().id("cab").
                label("Cable Type").sourceType(CodelistData.IMPORT_TYPE.GML_DICTIONARY).build()).get()

        then:

        codeListEntity.onStarted()
    }

    def "Test onReloaded"(){
        when:

        CodelistEntity codeListEntity =  codelistFactory.createInstance( new ImmutableCodelistData.Builder().id("cab").
                label("Cable Type").sourceType(CodelistData.IMPORT_TYPE.GML_DICTIONARY).build()).get()

        then:
        codeListEntity.onReloaded()
    }

    def "Test Get Value"(){
        when:

        Map<String, String> entries = new HashMap<>()
        entries.put(key, value)

        CodelistEntity codeListEntity =  codelistFactory.createInstance( new ImmutableCodelistData.Builder().id("cab").
                label("Cable Type").putAllEntries(entries).sourceType(CodelistData.IMPORT_TYPE.GML_DICTIONARY).build()).get()

        then:

        value == codeListEntity.getValue(key)

        where:

        key        | value
        "-999999" | "No Information"
        "10"      | "No Tethering"
        "11"      | "Load Bearing"
        "12"      | "Guide"
        "13"      | "Barrier"
        "15"      | "Towing"
        "19"      | "Cableway"
    }

    def "Test Test getData"() {

        when:

        Map<String, String> entries = new HashMap<>()
        entries.put("-999999", "No Information")
        entries.put("10", "No Tethering")
        entries.put("11", "Load Bearing")
        entries.put("12", "Guide")
        entries.put("13", "Barrier")
        entries.put("15", "Towing")
        entries.put("19", "Cableway")

        CodelistEntity codeListEntity = codelistFactory.createInstance(new ImmutableCodelistData.Builder().id("cab").
                label("Cable Type").putAllEntries(entries).sourceType(CodelistData.IMPORT_TYPE.GML_DICTIONARY).build()).get()

        then:

        CodelistData cl = codeListEntity.getData()

        "cab" == cl.getId()
        "Cable Type" == cl.getLabel()
        cl.getEntries().containsKey("19")
        cl.getEntries().containsValue("Cableway")


    }

    def "Test getType"(){

        when:

        CodelistEntity codeListEntity =  codelistFactory.createInstance( new ImmutableCodelistData.Builder().id("cab").
                label("Cable Type").sourceType(CodelistData.IMPORT_TYPE.GML_DICTIONARY).build()).get()

        then:

        "codelists" == codeListEntity.getType()


    }
}
