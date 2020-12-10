package de.ii.xtraplatform.stringtemplates.domain

import spock.lang.Ignore
import spock.lang.Specification

class StringTemplateFiltersSpec extends Specification {

    @Ignore
    def 'All arguments are null'() {
        when:
            String result = StringTemplateFilters.applyTemplate(null, null, null, null)
        then:
            result.isEmpty()
    }

    @Ignore
    def 'Template is null, value is present'() {
        given:
            String template = null
            String value = "foobar"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result.isEmpty()
    }

    @Ignore
    def 'Value is null, template is present'() {
        given:
            String template = "{{value | replace:'ABC':' '}}"
            String value = null
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result.isEmpty()
    }

    def 'Template is empty, value is present'() {
        given:
            String template = ""
            String value = "foobar"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == value
    }

    def 'Value is empty, template is present'() {
        given:
            String template = "{{value | replace:'ABC':' '}}"
            String value = ""
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result.isEmpty()
    }

    @Ignore
    def 'Malformed template'() {
        given:
            String template = "{replace:'ABC':' '}}"
            String value = "foobar"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == value
    }

    def 'replace test'() {
        given:
            String template = "{{value | replace:'ABC':' '}}"
            String value = "foobarABCtest"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == "foobar test"
    }

    def 'append test'() {
        given:
            String template = "{{value | append:'bar'}}"
            String value = "foo"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == "foobar"
    }

    def 'prepend test'() {
        given:
            String template = "{{value | prepend:'bar'}}"
            String value = "foo"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == "barfoo"
    }

    def 'toLower test'() {
        given:
            String template = "{{value | toLower}}"
            String value = "fooBAR"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == "foobar"
    }

    def 'toUpper test'() {
        given:
            String template = "{{value | toUpper}}"
            String value = "fooBAR"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == "FOOBAR"
    }

    def 'urlEncode test'() {
        given:
            String template = "{{value | urlEncode}}"
            String value = "query=foo&bar test"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == "query%3Dfoo%26bar+test"
    }

    def 'urlencode test'() {
        given:
        String template = "{{value | urlencode}}"
        String value = "query=foo&bar test"
        when:
        String result = StringTemplateFilters.applyTemplate(template, value)
        then:
        result == "query%3Dfoo%26bar+test"
    }

    def 'markdown test'() {
        given:
            String template = "{{value | markdown}}"
            String value = "foo*bar*"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == "foo<em>bar</em>"
    }

    def 'assignTo test'() {
        given:
            String template = "{{value | assignTo:'foo'}}"
            String value = "{{foo}} bar"
        when:
            String result = StringTemplateFilters.applyTemplate(template, value)
        then:
            result == "{{foo}} bar bar"
    }

    def 'Multiple filters'() {
        given:
        String template = "{{value | prepend:'query=' | append:' test' | urlencode}}"
        String value = "foo&bar"
        when:
        String result = StringTemplateFilters.applyTemplate(template, value)
        then:
        result == "query%3Dfoo%26bar+test"
    }

}
