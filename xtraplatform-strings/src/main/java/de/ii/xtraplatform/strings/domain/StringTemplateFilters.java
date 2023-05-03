/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.strings.domain;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.CoreHtmlNodeRenderer;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class StringTemplateFilters {

  private static final Logger LOGGER = LoggerFactory.getLogger(StringTemplateFilters.class);

  static final Set<Extension> EXTENSIONS = Collections.singleton(TablesExtension.create());
  static final Parser parser = Parser.builder().extensions(EXTENSIONS).build();

  static final HtmlRenderer renderer =
      HtmlRenderer.builder()
          .extensions(EXTENSIONS)
          .nodeRendererFactory(
              context ->
                  new CoreHtmlNodeRenderer(context) {
                    @Override
                    public void visit(Paragraph paragraph) {
                      this.visitChildren(paragraph);
                    }
                  })
          .attributeProviderFactory(
              context ->
                  (node, tagName, attributes) -> {
                    if (node instanceof Link) {
                      attributes.put("target", "_blank");
                    }
                  })
          .build();

  public static String applyFilterMarkdown(String value) {

    Node document = parser.parse(value);
    return renderer.render(document);
  }

  public static String applyTemplate(String template, String value) {
    return applyTemplate(template, value, isHtml -> {});
  }

  public static String applyTemplate(String template, String value, Consumer<Boolean> isHtml) {
    return applyTemplate(template, value, isHtml, "value");
  }

  public static String applyTemplate(
      String template, String value, Consumer<Boolean> isHtml, String valueSubst) {
    if (Objects.isNull(value) || value.isEmpty()) {
      return "";
    }

    if (Objects.isNull(template) || template.isEmpty()) {
      return value;
    }

    return applyTemplate(
        template, isHtml, key -> Objects.equals(key, valueSubst) ? value : null, false);
  }

  public static String applyTemplate(String template, Function<String, String> valueLookup) {
    return applyTemplate(template, isHtml -> {}, valueLookup, false);
  }

  public static String applyTemplate(
      String template, Function<String, String> valueLookup, boolean allowSingleCurlyBraces) {
    return applyTemplate(template, isHtml -> {}, valueLookup, allowSingleCurlyBraces);
  }

  static Pattern valuePattern = Pattern.compile("\\{\\{([\\w.]+)( ?\\| ?[\\w]+(:'[^']*')*)*\\}\\}");
  static Pattern valuePatternSingle = Pattern.compile("\\{([\\w.]+)( ?\\| ?[\\w]+(:'[^']*')*)*\\}");
  static Pattern filterPattern = Pattern.compile(" ?\\| ?([\\w]+)((?::'[^']*')*)");

  public static String applyTemplate(
      String template,
      Consumer<Boolean> isHtml,
      Function<String, String> valueLookup,
      boolean allowSingleCurlyBraces) {

    if (Objects.isNull(template) || template.isEmpty()) {
      return "";
    }

    String formattedValue = "";
    Matcher matcher =
        !allowSingleCurlyBraces || template.contains("{{")
            ? valuePattern.matcher(template)
            : valuePatternSingle.matcher(template);
    boolean hasAppliedMarkdown = false;
    Map<String, String> assigns = new HashMap<>();

    int lastMatch = 0;
    while (matcher.find()) {
      String key = matcher.group(1);
      String filteredValue = valueLookup.apply(key);
      Matcher matcher2 = filterPattern.matcher(template.substring(matcher.start(), matcher.end()));
      while (matcher2.find()) {
        String filter = matcher2.group(1);
        String params = matcher2.group(2);
        List<String> parameters =
            matcher2.groupCount() < 2
                ? ImmutableList.of()
                : Splitter.onPattern("(?<=^|'):(?='|$)")
                    .omitEmptyStrings()
                    .splitToList(params)
                    .stream()
                    .map(s -> s.substring(1, s.length() - 1))
                    .collect(Collectors.toList());

        if (Objects.isNull(filteredValue)) {
          if (filter.equals("orElse") && parameters.size() >= 1) {
            filteredValue =
                applyTemplate(parameters.get(0).replace("\"", "'"), isHtml, valueLookup, false);
          }
        } else {
          if (filter.equals("markdown")) {
            filteredValue = applyFilterMarkdown(filteredValue);
            hasAppliedMarkdown = true;
          } else if (filter.equals("replace") && parameters.size() >= 2) {
            filteredValue = filteredValue.replaceAll(parameters.get(0), parameters.get(1));
          } else if (filter.equals("prepend") && parameters.size() >= 1) {
            filteredValue = parameters.get(0) + filteredValue;
          } else if (filter.equals("append") && parameters.size() >= 1) {
            filteredValue = filteredValue + parameters.get(0);
          } else if (filter.equals("urlEncode") || filter.equals("urlencode")) {
            try {
              filteredValue = URLEncoder.encode(filteredValue, Charsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
              // ignore
            }
          } else if (filter.equals("toLower")) {
            filteredValue = filteredValue.toLowerCase();
          } else if (filter.equals("toUpper")) {
            filteredValue = filteredValue.toUpperCase();
          } else if (filter.equals("assignTo") && parameters.size() >= 1) {
            assigns.put(parameters.get(0), filteredValue);
          } else if (filter.equals("unHtml")) {
            filteredValue = filteredValue.replaceAll("<.*?>", "");
          } else if (!filter.equals("orElse")) {
            LOGGER.warn("Template filter '{}' not supported", filter);
          }
        }
      }
      formattedValue +=
          template.substring(lastMatch, matcher.start())
              + Objects.requireNonNullElse(filteredValue, "");
      lastMatch = matcher.end();
    }
    formattedValue += template.substring(lastMatch);
    for (Map.Entry<String, String> entry : assigns.entrySet()) {
      String valueSubst2 = entry.getKey();
      String value2 = entry.getValue();
      formattedValue = formattedValue.replaceAll("\\{\\{" + valueSubst2 + "}}", value2);
    }

    isHtml.accept(hasAppliedMarkdown);

    return formattedValue;
  }
}
