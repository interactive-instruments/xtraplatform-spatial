package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;
import java.util.ListIterator;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface FeatureStoreRelatedContainer extends FeatureStoreAttributesContainer {

    List<FeatureStoreRelation> getInstanceConnection();

    @Override
    @Value.Derived
    default String getInstanceContainerName() {
        return getInstanceConnection().get(0).getSourceContainer();
    }

    @Override
    @Value.Derived
    @Value.Auxiliary
    default List<String> getSortKeys() {
        ImmutableList.Builder<String> keys = ImmutableList.builder();

        FeatureStoreRelation previousRelation = null;

        for (int i = 0; i < getInstanceConnection().size(); i++) {
            FeatureStoreRelation relation = getInstanceConnection().get(i);
            // add keys only for main table and target tables of M:N or 1:N relations
            if (i == 0 || previousRelation.isM2N() || previousRelation.isOne2N()) {
                keys.add(String.format("%s.%s", relation.getSourceContainer(), relation.getSourceSortKey()));
            }
            previousRelation = relation;
        }

        keys.add(String.format("%s.%s", getName(), getSortKey()));

        return keys.build();
    }

    //TODO: should we do this here? can we derive it from the above?
    default List<String> getSortKeys(ListIterator<String> aliasesIterator) {
        ImmutableList.Builder<String> keys = ImmutableList.builder();

        int keyIndex = 0;
        FeatureStoreRelation previousRelation = null;

        for (int i = 0; i < getInstanceConnection().size(); i++) {
            FeatureStoreRelation relation = getInstanceConnection().get(i);
            String alias = aliasesIterator.next();
            if (relation.isM2N()) {
                //skip junction alias
                aliasesIterator.next();
            }

            // add keys only for main table and target tables of M:N or 1:N relations
            if (i == 0 || previousRelation.isM2N() || previousRelation.isOne2N()) {
                String suffix = keyIndex > 0 ? "_" + keyIndex : "";
                keys.add(String.format("%s.%s AS SKEY%s", alias, relation.getSourceSortKey(), suffix));
                keyIndex++;
            }

            previousRelation = relation;
        }

        // add key for value table
        keys.add(String.format("%s.%s AS SKEY_%d", aliasesIterator.next(), getSortKey(), keyIndex));

        return keys.build();
    }
}