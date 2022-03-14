package de.ii.xtraplatform.features.gml.infra.fes;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.features.gml.infra.req.FES.VERSION;
import de.ii.xtraplatform.features.gml.infra.xml.XMLDocument;
import org.w3c.dom.Element;

public class FesEnvelope extends FesExpression {

  private final BoundingBox boundingBox;

  public FesEnvelope(BoundingBox boundingBox) {
    this.boundingBox = boundingBox;
  }

  public BoundingBox getBoundingBox() {
    return boundingBox;
  }

  @Override
  public void toXML(VERSION version, Element e, XMLDocument doc) {

  }
}
