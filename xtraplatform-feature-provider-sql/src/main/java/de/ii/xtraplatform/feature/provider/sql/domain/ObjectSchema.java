package de.ii.xtraplatform.feature.provider.sql.domain;

import java.util.List;

public interface ObjectSchema {

    List<ObjectSchema> getChildren();

    List<ValueSchema> getProperties();
}
