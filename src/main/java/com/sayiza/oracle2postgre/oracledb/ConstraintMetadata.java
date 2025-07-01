package com.sayiza.oracle2postgre.oracledb;

import java.util.ArrayList;
import java.util.List;

// Represents a constraint (e.g., primary key)
public class ConstraintMetadata {
  private String constraintName;
  private String constraintType; // e.g., "P" for primary key
  private List<String> columnNames;

  public ConstraintMetadata(String constraintName, String constraintType) {
    this.constraintName = constraintName;
    this.constraintType = constraintType;
    this.columnNames = new ArrayList<>();
  }

  public void addColumnName(String columnName) { columnNames.add(columnName); }

  // Getters
  public String getConstraintName() { return constraintName; }
  public String getConstraintType() { return constraintType; }
  public List<String> getColumnNames() { return columnNames; }

  @Override
  public String toString() {
    return "ConstraintMetadata{name='" + constraintName + "', type='" + constraintType + "', columns=" + columnNames + "}";
  }
}