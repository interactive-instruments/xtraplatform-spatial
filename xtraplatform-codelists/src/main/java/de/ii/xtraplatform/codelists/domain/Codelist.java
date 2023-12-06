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
import de.ii.xtraplatform.values.domain.StoredValue;
import de.ii.xtraplatform.values.domain.ValueBuilder;
import de.ii.xtraplatform.values.domain.annotations.FromValueStore;
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
 * label: Umweltbereich, für den Umweltziele festgelegt werden können.
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
 *     the data directory (old) or in the [Store (new)](41-store-new.md) as values with type
 *     `codelists`.
 *     <p>When using the new store layout, codelists have a path instead of an id. That means for
 *     example `values/codelists/bar.yml` would work like before, but you could additionally define
 *     `values/codelists/foo/bar.yml`. To reference that codelist somewhere else in the
 *     configuration you would need to use `foo/bar`.
 * @langDe ## Speicherung
 *     <p>Die Codelisten liegen als YAML-Dateien im ldproxy-Datenverzeichnis unter dem relativen
 *     Pfad `store/entities/codelists/{codelistId}.yml` (alt) oder im [Store (neu)](41-store-new.md)
 *     als Values mit Typ `codelists`.
 *     <p>Wenn das neue Store-Layout verwendet wird, haben Codelisten einen Pfad anstatt einer Id.
 *     Das heißt z.B. `values/codelists/bar.yml` würde wie vorher funktionieren, aber man könnte
 *     zusätzlich `values/codelists/foo/bar.yml` definieren. Um diese Codelist an anderer Stelle in
 *     der Konfiguration zu referenzieren würde man `foo/bar` verwenden.
 * @ref:cfgProperties {@link de.ii.xtraplatform.codelists.domain.ImmutableCodelist}
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
@FromValueStore(type = "codelists")
@JsonDeserialize(builder = ImmutableCodelist.Builder.class)
public interface Codelist extends StoredValue {

  enum ImportType {
    TEMPLATES,
    GML_DICTIONARY,
    ONEO_SCHLUESSELLISTE
  }

  abstract class Builder implements ValueBuilder<Codelist> {}

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
   * @langDe Immer `TEMPLATES`.
   * @default TEMPLATES
   */
  Optional<ImportType> getSourceType();

  @DocIgnore
  Optional<String> getSourceUrl();

  /**
   * @langEn Optional default value.
   * @langDe Optional kann ein Defaultwert angegeben werden.
   * @default the value
   */
  Optional<String> getFallback();

  default String getValue(String key) {
    return Optional.ofNullable(getEntries().get(key)).orElse(getFallback().orElse(key));
  }
}
