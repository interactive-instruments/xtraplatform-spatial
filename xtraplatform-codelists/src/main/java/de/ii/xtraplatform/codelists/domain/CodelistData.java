/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.codelists.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

/**
 * # Codelists
 * @langEn Codelists allow to map property values to a different value. This is
 * useful especially for HTML representations.
 * @langDe Codelisten können zum Übersetzen von Eigenschaftswerten in einen anderen
 * Wert genutzt werden, meist für die HTML-Ausgabe.
 * @see de.ii.xtraplatform.store.domain.entities.EntityData
 *
 * ## Konfiguration
 * @langEn The following table describes the structure of the code list files.
 *
 * For the target values in `entries` and for `fallback` also [`stringFormat` transformations]
 * (../providers/transformations.md) can be used. If the transformed value is intended for HTML
 * output, then Markdown markup can also be used, this will be formatted in the HTML output.
 * @langDe Die nachfolgende Tabelle beschreibt die Struktur der Codelisten-Dateien.
 *
 * Bei den Zielwerten in `entries` und bei `fallback` können auch [`stringFormat`-Transformationen]
 * (../providers/transformations.md) genutzt werden. Ist der transformierte Wert für die HTML-Ausgabe
 * gedacht, dann kann auch Markdown-Markup verwendet werden, dieser wird bei der HTML-Ausgabe
 * aufbereitet.
 *
 * @langEn Based on the INSPIRE codelist [EnvironmentalDomain](https://inspire.ec.europa.eu/codeList/EnvironmentalDomain),
 * maps values like `soil` to the German label of the entry in the INSPIRE codelist registry.
 * @langDe Basierend auf der INSPIRE-Codelist
 * [EnvironmentalDomain](https://inspire.ec.europa.eu/codeList/EnvironmentalDomain)
 * werden Werte wie "soil" auf das deutschsprachige Label in der INSPIRE-Codelist-Registry abgebildet:
 * <code>
 * ```yaml
 * ---
 * id: environmental-domain
 * label: Umweltbereich, für den Umweltziele festgelegt werden können.
 * sourceType: TEMPLATES
 * entries:
 *   air: Luft
 *   climateAndClimateChange: Klima und Klimawandel
 *   healthProtection: Gesundheitsschutz
 *   landUse: Bodennutzung
 *   naturalResources: natürliche Ressourcen
 *   natureAndBiodiversity: Natur und biologische Vielfalt
 *   noise: Lärm
 *   soil: Boden
 *   sustainableDevelopment: nachhaltige Entwicklung
 *   waste: Abfall
 *   water: Wasser
 * ```
 * </code>
 *
 * ## Storage
 * @langEn Codelists reside under the relative path `store/entities/codelists/{codelistId}.yml` in the
 * data directory.
 * @langDe Die Codelisten liegen als YAML-Dateien im ldproxy-Datenverzeichnis unter dem relativen
 * Pfad `store/entities/codelists/{codelistId}.yml`.
 */
@DocFile(path = "configuration/codelists", name = "README.md")
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCodelistData.Builder.class)
public interface CodelistData extends EntityData {

    enum IMPORT_TYPE {
        TEMPLATES,
        GML_DICTIONARY,
        ONEO_SCHLUESSELLISTE
    }

    abstract class Builder implements EntityDataBuilder<CodelistData> {
    }

    /**
     * @langEn Human readable label.
     * @langDe Eine lesbare Bezeichnung der Codelist, die im Manager angezeigt wird.
     * @default
     */
    String getLabel();

    /**
     * @langEn Map with the original value as key and the new value as value. Values
     * might use [`stringFormat` transformations](../providers/transformations.md).
     * @langDe Jeder Eintrag bildet einen Wert auf den neuen Wert ab.
     * @default `{}`
     */
    Map<String, String> getEntries();

    /**
     * @langEn Always `TEMPLATES`.
     * @langDe `TEMPLATES` für alle manuell erstellte Codelisten.
     * @default
     */
    IMPORT_TYPE getSourceType();

    Optional<String> getSourceUrl();

    /**
     * @langEn Optional default value. Might use [`stringFormat` transformations](../providers/transformations.md).
     * @langDe Optional kann ein Defaultwert angegeben werden. Dabei können auch [`stringFormat`-Transformationen]
     * (../providers/transformations.md) genutzt werden.
     * @default the value
     */
    Optional<String> getFallback();
}
