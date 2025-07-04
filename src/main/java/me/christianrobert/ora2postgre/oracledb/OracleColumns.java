package me.christianrobert.ora2postgre.oracledb;

public class OracleColumns {
  public String name;
  public String dataType; // e.g., VARCHAR2, NUMBER(10,2)
  public int dataLength;
  public int dataPrecision;
  public int dataScale;

  public OracleColumns(String name, String dataType, int dataLength, int dataPrecision, int dataScale) {
    this.name = name;
    this.dataType = dataType;
    this.dataLength = dataLength;
    this.dataPrecision = dataPrecision;
    this.dataScale = dataScale;
  }
}
