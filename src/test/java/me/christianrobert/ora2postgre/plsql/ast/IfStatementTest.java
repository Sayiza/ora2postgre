package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class IfStatementTest {

  @Test
  public void testSimpleIfStatementToPostgre() {
    // Test Oracle function with simple IF statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION testifsimple( pId number ) 
    return varchar2
  is 
    vResult varchar2(100);
  begin 
    if pId > 0 then
      vResult := 'Positive';
    end if;
    return vResult;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain key PostgreSQL IF elements
    assert postgreSql.contains("IF") : "Should contain IF keyword";
    assert postgreSql.contains("THEN") : "Should contain THEN keyword";
    assert postgreSql.contains("END IF") : "Should contain END IF";
    assert postgreSql.contains("pId") : "Should contain condition";
    assert postgreSql.contains("vResult := 'Positive'") : "Should contain assignment";
  }

  @Test
  public void testIfElseStatementToPostgre() {
    // Test Oracle function with IF-ELSE statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION testifelse( pId number ) 
    return varchar2
  is 
    vResult varchar2(100);
  begin 
    if pId > 0 then
      vResult := 'Positive';
    else
      vResult := 'Zero or Negative';
    end if;
    return vResult;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain key PostgreSQL IF-ELSE elements
    assert postgreSql.contains("IF") : "Should contain IF keyword";
    assert postgreSql.contains("THEN") : "Should contain THEN keyword";
    assert postgreSql.contains("ELSE") : "Should contain ELSE keyword";
    assert postgreSql.contains("END IF") : "Should contain END IF";
    assert postgreSql.contains("'Positive'") : "Should contain positive branch";
    assert postgreSql.contains("'Zero or Negative'") : "Should contain negative branch";
  }

  @Test
  public void testIfElsifElseStatementToPostgre() {
    // Test Oracle function with IF-ELSIF-ELSE statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION testifelseif( pId number ) 
    return varchar2
  is 
    vResult varchar2(100);
  begin 
    if pId > 0 then
      vResult := 'Positive';
    elsif pId < 0 then
      vResult := 'Negative';
    else
      vResult := 'Zero';
    end if;
    return vResult;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain key PostgreSQL IF-ELSIF-ELSE elements
    assert postgreSql.contains("IF") : "Should contain IF keyword";
    assert postgreSql.contains("THEN") : "Should contain THEN keyword";
    assert postgreSql.contains("ELSIF") : "Should contain ELSIF keyword";
    assert postgreSql.contains("ELSE") : "Should contain ELSE keyword";
    assert postgreSql.contains("END IF") : "Should contain END IF";
    assert postgreSql.contains("'Positive'") : "Should contain positive branch";
    assert postgreSql.contains("'Negative'") : "Should contain negative branch";
    assert postgreSql.contains("'Zero'") : "Should contain zero branch";
  }

  @Test
  public void testTriggerLikeIfStatementToPostgre() {
    // Test Oracle function with IF statement like used in triggers
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION audittrigger( pAction varchar2, pId number ) 
    return varchar2
  is 
  begin 
    if pAction = 'INSERT' then
      insert into audit_table (action, table_id, timestamp) values ('INSERT', pId, sysdate);
    elsif pAction = 'UPDATE' then
      insert into audit_table (action, table_id, timestamp) values ('UPDATE', pId, sysdate);
    end if;
    return 'OK';
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain key PostgreSQL elements for audit trigger logic
    assert postgreSql.contains("IF") : "Should contain IF keyword";
    assert postgreSql.contains("ELSIF") : "Should contain ELSIF keyword";
    assert postgreSql.contains("'INSERT'") : "Should contain INSERT condition";
    assert postgreSql.contains("'UPDATE'") : "Should contain UPDATE condition";
    assert postgreSql.contains("INSERT INTO TEST_SCHEMA.AUDIT_TABLE") : "Should contain audit insert";
    assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
  }
}