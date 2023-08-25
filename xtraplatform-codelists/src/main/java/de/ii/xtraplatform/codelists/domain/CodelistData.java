/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.codelists.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn # Codelists
 *     <p>Codelists allow to map property values to a different value. This is useful especially for
 *     HTML representations.
 * @langDe # Codelisten
 *     <p>Codelisten können zum Übersetzen von Eigenschaftswerten in einen anderen Wert genutzt
 *     werden, meist für die HTML-Ausgabe.
 * @langEn ## Configuration
 *     <p>The following table describes the structure of the code list files.
 *     <p>{@docTable:properties}
 *     <p>For the target values in `entries` and for `fallback` also
 *     [stringFormat](../providers/details/transformations.md) transformations can be used. If the
 *     transformed value is intended for HTML output, then Markdown markup can also be used, this
 *     will be formatted in the HTML output.
 * @langDe ## Konfiguration
 *     <p>Die nachfolgende Tabelle beschreibt die Struktur der Codelisten-Dateien.
 *     <p>{@docTable:properties}
 *     <p>Bei den Zielwerten in `entries` und bei `fallback` können auch
 *     [stringFormat](../providers/details/transformations.md)-Transformationen genutzt werden. Ist
 *     der transformierte Wert für die HTML-Ausgabe gedacht, dann kann auch Markdown-Markup
 *     verwendet werden, dieser wird bei der HTML-Ausgabe aufbereitet.
 * @langEn ### Example
 *     <p>Based on the INSPIRE codelist
 *     [EnvironmentalDomain](https://inspire.ec.europa.eu/codeList/EnvironmentalDomain), maps values
 *     like `soil` to the German label of the entry in the INSPIRE codelist registry.
 * @langDe ### Beispiel
 *     <p>Basierend auf der INSPIRE-Codelist
 *     [EnvironmentalDomain](https://inspire.ec.europa.eu/codeList/EnvironmentalDomain) werden Werte
 *     wie `soil` auf das deutschsprachige Label in der INSPIRE-Codelist-Registry abgebildet:
 * @langAll <code>
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
 * @langEn ## Storage
 *     <p>Codelists reside under the relative path `store/entities/codelists/{codelistId}.yml` in
 *     the data directory.
 * @langDe ## Speicherung
 *     <p>Die Codelisten liegen als YAML-Dateien im ldproxy-Datenverzeichnis unter dem relativen
 *     Pfad `store/entities/codelists/{codelistId}.yml`.
 * @ref:cfgProperties {@link de.ii.xtraplatform.codelists.domain.ImmutableCodelistData}
 */
@DocFile(
    path = "auxiliaries",
    name = "codelists.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES)
    })
@Value.Immutable
@Value.Style(
    builder = "new",
    deepImmutablesDetection = true,
    attributeBuilderDetection = true,
    passAnnotations = DocIgnore.class)
@JsonDeserialize(builder = ImmutableCodelistData.Builder.class)
public interface CodelistData extends EntityData {

  enum ImportType {
    TEMPLATES,
    GML_DICTIONARY,
    ONEO_SCHLUESSELLISTE
  }

  abstract class Builder implements EntityDataBuilder<CodelistData> {
    public abstract Builder id(String id);

    public ImmutableCodelistData.Builder fillRequiredFieldsWithPlaceholders() {
      return ((ImmutableCodelistData.Builder) this.id("__DEFAULT__"));
    }
  }

  /**
   * @langEn Human readable label.
   * @langDe Eine lesbare Bezeichnung der Codelist, die im Manager angezeigt wird.
   * @default id
   */
  Optional<String> getLabel();

  /**
   * @langEn Map with the original value as key and the new value as value.
   * @langDe Jeder Eintrag bildet einen Original-Wert auf den neuen Wert ab.
   * @default `{}`
   */
  Map<String, String> getEntries();

  /**
   * @langEn Always `TEMPLATES`.
   * @langDe `TEMPLATES` für alle manuell erstellte Codelisten.
   * @default TEMPLATES
   */
  ImportType getSourceType();

  @DocIgnore
  Optional<String> getSourceUrl();

  /**
   * @langEn Optional default value.
   * @langDe Optional kann ein Defaultwert angegeben werden.
   * @default the value
   */
  Optional<String> getFallback();
}
