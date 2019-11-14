package de.ii.xtraplatform.feature.provider.pgis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathColumn;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathJoin;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSegmentDiff;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlMappingParser;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSegmentDiff;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import io.dropwizard.jackson.Jackson;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static de.ii.xtraplatform.feature.provider.pgis.SqlPathTree.depthFirst;

public class SqlPathTreeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlPathTreeTest.class);

    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
    private static final TypeReference<HashMap<String, Object>> GENERIC_MAP = new TypeReference<HashMap<String, Object>>() {
    };

    private static int comparePathsByPriority(Map.Entry<String, Optional<Integer>> path1,
                                              Map.Entry<String, Optional<Integer>> path2) {
        return !path1.getValue()
                     .isPresent() ? 1 : !path2.getValue()
                                              .isPresent() ? -1 : path1.getValue()
                                                                       .get() - path2.getValue()
                                                                                     .get();
    }

    private static Map.Entry<String, Optional<Integer>> extractPathAndSortPriority(Map.Entry<String, Object> mapping) {
        Map<String, Object> s2 = (Map<String, Object>) mapping.getValue();
        Map<String, Object> s22 = (Map<String, Object>) s2.get("general");
        Optional<Integer> s222 = Optional.ofNullable(s22.get("sortPriority"))
                                         .map(o -> ((Integer) o));

        return new AbstractMap.SimpleImmutableEntry<>(mapping.getKey(), s222);
    }

    private static Map.Entry<String, List<String>> extractSortedPaths(Map.Entry<String, Object> featureType) {
        Map<String, Object> featureTypeMappings = (Map<String, Object>) featureType.getValue();

        List<String> sortedPaths = featureTypeMappings.entrySet()
                                                      .stream()
                                                      .map(SqlPathTreeTest::extractPathAndSortPriority)
                                                      .sorted(SqlPathTreeTest::comparePathsByPriority)
                                                      .map(Map.Entry::getKey)
                                                      .collect(Collectors.toList());

        return new AbstractMap.SimpleImmutableEntry<>(featureType.getKey(), sortedPaths);
    }

    private static Map<String, Object> readMappings(Path serviceFile) throws IOException {

        Map<String, Object> service = MAPPER.readValue(serviceFile.toFile(), GENERIC_MAP);
        Map<String, Object> featureProvider = (Map<String, Object>) service.get("featureProvider");
        Map<String, Object> allMappings = (Map<String, Object>) featureProvider.get("mappings");

        return allMappings;
    }

    private static Map<String, List<String>> readMappingPaths(Map<String, Object> mappings) throws IOException {

        return mappings.entrySet()
                       .stream()
                       .map(SqlPathTreeTest::extractSortedPaths)
                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static SqlPathSegmentDiff treeToDiff(SqlPathTree tree) {
        ImmutableSqlPathSegmentDiff.Builder builder = ImmutableSqlPathSegmentDiff.builder()
                                                                                 .type(tree.getType())
                                                                                 .path(tree.getPath())
                                                                                 .table(tree.getTableName());
        if (tree.getParentPaths()
                .size() > 0) {
            builder.parentPath(tree.getParentPaths()
                                   .get(tree.getParentPaths()
                                            .size() - 1));
        }

        for (int i = 0; i < tree.getColumns()
                                .size(); i++) {
            builder.addColumns(ImmutableSqlPathColumn.builder()
                                                     .path(tree.getColumnPaths()
                                                               .get(i))
                                                     .build());
        }
        for (int i = 0; i < tree.getJoinPathElements()
                                .size(); i++) {
            if (tree.getJoinPathElements()
                    .get(i)
                    .second()
                    .isPresent()) {
                builder.addJoins(ImmutableSqlPathJoin.builder()
                                                     .targetTable(tree.getJoinPathElements()
                                                                      .get(i)
                                                                      .first())
                                                     .targetColumn(tree.getJoinPathElements()
                                                                       .get(i)
                                                                       .second()
                                                                       .get()
                                                                       .get(1))
                                                     .sourceColumn(tree.getJoinPathElements()
                                                                       .get(i)
                                                                       .second()
                                                                       .get()
                                                                       .get(0))
                                                     .build());
            } else {
                boolean br2 = true;
            }
        }

        return builder.build();
    }

    private static SqlPathSegmentDiff segmentToDiff(SqlPathTable segment) {
        ImmutableSqlPathSegmentDiff.Builder builder = ImmutableSqlPathSegmentDiff.builder()
                                                                                 .type(segment.getType())
                                                                                 .path(segment.getPath())
                                                                                 .table(segment.getTableName());
        if (Objects.nonNull(segment.getParent())) {
            builder.parentPath(segment.getParent()
                                      .getPath());
        }

        for (int i = 0; i < segment.getColumns()
                                   .size(); i++) {
            builder.addColumns(ImmutableSqlPathColumn.builder()
                                                     .path(segment.getColumnPaths()
                                                                  .get(i))
                                                     .build());
        }
        for (int i = 0; i < segment.getJoinPathElements()
                                   .size(); i++) {
            if (segment.getJoinPathElements()
                       .get(i)
                       .second()
                       .isPresent()) {
                builder.addJoins(ImmutableSqlPathJoin.builder()
                                                     .targetTable(segment.getJoinPathElements()
                                                                         .get(i)
                                                                         .first())
                                                     .targetColumn(segment.getJoinPathElements()
                                                                          .get(i)
                                                                          .second()
                                                                          .get()
                                                                          .get(1))
                                                     .sourceColumn(segment.getJoinPathElements()
                                                                          .get(i)
                                                                          .second()
                                                                          .get()
                                                                          .get(0))
                                                     .build());
            } else {
                boolean br2 = true;
            }
        }

        return builder.build();
    }

    private static Map<String, SqlPathTree> toPathTrees(Map<String, List<String>> paths) {
        return paths.entrySet()
                    .stream()
                    .map(featureTypePaths -> {
                        SqlPathTree sqlPathTree = new SqlPathTree.Builder()
                                .fromPaths(featureTypePaths.getValue())
                                .build();

                        return new AbstractMap.SimpleImmutableEntry<>(featureTypePaths.getKey(), sqlPathTree);
                    })
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    static SqlPathParser sqlPathParser = new SqlPathParser();

    private static Map<String, SqlPathTable> toPathSegments(Map<String, Object> mappings) {
        return mappings.entrySet()
                    .stream()
                    .map(featureTypePaths -> {
                        SqlPathTable sqlPathTable = sqlPathParser.parse(toSourcePathMappings((Map<String, Object>) featureTypePaths.getValue()));

                        return new AbstractMap.SimpleImmutableEntry<>(featureTypePaths.getKey(), sqlPathTable);
                    })
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, List<SqlPathTable>> toPaths(Map<String, Object> mappings) {
        SqlPathSyntax sqlPathSyntax = ImmutableSqlPathSyntax.builder().build();
        SqlMappingParser mappingParser = new SqlMappingParser(sqlPathSyntax);

        return mappings.entrySet()
                       .stream()
                       .map(featureTypePaths -> {
                           Map<String, SourcePathMapping> stringSourcePathMappingMap = toSourcePathMappings((Map<String, Object>) featureTypePaths.getValue());
                           List<String> paths = mappingParser.parse(stringSourcePathMappingMap);
                           List<SqlPathTable> sqlPaths = sqlPathSyntax.parse(paths);

                           return new AbstractMap.SimpleImmutableEntry<>(featureTypePaths.getKey(), sqlPaths);
                       })
                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    public void test() throws IOException {

        Path serviceFile = Paths.get("/home/zahnen/development/ldproxy-pgis/build/data/store/entities/services/oneo");

        Map<String, Object> mappings = readMappings(serviceFile);
        Map<String, List<String>> paths = readMappingPaths(mappings);

        List<SqlPathTree> pathTrees = new ArrayList<>(toPathTrees(paths).values());

        List<SqlPathTable> pathSegments = new ArrayList<>(toPathSegments(mappings).values());


        //TODO
        Map<String, List<SqlPathTable>> stringListMap = toPaths(mappings);
        //TODO


        for (int i = 0; i < pathTrees.size(); i++) {
            List<SqlPathSegmentDiff> old = StreamSupport.stream(depthFirst(pathTrees.get(i)).spliterator(), true)
                                                        .map(sqlPathTree -> treeToDiff(sqlPathTree))
                                                        .collect(Collectors.toList());

            List<SqlPathSegmentDiff> nu = StreamSupport.stream(SqlPathTable.depthFirst(pathSegments.get(i))
                                                                           .spliterator(), true)
                                                       .map(sqlPathSegment -> segmentToDiff(sqlPathSegment))
                                                       .collect(Collectors.toList());

            Assert.assertEquals(old, nu);
        }
    }

    enum TYPES {UNKNOWN, ID}

    public static Map<String, SourcePathMapping> toSourcePathMappings(Map<String, Object> mappings) {
        return mappings.entrySet()
                                                                    .stream()
                                                                    .map(stringObjectEntry -> {

                                                                        Map<String, Object> value = (Map<String, Object>) stringObjectEntry.getValue();
                                                                        Map<String, Object> general = (Map<String, Object>) value.get(TargetMapping.BASE_TYPE);

                                                                        ImmutableSourcePathMapping sourcePathMapping = new ImmutableSourcePathMapping.Builder().putMappings(TargetMapping.BASE_TYPE, new TargetMapping() {


                                                                            @Nullable
                                                                            @Override
                                                                            public String getName() {
                                                                                return null;
                                                                            }

                                                                            @Nullable
                                                                            @Override
                                                                            public Enum getType() {
                                                                                return Optional.ofNullable(general.get("type"))
                                                                                               .map(val -> Objects.equals(val, "ID"))
                                                                                               .orElse(false) ? TYPES.ID : TYPES.UNKNOWN;
                                                                            }

                                                                            @Nullable
                                                                            @Override
                                                                            public Boolean getEnabled() {
                                                                                return true;
                                                                            }

                                                                            @Nullable
                                                                            @Override
                                                                            public Integer getSortPriority() {
                                                                                return (Integer) general.get("sortPriority");
                                                                            }

                                                                            @Nullable
                                                                            @Override
                                                                            public String getFormat() {
                                                                                return null;
                                                                            }

                                                                            @Override
                                                                            public boolean isSpatial() {
                                                                                return false;
                                                                            }
                                                                        })
                                                                                                                                                               .build();
                                                                        return new AbstractMap.SimpleImmutableEntry<>(stringObjectEntry.getKey(), sourcePathMapping);
                                                                    })
                                                                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}