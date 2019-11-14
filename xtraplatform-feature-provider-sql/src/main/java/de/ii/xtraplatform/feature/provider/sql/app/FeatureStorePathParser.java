package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPath;
import de.ii.xtraplatform.feature.provider.sql.SqlPath;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreRelation;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreRelation.CARDINALITY;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureStoreAttribute;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureStoreRelatedContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureStoreRelation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FeatureStorePathParser {

    private final SqlPathSyntax syntax;

    public FeatureStorePathParser(SqlPathSyntax syntax) {
        this.syntax = syntax;
    }

    public List<FeatureStoreInstanceContainer> parse(List<String> paths) {


        List<SqlPath> sortedPaths = paths
                .stream()
                .sorted(this::sortByPriority)
                .map(this::toSqlPath)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        List<FeatureStoreInstanceContainer> mergedPaths = toInstanceContainers(sortedPaths);

        //Set<SqlPath> allPaths = fanOutObjectTables(mergedPaths);


        //return toTableTree(ImmutableList.copyOf(allPaths));

        return mergedPaths;
    }

    //TODO: test with mappings without sortPriority, e.g. daraa
    private int sortByPriority(String path1, String path2) {
        OptionalInt priority1 = syntax.getPriorityFlag(path1);
        OptionalInt priority2 = syntax.getPriorityFlag(path2);

        return !priority1.isPresent() ? 1 : !priority2.isPresent() ? -1 : priority1.getAsInt() - priority2.getAsInt();
    }

    //TODO: merge into toInstanceContainers
    private Optional<SqlPath> toSqlPath(String path) {
        Matcher matcher = syntax.getColumnPathPattern()
                                .matcher(path);

        if (matcher.find()) {
            String tablePath = matcher.group(SqlPathSyntax.MatcherGroups.PATH);
            List<String> columns = syntax.getMultiColumnSplitter()
                                         .splitToList(matcher.group(SqlPathSyntax.MatcherGroups.COLUMNS));
            String flags = matcher.group(SqlPathSyntax.MatcherGroups.FLAGS);
            OptionalInt priority = syntax.getPriorityFlag(flags);
            boolean hasOid = syntax.getOidFlag(flags);
            List<String> tablePathAsList = syntax.asList(tablePath);
            boolean isRoot = tablePathAsList.size() == 1;
            boolean isJunction = syntax.isJunctionTable(tablePathAsList.get(tablePathAsList.size() - 1));
            Optional<String> queryable = syntax.getQueryableFlag(flags);
            boolean isSpatial = syntax.getSpatialFlag(flags);

            return Optional.of(ImmutableSqlPath.builder()
                                               .tablePath(tablePath)
                                               .columns(columns)
                                               .hasOid(hasOid)
                                               .sortPriority(priority)
                                               .isRoot(isRoot)
                                               .isJunction(isJunction)
                                               .queryable(queryable)
                                               .isSpatial(isSpatial)
                                               .build());
        }

        return Optional.empty();
    }

    private List<FeatureStoreInstanceContainer> toInstanceContainers(List<SqlPath> sqlPaths) {
        LinkedHashMap<String, List<SqlPath>> groupedPaths = sqlPaths.stream()
                                                                    .collect(Collectors.groupingBy(SqlPath::getTablePath, LinkedHashMap::new, Collectors.toList()));

        LinkedHashMap<String, ImmutableFeatureStoreInstanceContainer.Builder> instanceContainerBuilders = new LinkedHashMap<>();

        //TODO
        final int[] instancePos = {0};

        groupedPaths.entrySet()
                    .stream()
                    //TODO: is this really needed?
                    //.sorted(Comparator.comparingInt(entry -> syntax.asList(entry.getKey())
                    //                                               .size()))
                    .forEach(entry -> {
                        String tablePath = entry.getKey();
                        List<String> tablePathAsList = syntax.asList(tablePath);
                        List<SqlPath> columnPaths = entry.getValue();
                        List<String> columns = columnPaths.stream()
                                                          .flatMap(sqlPath -> sqlPath.getColumns()
                                                                                     .stream())
                                                          .collect(Collectors.toList());
                        List<FeatureStoreAttribute> attributes = columnPaths.stream()
                                                                            .flatMap(sqlPath -> sqlPath.getColumns()
                                                                                                       .stream()
                                                                                                       .map(name -> ImmutableFeatureStoreAttribute.builder()
                                                                                                                                                  .name(name)
                                                                                                                                                  .path(tablePathAsList)
                                                                                                                                                  .addPath(name)
                                                                                                                                                  .queryable(sqlPath.getQueryable())
                                                                                                                                                  .isSpatial(sqlPath.isSpatial())
                                                                                                                                                  .build()))
                                                                            .collect(Collectors.toList());
                        boolean hasOid = columnPaths.stream()
                                                    .anyMatch(SqlPath::hasOid);
                        OptionalInt priority = columnPaths.stream()
                                                          .flatMapToInt(columnPath -> {
                                                              OptionalInt sortPriority = columnPath.getSortPriority();
                                                              return sortPriority.isPresent() ? IntStream.of(sortPriority.getAsInt()) : IntStream.empty();
                                                          })
                                                          .findFirst();
                        boolean isRoot = columnPaths.stream()
                                                    .anyMatch(SqlPath::isRoot);
                        Matcher instanceContainerNameMatcher = syntax.getTablePattern()
                                                                     .matcher(tablePathAsList.get(0));
                        if (!instanceContainerNameMatcher.find()) {
                            throw new IllegalArgumentException();
                        }
                        String instanceContainerName = instanceContainerNameMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);
                        Matcher attributesContainerNameMatcher = syntax.getTablePattern()
                                                                       .matcher(tablePathAsList.get(tablePathAsList.size() - 1));
                        if (!attributesContainerNameMatcher.find()) {
                            throw new IllegalArgumentException();
                        }
                        String attributesContainerName = attributesContainerNameMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);

                        if (!instanceContainerBuilders.containsKey(instanceContainerName)) {
                            instanceContainerBuilders.put(instanceContainerName, ImmutableFeatureStoreInstanceContainer.builder());
                        }

                        if (isRoot) {
                            instanceContainerBuilders.get(instanceContainerName)
                                                     .name(instanceContainerName)
                                                     .path(tablePathAsList)
                                                     //TODO: default id field from syntax options, optional id flag
                                                     .sortKey("id")
                                                     .attributes(attributes)
                                                     .attributesPosition(instancePos[0]);
                            instancePos[0] = 0;
                        } else {
                            List<FeatureStoreRelation> instanceConnection = toRelations(tablePathAsList);
                            //TODO: default id field from syntax options, optional id flag
                            String sortKey = syntax.isJunctionTable(attributesContainerName)
                                    //TODO: oneo uses columns.get(columns.size()-1) instead, thats not a good default value
                                    //TODO: support flag {orderBy=btkomplex_id}{orderDir=ASC}
                                    ? instanceConnection.get(instanceConnection.size() - 1)
                                                        .getTargetField()
                                    : "id";

                            ImmutableFeatureStoreRelatedContainer attributesContainer = ImmutableFeatureStoreRelatedContainer.builder()
                                                                                                                             .name(attributesContainerName)
                                                                                                                             .path(tablePathAsList)
                                                                                                                             //TODO
                                                                                                                             .sortKey(sortKey)
                                                                                                                             .instanceConnection(instanceConnection)
                                                                                                                             .attributes(attributes)
                                                                                                                             .build();

                            instanceContainerBuilders.get(instanceContainerName)
                                                     .addRelatedContainers(attributesContainer);

                            instancePos[0]++;
                        }
                    });

        return instanceContainerBuilders.values()
                                        .stream()
                                        .map(ImmutableFeatureStoreInstanceContainer.Builder::build)
                                        .collect(Collectors.toList());
    }

    private List<FeatureStoreRelation> toRelations(List<String> path) {

        if (path.size() < 2) {
            throw new IllegalArgumentException(String.format("not a valid relation path: %s", path));
        }

        if (path.size() > 2) {
            return IntStream.range(2, path.size())
                            .mapToObj(i -> toRelations(path.get(i - 2), path.get(i - 1), path.get(i), i == path.size() - 1))
                            .flatMap(Function.identity())
                            .collect(Collectors.toList());
        }

        return IntStream.range(1, path.size())
                        .mapToObj(i -> toRelation(path.get(i - 1), path.get(i)))
                        .collect(Collectors.toList());
    }

    private Stream<FeatureStoreRelation> toRelations(String source, String link, String target, boolean isLast) {
        if (syntax.isJunctionTable(source)) {
            if (isLast) {
                return Stream.of(toRelation(link, target));
            } else {
                return Stream.empty();
            }
        }
        if (syntax.isJunctionTable(target) && !isLast) {
            return Stream.of(toRelation(source, link));
        }

        if (syntax.isJunctionTable(link)) {
            return Stream.of(toRelation(source, link, target));
        }

        return Stream.of(toRelation(source, link), toRelation(link, target));
    }

    //TODO: support sortKey flag on table instead of getDefaultPrimaryKey
    private FeatureStoreRelation toRelation(String source, String target) {
        Matcher sourceMatcher = syntax.getTablePattern()
                                      .matcher(source);
        Matcher targetMatcher = syntax.getJoinedTablePattern()
                                      .matcher(target);
        if (sourceMatcher.find() && targetMatcher.find()) {
            String sourceField = targetMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD);
            String targetField = targetMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD);
            boolean isOne2One = Objects.equals(targetField, syntax.getOptions()
                                                                  .getDefaultPrimaryKey());

            return ImmutableFeatureStoreRelation.builder()
                                                .cardinality(isOne2One ? CARDINALITY.ONE_2_ONE : CARDINALITY.ONE_2_N)
                                                .sourceContainer(sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                                                .sourceField(sourceField)
                                                .sourceSortKey(syntax.getOptions()
                                                                     .getDefaultPrimaryKey())
                                                .targetContainer(targetMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                                                .targetField(targetField)
                                                .build();
        }

        throw new IllegalArgumentException(String.format("not a valid relation path: %s/%s", source, target));
    }

    private FeatureStoreRelation toRelation(String source, String link, String target) {
        Matcher sourceMatcher = syntax.getTablePattern()
                                      .matcher(source);
        Matcher junctionMatcher = syntax.getJoinedTablePattern()
                                        .matcher(link);
        Matcher targetMatcher = syntax.getJoinedTablePattern()
                                      .matcher(target);
        if (sourceMatcher.find() && junctionMatcher.find() && targetMatcher.find()) {
            return ImmutableFeatureStoreRelation.builder()
                                                .cardinality(CARDINALITY.M_2_N)
                                                .sourceContainer(sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                                                .sourceField(junctionMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD))
                                                .sourceSortKey(syntax.getOptions()
                                                                     .getDefaultPrimaryKey())
                                                .junctionSource(junctionMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD))
                                                .junction(junctionMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                                                .junctionTarget(targetMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD))
                                                .targetContainer(targetMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                                                .targetField(targetMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD))
                                                .build();
        }

        throw new IllegalArgumentException(String.format("not a valid relation path: %s/%s/%s", source, link, target));
    }
}
