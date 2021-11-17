/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.feature.provider.sql.app.FilterEncoderSql;
import de.ii.xtraplatform.features.domain.ImmutableTuple;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlPath.Builder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlPathParser {

  private enum MatcherGroups {
    PATH,
    TABLE,
    COLUMNS,
    SOURCEFIELD,
    TARGETFIELD,
    FLAGS,
    SORTKEY,
    PRIMARYKEY,
    FILTER
  }

  private interface Tokens {
    String PATH_SEPARATOR = "/";
    String MULTI_COLUMN_SEPARATOR = ":";
    String JOIN_START = "[";
    String JOIN_SEPARATOR = "=";
    String JOIN_END = "]";
  }

  private interface PatternStrings {
    String IDENTIFIER = "[a-zA-Z_]{1}[a-zA-Z0-9_]*";
    String FLAGS = "(?:\\{[a-z_]+.*?\\})*";
    String SORT_KEY_FLAG = String.format("\\{sortKey=(?<%s>.+?)\\}", MatcherGroups.SORTKEY);
    String PRIMARY_KEY_FLAG =
        String.format("\\{primaryKey=(?<%s>.+?)\\}", MatcherGroups.PRIMARYKEY);
    String FILTER_FLAG = String.format("\\{filter=(?<%s>.+?)\\}", MatcherGroups.FILTER);
    // TODO: remove
    String JUNCTION_FLAG = "\\{junction\\}";
    String COLUMN =
        String.format("%s(?:%s%s)*", IDENTIFIER, Tokens.MULTI_COLUMN_SEPARATOR, IDENTIFIER);
    String JOIN =
        String.format(
            "%s(?<%s>%s)%s(?<%s>%s)%s",
            Pattern.quote(Tokens.JOIN_START),
            MatcherGroups.SOURCEFIELD,
            IDENTIFIER,
            Pattern.quote(Tokens.JOIN_SEPARATOR),
            MatcherGroups.TARGETFIELD,
            IDENTIFIER,
            Pattern.quote(Tokens.JOIN_END));
    String JOIN_PLAIN =
        String.format(
            "%s(?:%s)%s(?:%s)%s",
            Pattern.quote(Tokens.JOIN_START),
            IDENTIFIER,
            Pattern.quote(Tokens.JOIN_SEPARATOR),
            IDENTIFIER,
            Pattern.quote(Tokens.JOIN_END));
    String ROOT_TABLE =
        String.format(
            "(?:%s)(?<%s>%s)(?<%s>%s)?",
            Tokens.PATH_SEPARATOR, MatcherGroups.TABLE, IDENTIFIER, MatcherGroups.FLAGS, FLAGS);
    String JOINED_TABLE =
        String.format(
            "%s(?<%s>%s)(?<%s>%s)?",
            JOIN, MatcherGroups.TABLE, IDENTIFIER, MatcherGroups.FLAGS, FLAGS);
    String JOINED_TABLE_PLAIN = String.format("(?:%s)(?:%s)(?:%s)?", JOIN_PLAIN, IDENTIFIER, FLAGS);
    String COLUMN_PATH =
        String.format(
            "(?<%s>(?:%s%s)*)(?<%s>%s)(?<%s>%s)?",
            MatcherGroups.PATH,
            JOINED_TABLE_PLAIN,
            Tokens.PATH_SEPARATOR,
            MatcherGroups.COLUMNS,
            COLUMN,
            MatcherGroups.FLAGS,
            FLAGS);
  }

  private interface Patterns {
    Pattern ROOT_TABLE = Pattern.compile(PatternStrings.ROOT_TABLE);

    Pattern JOINED_TABLE = Pattern.compile(PatternStrings.JOINED_TABLE);

    Pattern COLUMN_PATH = Pattern.compile(PatternStrings.COLUMN_PATH);

    // TODO: remove
    Pattern JUNCTION_TABLE_FLAG = Pattern.compile(PatternStrings.JUNCTION_FLAG);

    Pattern SORT_KEY_FLAG = Pattern.compile(PatternStrings.SORT_KEY_FLAG);

    Pattern FILTER_FLAG = Pattern.compile(PatternStrings.FILTER_FLAG);

    Pattern PRIMARY_KEY_FLAG = Pattern.compile(PatternStrings.PRIMARY_KEY_FLAG);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlPathParser.class);

  private static final Splitter PATH_SPLITTER =
      Splitter.on(Tokens.PATH_SEPARATOR).omitEmptyStrings();
  private static final Joiner PATH_JOINER = Joiner.on(Tokens.PATH_SEPARATOR).skipNulls();
  private static final Splitter MULTI_COLUMN_SPLITTER =
      Splitter.on(Tokens.MULTI_COLUMN_SEPARATOR).omitEmptyStrings();

  private final SqlPathDefaults defaults;
  private final Cql cql;
  // TODO: remove
  private final Optional<Pattern> junctionTableMatcher;

  public SqlPathParser(SqlPathDefaults defaults, Cql cql) {
    this.defaults = defaults;
    this.cql = cql;
    this.junctionTableMatcher = defaults.getJunctionTablePattern().map(Pattern::compile);
  }

  public SqlPath parseColumnPath(String path) {
    Matcher matcher = Patterns.COLUMN_PATH.matcher(path);

    if (matcher.find()) {
      String column = matcher.group(MatcherGroups.COLUMNS.name());

      if (Objects.nonNull(column)) {
        List<String> columns = MULTI_COLUMN_SPLITTER.splitToList(column);
        Builder builder = new ImmutableSqlPath.Builder().name(column).columns(columns);

        String tablePath = matcher.group(MatcherGroups.PATH.name());

        if (Objects.nonNull(tablePath)) {
          builder.parentTables(parseTables(tablePath));
        }

        // TODO
        builder.sortKey("").primaryKey("").junction(false);

        return builder.build();
      }
    }

    throw new IllegalArgumentException("Invalid sourcePath in provider configuration: " + path);
  }

  public SqlPath parseTablePath(String path) {
    Matcher matcher = Patterns.ROOT_TABLE.matcher(path);

    if (matcher.find()) {
      return parseTable(matcher, false);
    }

    List<SqlPath> sqlPaths = parseTables(path);

    if (!sqlPaths.isEmpty()) {
      return new ImmutableSqlPath.Builder()
          .from(sqlPaths.get(sqlPaths.size() - 1))
          .parentTables(sqlPaths.subList(0, sqlPaths.size() - 1))
          .build();
    }

    throw new IllegalArgumentException("Invalid sourcePath in provider configuration: " + path);
  }

  private List<SqlPath> parseTables(String tablePath) {
    List<SqlPath> tables = new ArrayList<>();
    Matcher tableMatcher = Patterns.JOINED_TABLE.matcher(tablePath);

    while (tableMatcher.find()) {
      tables.add(parseTable(tableMatcher, true));
    }

    return tables;
  }

  private SqlPath parseTable(Matcher tableMatcher, boolean hasJoin) {
    String table = tableMatcher.group(MatcherGroups.TABLE.name());
    Builder builder = new ImmutableSqlPath.Builder().name(table);

    if (hasJoin) {
      String sourceField = tableMatcher.group(MatcherGroups.SOURCEFIELD.name());
      String targetField = tableMatcher.group(MatcherGroups.TARGETFIELD.name());

      builder.name(table).join(ImmutableTuple.of(sourceField, targetField));
    }

    String flags = Optional.ofNullable(tableMatcher.group(MatcherGroups.FLAGS.name())).orElse("");

    builder
        .sortKey(getSortKey(flags))
        .primaryKey(getPrimaryKey(flags))
        .junction(isJunctionTable(table, flags))
        .filter(getFilterFlag(flags).map(filterText -> cql.read(filterText, Format.TEXT)))
        .filterString(getFilterFlag(flags));

    return builder.build();
  }

  public List<String> asList(String path) {
    return PATH_SPLITTER.splitToList(path);
  }

  public String asString(List<String> pathElements) {
    StringBuilder path = new StringBuilder();
    if (!pathElements.isEmpty() && !pathElements.get(0).startsWith(Tokens.PATH_SEPARATOR)) {
      path.append(Tokens.PATH_SEPARATOR);
    }
    PATH_JOINER.appendTo(path, pathElements);

    return path.toString();
  }

  public String asString(String... pathAndPathElements) {
    return asString(Arrays.asList(pathAndPathElements));
  }

  public boolean isJunctionTable(String table, String flags) {
    return junctionTableMatcher.filter(pattern -> pattern.matcher(table).find()).isPresent()
        || Patterns.JUNCTION_TABLE_FLAG.matcher(flags).find();
  }

  public String getSortKey(String flags) {
    Matcher matcher = Patterns.SORT_KEY_FLAG.matcher(flags);

    if (matcher.find()) {
      return matcher.group(MatcherGroups.SORTKEY.name());
    }

    return defaults.getSortKey();
  }

  public String getPrimaryKey(String flags) {
    Matcher matcher = Patterns.PRIMARY_KEY_FLAG.matcher(flags);

    if (matcher.find()) {
      return matcher.group(MatcherGroups.PRIMARYKEY.name());
    }

    return defaults.getPrimaryKey();
  }

  public Optional<String> getFilterFlag(String flags) {
    Matcher matcher = Patterns.FILTER_FLAG.matcher(flags);

    if (matcher.find()) {
      return Optional.of(matcher.group(MatcherGroups.FILTER.name()));
    }

    return Optional.empty();
  }

  public List<SqlRelation> extractRelations(SqlPath parentPath, SqlPath path) {

    List<SqlPath> allTables = getAllTables(parentPath, path);

    return extractRelations(allTables);
  }

  public List<SqlRelation> extractRelations(SchemaSql parent, SqlPath path) {

    List<SqlPath> allTables =
        getAllTables(
            new ImmutableSqlPath.Builder()
                .name(parent.getName())
                .sortKey(parent.getSortKey().orElse(defaults.getSortKey()))
                .primaryKey(parent.getPrimaryKey().orElse(defaults.getPrimaryKey()))
                .filter(parent.getFilter())
                .filterString(parent.getFilter().map(filterCql -> cql.write(filterCql, Format.TEXT)))
                .junction(false)
                .build(),
            path);

    return extractRelations(allTables);
  }

  private List<SqlRelation> extractRelations(List<SqlPath> paths) {
    if (paths.size() < 2) {
      return ImmutableList.of();
    }

    if (paths.size() == 2) {
      return IntStream.range(1, paths.size())
          .mapToObj(i -> toRelation(paths.get(i - 1), paths.get(i)))
          .collect(Collectors.toList());
    }

    return IntStream.range(2, paths.size())
        .mapToObj(
            i ->
                toRelations(
                    paths.get(i - 2), paths.get(i - 1), paths.get(i), i == paths.size() - 1))
        .flatMap(Function.identity())
        .collect(Collectors.toList());
  }

  private List<SqlPath> getAllTables(SqlPath parentPath, SqlPath path) {
    ImmutableList.Builder<SqlPath> pathsBuilder =
        new ImmutableList.Builder<SqlPath>().add(parentPath).addAll(path.getParentTables());

    if (path.isBranch()) {
      pathsBuilder.add(path);
    }

    return pathsBuilder.build();
  }

  private Stream<SqlRelation> toRelations(
      SqlPath source, SqlPath link, SqlPath target, boolean isLast) {
    if (source.isJunction()) {
      if (isLast) {
        return Stream.of(toRelation(link, target));
      } else {
        return Stream.empty();
      }
    }
    if (target.isJunction() && !isLast) {
      return Stream.of(toRelation(source, link));
    }

    if (link.isJunction()) {
      return Stream.of(toRelation(source, link, target));
    }

    return Stream.of(toRelation(source, link), toRelation(link, target));
  }

  // TODO: support sortKey flag on table instead of getDefaultPrimaryKey
  private SqlRelation toRelation(SqlPath source, SqlPath target) {
    if (!target.getJoin().isPresent()) {
      throw new IllegalArgumentException();
    }

    String sourceField = target.getJoin().get().first();
    String targetField = target.getJoin().get().second();
    boolean isOne2One = Objects.equals(targetField, source.getPrimaryKey());

    return new ImmutableSqlRelation.Builder()
        .cardinality(
            isOne2One ? SqlRelation.CARDINALITY.ONE_2_ONE : SqlRelation.CARDINALITY.ONE_2_N)
        .sourceContainer(source.getName())
        .sourceField(sourceField)
        .sourcePrimaryKey(source.getPrimaryKey())
        .sourceSortKey(source.getSortKey())
        .sourceFilter(source.getFilterString())
        .targetContainer(target.getName())
        .targetField(targetField)
        .targetFilter(target.getFilterString())
        .build();
  }

  private SqlRelation toRelation(SqlPath source, SqlPath link, SqlPath target) {
    if (!link.getJoin().isPresent() || !target.getJoin().isPresent()) {
      throw new IllegalArgumentException();
    }

    String sourceField = link.getJoin().get().first();
    String junctionSourceField = link.getJoin().get().second();
    String junctionTargetField = target.getJoin().get().first();
    String targetField = target.getJoin().get().second();

    return new ImmutableSqlRelation.Builder()
        .cardinality(SqlRelation.CARDINALITY.M_2_N)
        .sourceContainer(source.getName())
        .sourceField(sourceField)
        .sourcePrimaryKey(source.getPrimaryKey())
        .sourceSortKey(source.getSortKey())
        .sourceFilter(source.getFilterString())
        .junctionSource(junctionSourceField)
        .junction(link.getName())
        .junctionTarget(junctionTargetField)
        .junctionFilter(link.getFilterString())
        .targetContainer(target.getName())
        .targetField(targetField)
        .targetFilter(target.getFilterString())
        .build();
  }
}
