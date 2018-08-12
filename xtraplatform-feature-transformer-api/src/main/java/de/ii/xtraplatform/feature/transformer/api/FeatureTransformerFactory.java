package de.ii.xtraplatform.feature.transformer.api;

import java.io.OutputStream;

/**
 * @author zahnen
 */
public interface FeatureTransformerFactory {
    FeatureTransformer create(FeatureTransformerParameter parameter);

    abstract class FeatureTransformerParameter {
        public abstract OutputStream getOutputStream();
    }
}
