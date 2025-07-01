package com.sayiza.oracle2postgre.oracledb;

import java.util.List;
import com.sayiza.oracle2postgre.global.PostgreSqlIdentifierUtils;

public class SynonymMetadata {
  private String schema; // Schema owning the synonym (Oracle user)
  private String synonymName; // Synonym name
  private String referencedSchema; // Schema of the referenced object
  private String referencedObjectName; // Name of the referenced object (table or view)
  private String referencedObjectType; // Type of referenced object (e.g., TABLE, VIEW)

  public SynonymMetadata(String schema, String synonymName, String referencedSchema,
                         String referencedObjectName, String referencedObjectType) {
    this.schema = schema;
    this.synonymName = synonymName;
    this.referencedSchema = referencedSchema;
    this.referencedObjectName = referencedObjectName;
    this.referencedObjectType = referencedObjectType;
  }

  // Getters
  public String getSchema() { return schema; }
  public String getSynonymName() { return synonymName; }
  public String getReferencedSchema() { return referencedSchema; }
  public String getReferencedObjectName() { return referencedObjectName; }
  public String getReferencedObjectType() { return referencedObjectType; }

  @Override
  public String toString() {
    return "SynonymMetadata{schema='" + schema + "', synonymName='" + synonymName +
            "', referencedSchema='" + referencedSchema + "', referencedObjectName='" +
            referencedObjectName + "', referencedObjectType='" + referencedObjectType + "'}";
  }

  /**
   * Generates PostgreSQL-compatible statements for the synonym.
   * For now, outputs a comment as a placeholder, as synonyms will be handled by replacing
   * references with schema-qualified names in a later step.
   *
   * @return List of SQL statements (comments for now)
   */
  public List<String> toPostgre() {
    String quotedSynonym = PostgreSqlIdentifierUtils.quoteIdentifier(synonymName);
    String quotedRefSchema = PostgreSqlIdentifierUtils.quoteIdentifier(referencedSchema);
    String quotedRefObject = PostgreSqlIdentifierUtils.quoteIdentifier(referencedObjectName);
    String comment = String.format("-- Synonym: %s.%s -> %s.%s (%s)",
            PostgreSqlIdentifierUtils.quoteIdentifier(schema), quotedSynonym,
            quotedRefSchema, quotedRefObject, referencedObjectType);
    return List.of(comment);
  }

}