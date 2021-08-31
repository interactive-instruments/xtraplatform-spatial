package org.kortforsyningen.proj;

import java.util.Objects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ProjExtensions {

  //TODO: not implemented in JNI layer, add in org_kortforsyningen_proj_SharedPointer.h and bindings.cpp
  public static CoordinateReferenceSystem normalizeForVisualization(final CoordinateReferenceSystem operation) {
    Objects.requireNonNull(operation);
    if (operation instanceof IdentifiableObject) {
      return (CoordinateReferenceSystem) ((IdentifiableObject) operation).impl.normalizeForVisualization();
    } else {
      throw new UnsupportedImplementationException("operation", operation);
    }
  }

}
