package de.ii.xtraplatform.features.gml.domain;

public enum GmlType {
  ID("ID"),
  STRING("string"),
  DATE_TIME("dateTime"),
  DATE("date"),
  GEOMETRY("geometry"),
  DECIMAL("decimal"),
  DOUBLE("double"),
  FLOAT("float"),
  INT("int"),
  INTEGER("integer"),
  LONG("long"),
  SHORT("short"),
  BOOLEAN("boolean"),
  URI("anyURI"),
  NONE("");

  private String stringRepresentation;

  GmlType(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
  }

  @Override
  public String toString() {
    return stringRepresentation;
  }

  public static GmlType fromString(String type) {
    for (GmlType v : GmlType.values()) {
      if (v.toString().equals(type)) {
        return v;
      }
    }

    return NONE;
  }

  public static boolean contains(String type) {
    for (GmlType v : GmlType.values()) {
      if (v.toString().equals(type)) {
        return true;
      }
    }
    return false;
  }

  public boolean isValid() {
    return this != NONE;
  }
}
