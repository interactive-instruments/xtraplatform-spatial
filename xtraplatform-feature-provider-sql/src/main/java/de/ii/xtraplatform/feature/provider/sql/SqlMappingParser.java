package de.ii.xtraplatform.feature.provider.sql;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SqlMappingParser {

    private final SqlPathSyntax syntax;

    public SqlMappingParser(SqlPathSyntax syntax) {
        this.syntax = syntax;
    }

    /**
     * normalizes input, delegates to {@link SqlPathParser#parse(List)}
     *
     * @param featureTypeMappings sql paths with mapping instructions
     * @return tree of {@link SqlPathTable}
     */
    public List<String> parse(Map<String, SourcePathMapping> featureTypeMappings) {
        return featureTypeMappings.entrySet()
                                  .stream()
                                  .map(this::toPathWithFlags)
                                  .collect(ImmutableList.toImmutableList());
    }

    private String toPathWithFlags(Map.Entry<String, SourcePathMapping> mapping) {
        String path = mapping.getKey();
        Optional<TargetMapping> generalMapping = Optional.ofNullable(mapping.getValue()
                                                                            .getMappingForType(TargetMapping.BASE_TYPE));
        Optional<Integer> sortPriority = generalMapping.map(TargetMapping::getSortPriority);
        boolean isOid = generalMapping.flatMap(targetMapping -> Optional.ofNullable(targetMapping.getType()))
                                      .map(enumValue -> enumValue.toString()
                                                                 .equals("ID"))
                                      .orElse(false);

        if (isOid) {
            path = syntax.setOidFlag(path);
        }
        if (sortPriority.isPresent()) {
            path = syntax.setPriorityFlag(path, sortPriority.get());
        }

        return path;
    }
}
