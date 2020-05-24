package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

public class FeatureSchemaMapper<T extends SchemaBase<T>> implements FeatureReaderGeneric {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSchemaMapper.class);

    private final SchemaMapping<T> mapping;
    private final FeatureReader<T, T> delegate;
    private List<T> currentNesting;
    private List<String> currentNestingPath;

    public FeatureSchemaMapper(SchemaMapping<T> mapping,
                               FeatureReader<T, T> delegate) {
        this.mapping = mapping;
        this.delegate = delegate;
        this.currentNesting = new ArrayList<>();
        this.currentNestingPath = new ArrayList<>();
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched,
                        Map<String, String> context) throws Exception {
        delegate.onStart(numberReturned, numberMatched, mapping.getTargetSchema());
    }

    @Override
    public void onEnd() throws Exception {
        delegate.onEnd();
    }

    @Override
    public void onFeatureStart(List<String> path, Map<String, String> context) throws Exception {
        delegate.onFeatureStart(path, mapping.getTargetSchema());
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        delegate.onFeatureEnd(path);
    }

    @Override
    public void onObjectStart(List<String> path, Map<String, String> context) throws Exception {
        List<T> targetSchemas = mapping.getTargetSchemas(path);

        T objectSchema = targetSchemas.get(targetSchemas.size() - 1);

        //TODO
        if (objectSchema.getType() == SchemaBase.Type.GEOMETRY && context.containsKey("geometryType")) {
            SimpleFeatureGeometry geometryType = SimpleFeatureGeometry.valueOf(context.get("geometryType").toUpperCase());

            if (!objectSchema.getGeometryType().isPresent()) {
                objectSchema = mapping.schemaWithGeometryType(objectSchema, geometryType);
                targetSchemas = Lists.newArrayList(Iterables.concat(targetSchemas.subList(0, targetSchemas.size()-1), ImmutableList.of(objectSchema)));
            } else {
                //TODO: warn or reject
            }
        }

        if (objectSchema.isObject() && objectSchema.isArray() && Objects.equals(currentNesting, targetSchemas)) {
            delegate.onObjectStart(path, objectSchema);
        }

        ensureNestingIsOpen(path, targetSchemas);
    }

    @Override
    public void onObjectEnd(List<String> path) throws Exception {
        List<T> targetSchemas = mapping.getTargetSchemas(path);

        if (Objects.equals(path.get(0), "geometry")) {
            closeDiffering(ImmutableList.of(), ImmutableList.of());
            return;
        }

        T objectSchema = targetSchemas.get(targetSchemas.size() - 1);

        if (objectSchema.isObject()) {
            if (!objectSchema.isArray() && Objects.equals(peek(), objectSchema)) {
                pop();
            }

            delegate.onObjectEnd(path);
        }

        //closeLast(path, targetSchemas);
    }

    @Override
    public void onArrayStart(List<String> path, Map<String, String> context) throws Exception {
        List<T> targetSchemas = mapping.getTargetSchemas(path);

        T objectSchema = targetSchemas.get(targetSchemas.size() - 1);

        //TODO
        if (objectSchema.getType() == SchemaBase.Type.GEOMETRY && context.containsKey("geometryType")) {
            SimpleFeatureGeometry geometryType = SimpleFeatureGeometry.valueOf(context.get("geometryType").toUpperCase());

            if (!objectSchema.getGeometryType().isPresent()) {
                objectSchema = mapping.schemaWithGeometryType(objectSchema, geometryType);
                targetSchemas = Lists.newArrayList(Iterables.concat(targetSchemas.subList(0, targetSchemas.size()-1), ImmutableList.of(objectSchema)));
            } else {
                //TODO: warn or reject
            }
        }

        ensureNestingIsOpen(path, targetSchemas);

        if (Objects.equals(path.get(0), "geometry")) {
            delegate.onArrayStart(path, targetSchemas.get(targetSchemas.size() - 1));
        }
    }

    @Override
    public void onArrayEnd(List<String> path) throws Exception {
        List<T> targetSchemas = mapping.getTargetSchemas(path);

        //TODO
        if (Objects.equals(path.get(0), "geometry")) {
            delegate.onArrayEnd(path);
            return;
        }

        T arraySchema = targetSchemas.get(targetSchemas.size() - 1);

        if (arraySchema.isArray()) {
            if (Objects.equals(peek(), arraySchema)) {
                pop();
            }

            delegate.onArrayEnd(path);
        }

        //closeLast(path, targetSchemas);
    }

    //TODO: who closes nesting, e.g. osirisobjekt
    @Override
    public void onValue(List<String> path, String value, Map<String, String> context) throws Exception {
        List<T> targetSchemas = mapping.getTargetSchemas(path);

        //TODO
        if (Objects.equals(path.get(0), "geometry")) {
            //return;
            boolean br = true;
        }

        if (targetSchemas.isEmpty()) {
            LOGGER.warn("No mapping found for path {}.", path);
            return;
        }

        if (targetSchemas.size() > 1) {
            ensureNestingIsOpen(path, targetSchemas);
        }

        T valueSchema = targetSchemas.get(targetSchemas.size() - 1);

        delegate.onValue(path, value, valueSchema);
    }

    private void ensureNestingIsOpen(List<String> path, List<T> targetSchemas) throws Exception {
        closeDiffering(path, targetSchemas);


        for (T targetSchema : targetSchemas) {
            if (targetSchema.isObject() || targetSchema.isArray()) {
                if (!currentNesting.contains(targetSchema)) {
                    push(targetSchema);

                    switch (targetSchema.getType()) {
                        case OBJECT:
                        case GEOMETRY:
                            delegate.onObjectStart(path, targetSchema);
                            break;
                        case VALUE_ARRAY:
                        case OBJECT_ARRAY:
                            delegate.onArrayStart(path, targetSchema);
                            break;
                    }
                }
            }
        }

        currentNestingPath = path;
    }

    private void closeDiffering(List<String> path, List<T> targetSchemas) throws Exception {
        /*if (Objects.equals(currentNestingPath, path)) {
            return;
        }*/

        int commonPrefixLength = getCommonPrefixLength(currentNesting, targetSchemas);

        //if (commonPrefixLength > 0) {
        int to = currentNesting.size() - commonPrefixLength;
        for (int i = 0; i < to; i++) {
            T toClose = pop();
            close(currentNestingPath, toClose);
        }

        return;
        //}

        //TODO: this is for opening
        /*int overlapLength = getOverlapLength(currentNesting, targetSchemas);

        if (overlapLength > 0) {
            for (int i = 0; i < overlapLength; i++) {
                T toClose = pop();
                close(currentNestingPath, toClose);
            }

        }*/

    }

    private void closeLast(List<String> path, List<T> targetSchemas, boolean isArray) throws Exception {
        for (int i = targetSchemas.size() - 1; i >= 0; i--) {
            T targetSchema = targetSchemas.get(i);
            if ((!isArray && targetSchema.isObject()) || (isArray && targetSchema.isArray())) {
                if (Objects.equals(peek(), targetSchema)) {
                    pop();

                    close(path, targetSchema);
                }
            }
            break;
        }
    }

    private void close(List<String> path, T targetSchema) throws Exception {
        switch (targetSchema.getType()) {
            case OBJECT:
                delegate.onObjectEnd(path);
                break;
            case VALUE_ARRAY:
            case OBJECT_ARRAY:
                delegate.onArrayEnd(path);
                break;
        }
    }

    private void push(T targetSchema) {
        currentNesting.add(targetSchema);
    }

    private T pop() {
        if (currentNesting.isEmpty()) {
            return null;
        }
        return currentNesting.remove(currentNesting.size() - 1);
    }

    private T peek() {
        if (currentNesting.isEmpty()) {
            return null;
        }
        return currentNesting.get(currentNesting.size() - 1);
    }

    private int getCommonPrefixLength(List<T> path1, List<T> path2) {
        int commonSize = Math.min(path1.size(), path2.size());

        int index = 0;
        for (; index < commonSize; index++) {
            if (!Objects.equals(path1.get(index), path2.get(index))) {
                return index;
            }
        }

        return commonSize;
    }

    private int getOverlapLength(List<T> path1, List<T> path2) {
        int possibleOverlapIndex = path1.size();
        int path2Index = 0;

        for (int i = path1.size() - 1; i >= 0; i++) {
            if (Objects.equals(path1.get(i), path2.get(path2Index))) {
                possibleOverlapIndex = i;
                break;
            }
        }

        int overlapLength = path1.size() - possibleOverlapIndex;

        if (overlapLength > path2.size()) {
            return 0;
        }

        for (int i = possibleOverlapIndex; i < path1.size() - 1; i++) {
            if (!Objects.equals(path1.get(i), path2.get(path2Index))) {
                return 0;
            }
            path2Index++;
        }

        return overlapLength;
    }
}
