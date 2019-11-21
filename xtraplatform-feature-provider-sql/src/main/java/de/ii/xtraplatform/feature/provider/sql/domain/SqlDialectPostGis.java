package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.base.Splitter;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.EpsgCrs;

import java.util.List;
import java.util.Optional;

public class SqlDialectPostGis implements SqlDialect {

    private final static Splitter BBOX_SPLITTER = Splitter.onPattern("[(), ]")
                                                          .omitEmptyStrings()
                                                          .trimResults();

    @Override
    public String applyToWkt(String column) {
        return String.format("ST_AsText(ST_ForcePolygonCCW(%s))", column);
    }

    @Override
    public String applyToExtent(String column) {
        return String.format("ST_Extent(%s)", column);
    }

    @Override
    public Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs) {
        List<String> bbox = BBOX_SPLITTER.splitToList(extent);

        if (bbox.size() > 4) {
            return Optional.of(new BoundingBox(Double.parseDouble(bbox.get(1)), Double.parseDouble(bbox.get(2)), Double.parseDouble(bbox.get(3)), Double.parseDouble(bbox.get(4)), crs));
        }

        return Optional.empty();
    }

}
