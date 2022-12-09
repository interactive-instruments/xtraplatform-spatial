/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.xtraplatform.docs.DocColumn;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocFilesTemplate;
import de.ii.xtraplatform.docs.DocFilesTemplate.ForEach;
import de.ii.xtraplatform.docs.DocI18n;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.docs.DocVar;
import java.util.function.BiConsumer;

/**
 * @langEn # Overview
 *     <p>{@docTable:overview}
 * @langDe # Overview
 *     <p>{@docTable:overview}
 */
@DocFile(
    path = "providers/feature/extensions",
    name = "README.md",
    tables = {
      @DocTable(
          name = "overview",
          rows = {@DocStep(type = Step.IMPLEMENTATIONS)
            // @DocStep(type = Step.SORTED, params = "{@sortPriority}")
          },
          columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "[{@title}]({@docFile:name})"),
                header = {
                  @DocI18n(language = "en", value = "Extension"),
                  @DocI18n(language = "de", value = "Erweiterung")
                }),
            @DocColumn(
                value =
                    @DocStep(
                        type = Step.TAG,
                        params =
                            "<SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"center\" />"),
                header = {
                  @DocI18n(language = "en", value = "Classification"),
                  @DocI18n(language = "de", value = "Klassifizierung")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@body}"),
                header = {
                  @DocI18n(language = "en", value = "Description"),
                  @DocI18n(language = "de", value = "Beschreibung")
                })
          })
    })
@DocFilesTemplate(
    files = ForEach.IMPLEMENTATION,
    path = "providers/feature/extensions",
    // stripSuffix = "BuildingBlock",
    template = {
      @DocI18n(
          language = "en",
          value =
              "# {@title}\n\n"
                  + "<SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"super\" />\n\n"
                  + "{@body}\n\n"
                  + "## Scope\n\n"
                  + "{@scopeEn}\n\n"
                  + "## Configuration\n\n"
                  + "{@docTable:properties ### Options\n\n||| This extension has no configuration options.}\n\n"
                  + "{@docVar:example ### Example\n\n|||}\n"),
      @DocI18n(
          language = "de",
          value =
              "# {@title}\n\n"
                  + "<SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"super\" />\n\n"
                  + "{@body}\n\n"
                  + "## Umfang\n\n"
                  + "{@scopeDe}\n\n"
                  + "## Konfiguration\n\n"
                  + "{@docTable:properties ### Optionen\n\n||| Diese Erweiterung hat keine Konfigurationsoptionen.}\n\n"
                  + "{@docVar:example ### Beispiel\n\n|||}\n")
    },
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@propertyTable}"),
            @DocStep(type = Step.JSON_PROPERTIES),
            @DocStep(type = Step.UNMARKED)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    },
    vars = {
      @DocVar(
          name = "example",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@example}"),
            @DocStep(type = Step.TAG, params = "{@example}")
          })
    })
@AutoMultiBind
public interface FeatureQueriesExtension {

  enum LIFECYCLE_HOOK {
    STARTED
  }

  enum QUERY_HOOK {
    BEFORE,
    AFTER
  }

  boolean isSupported(FeatureProviderConnector<?, ?, ?> connector);

  void on(
      LIFECYCLE_HOOK hook, FeatureProvider2 provider, FeatureProviderConnector<?, ?, ?> connector);

  void on(
      QUERY_HOOK hook,
      FeatureProviderDataV2 data,
      FeatureProviderConnector<?, ?, ?> connector,
      Query query,
      BiConsumer<String, String> aliasResolver);
}
