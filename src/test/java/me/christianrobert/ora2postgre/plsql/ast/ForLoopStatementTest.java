package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.SynonymExtractor;
import me.christianrobert.ora2postgre.oracledb.SynonymMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class ForLoopStatementTest {

  @Test
  public void testForLoopToPostgre() {
    // Test Oracle function with FOR loop
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getnamefunction( pNr number ) 
    return varchar2
  is 
  begin 
    for r in ( select nr , name name1 from NAMETABLE )
    loop
      return r.name1;
    end loop;
    return null;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    // Add required table metadata for NAMETABLE
    TableMetadata nameTable = new TableMetadata("TEST_SCHEMA", "NAMETABLE");
    data.getTableSql().add(nameTable);

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Expected PostgreSQL function structure
    String expectedPattern = """
            CREATE OR REPLACE FUNCTION getnamefunction(pnr numeric)
            RETURNS varchar
            AS $$
            DECLARE
                r RECORD;
            BEGIN
                FOR r IN (SELECT nr, name AS name1, test_schema.testpackage_getnamefunction(nr) AS name2 FROM nametable)
                LOOP
                    RETURN r.name1;
                END LOOP;
                RETURN NULL;
            END;
            $$ LANGUAGE plpgsql;
            """;

    // Basic validation - should contain key PostgreSQL elements
    assert postgreSql.contains("DECLARE") : "Should contain DECLARE section";
    assert postgreSql.contains("RECORD") : "Should declare record variable";
    assert postgreSql.contains("FOR") : "Should contain FOR loop";
    assert postgreSql.contains("LOOP") : "Should contain LOOP keyword";
    assert postgreSql.contains("END LOOP") : "Should contain END LOOP";
  }
}