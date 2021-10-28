package de.ii.xtraplatform.routes.sql.domain;

import de.ii.xtraplatform.cql.domain.Geometry.Point;
import de.ii.xtraplatform.features.domain.FeatureQueryExtension;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
public interface RouteQuery extends FeatureQueryExtension {

  Point getStart();

  Point getEnd();

  List<Point> getWayPoints();

  List<String> getFlags();
}
