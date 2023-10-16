package de.ii.xtraplatform.codelists.app

import de.ii.xtraplatform.codelists.domain.Codelist
import de.ii.xtraplatform.codelists.domain.ImmutableCodelist
import spock.lang.Specification

class CodelistEntitySpec extends Specification {

    def "Test Get Value"() {
        when:

        Map<String, String> entries = new HashMap<>()
        entries.put(key, value)

        Codelist cl = new ImmutableCodelist.Builder().
                label("Cable Type").putAllEntries(entries).sourceType(Codelist.ImportType.GML_DICTIONARY).build()

        then:

        value == cl.getValue(key)

        where:

        key       | value
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

        Codelist cl = new ImmutableCodelist.Builder().
                label("Cable Type").putAllEntries(entries).sourceType(Codelist.ImportType.GML_DICTIONARY).build()

        then:

        "Cable Type" == cl.getLabel().get()
        cl.getEntries().containsKey("19")
        cl.getEntries().containsValue("Cableway")


    }
}
