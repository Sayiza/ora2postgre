package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class InsertStatementTest {

    @Test
    public void testSimpleInsertStatementToPostgre() {
        // Test Oracle function with simple INSERT statement
        String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION insertaudit( pId number, pAction varchar2 ) 
    return varchar2
  is 
  begin 
    insert into audit_table values (pId, pAction, sysdate);
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

        // Basic validation - should contain key PostgreSQL INSERT elements
        assert postgreSql.contains("INSERT INTO") : "Should contain INSERT INTO keyword";
        assert postgreSql.contains("AUDIT_TABLE") : "Should contain table name";
        assert postgreSql.contains("VALUES") : "Should contain VALUES keyword";
        assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
    }

    @Test
    public void testInsertWithColumnListToPostgre() {
        // Test Oracle function with INSERT statement specifying columns
        String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION insertauditcols( pId number, pAction varchar2 ) 
    return varchar2
  is 
  begin 
    insert into audit_table (id, action, created_at) values (pId, pAction, sysdate);
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

        // Basic validation - should contain key PostgreSQL INSERT elements
        assert postgreSql.contains("INSERT INTO") : "Should contain INSERT INTO keyword";
        assert postgreSql.contains("TEST_SCHEMA.AUDIT_TABLE") : "Should contain table name";
        assert postgreSql.contains("(id, action, created_at)") : "Should contain column list";
        assert postgreSql.contains("VALUES") : "Should contain VALUES keyword";
        assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
    }

    @Test
    public void testInsertWithSchemaToPostgre() {
        // Test Oracle function with INSERT statement using schema prefix
        String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION insertauditschema( pId number, pAction varchar2 ) 
    return varchar2
  is 
  begin 
    insert into TEST_SCHEMA.audit_table values (pId, pAction, sysdate);
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

        // Basic validation - should contain key PostgreSQL INSERT elements
        assert postgreSql.contains("INSERT INTO") : "Should contain INSERT INTO keyword";
        assert postgreSql.contains("TEST_SCHEMA.AUDIT_TABLE") : "Should contain schema and table name in lowercase";
        assert postgreSql.contains("VALUES") : "Should contain VALUES keyword";
    }

    @Test
    public void testTriggerLikeInsertWithIfToPostgre() {
        // Test Oracle function with INSERT inside IF statement (trigger-like pattern)
        String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION triggerfunc( pAction varchar2, pId number, pName varchar2 ) 
    return varchar2
  is 
  begin 
    if pAction = 'INSERT' then
      insert into audit_table (action, table_id, table_name, timestamp) 
      values ('INSERT', pId, pName, sysdate);
    elsif pAction = 'UPDATE' then
      insert into audit_table (action, table_id, table_name, timestamp) 
      values ('UPDATE', pId, pName, sysdate);
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

        // Basic validation - should contain combination of IF and INSERT
        assert postgreSql.contains("IF") : "Should contain IF keyword";
        assert postgreSql.contains("ELSIF") : "Should contain ELSIF keyword";
        assert postgreSql.contains("INSERT INTO") : "Should contain INSERT INTO keyword";
        assert postgreSql.contains("AUDIT_TABLE") : "Should contain table name";
        assert postgreSql.contains("VALUES") : "Should contain VALUES keyword";
        assert postgreSql.contains("'INSERT'") : "Should contain INSERT action";
        assert postgreSql.contains("'UPDATE'") : "Should contain UPDATE action";
        assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
    }

    @Test
    public void testInsertStatementDebug() {
        // Debug test to see what's actually being generated
        String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION debuginsert( pId number ) 
    return varchar2
  is 
  begin 
    insert into audit_table values (pId, 'TEST', sysdate);
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

        if (ast instanceof OraclePackage) {
            OraclePackage pkg = (OraclePackage) ast;
            if (!pkg.getFunctions().isEmpty()) {
                Function func = pkg.getFunctions().get(0);
                System.out.println("Function statements count: " + func.getStatements().size());
                
                for (int i = 0; i < func.getStatements().size(); i++) {
                    Statement stmt = func.getStatements().get(i);
                    System.out.println("Statement " + i + ": " + stmt.getClass().getSimpleName() + " - " + stmt.toString());
                }
                
                // Convert to PostgreSQL
                String postgreSql = func.toPostgre(data, false);
                System.out.println("Generated PostgreSQL:");
                System.out.println(postgreSql);
            }
        }
    }
}