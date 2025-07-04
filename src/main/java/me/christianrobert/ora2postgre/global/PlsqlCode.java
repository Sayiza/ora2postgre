package me.christianrobert.ora2postgre.global;

public class PlsqlCode {
  public String schema;
  public String code;

  public PlsqlCode(String schema, String code) {
    this.schema = schema;
    this.code = code;
  }
}
