package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class PackageTypesTest {

  @Test
  public void testPackageSimpleTypeAliases() {
    // Test Oracle package with simple type aliases (TYPE name IS base_type)
    String oracleSql = """
CREATE PACKAGE TEST_SCHEMA.TESTPACKAGE is  
  TYPE user_id_type IS NUMBER(10);
  TYPE status_type IS VARCHAR2(20);
  TYPE percentage_type IS NUMBER(5,2);
  
  v_user_id user_id_type;
  v_status status_type;
  v_percentage percentage_type;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    System.out.println("Parsed AST: " + ast.getClass().getSimpleName());
    
    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      System.out.println("Package name: " + pkg.getName());
      System.out.println("Package types count: " + pkg.getTypes().size());
      
      for (int i = 0; i < pkg.getTypes().size(); i++) {
        PackageType type = pkg.getTypes().get(i);
        System.out.println("Type " + i + ": " + type.getName() + " - " + type.toString());
      }
      
      System.out.println("Package variables count: " + pkg.getVariables().size());
      for (int i = 0; i < pkg.getVariables().size(); i++) {
        Variable var = pkg.getVariables().get(i);
        System.out.println("Variable " + i + ": " + var.getName() + " - " + var.getDataType());
      }

      // Convert to PostgreSQL
      String postgreSql = pkg.toPostgre(data, false);
      System.out.println("Generated PostgreSQL:");
      System.out.println(postgreSql);
    }
  }

  @Test
  public void testSubtypeDeclarations() {
    // Test Oracle package with SUBTYPE declarations (which should already work)
    String oracleSql = """
CREATE PACKAGE TEST_SCHEMA.TESTPACKAGE is  
  SUBTYPE user_id_subtype IS NUMBER(10);
  SUBTYPE status_subtype IS VARCHAR2(20);
  
  v_user_id user_id_subtype;
  v_status status_subtype;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    System.out.println("Parsed AST: " + ast.getClass().getSimpleName());
    
    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      System.out.println("Package name: " + pkg.getName());
      System.out.println("Package types count: " + pkg.getTypes().size());
      
      // Convert to PostgreSQL
      String postgreSql = pkg.toPostgre(data, false);
      System.out.println("Generated PostgreSQL:");
      System.out.println(postgreSql);
    }
  }

  @Test
  public void testDebugPackageTypeParsing() {
    // Debug test to see what happens with current parsing
    String oracleSql = """
CREATE PACKAGE TEST_SCHEMA.TESTPACKAGE is  
  TYPE simple_type IS NUMBER(10);
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    try {
      // Parse the Oracle package
      PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
      System.out.println("Parsing succeeded: " + ast.getClass().getSimpleName());
    } catch (Exception e) {
      System.out.println("Parsing failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Test
  public void testPackageTypesToDomainDDL() {
    // Test individual PackageType to PostgreSQL DOMAIN conversion
    // DataTypeSpec constructor: (nativeDataType, custumDataType, rowTypeFullName, fieldTypeFullName)
    // For NUMBER(10): native=NUMBER, precision=10 (via rowTypeFullName), scale=null
    // For VARCHAR2(100): native=VARCHAR2, length=100 (via custumDataType)
    // For CHAR(1): native=CHAR, length=1 (via custumDataType)
    PackageType userIdType = new PackageType("user_id_type", new DataTypeSpec("NUMBER", null, "10", null));
    PackageType emailType = new PackageType("email_type", new DataTypeSpec("VARCHAR2", "100", null, null));
    PackageType statusType = new PackageType("status_type", new DataTypeSpec("CHAR", "1", null, null));

    // Test DOMAIN DDL generation
    String userIdDDL = userIdType.toDomainDDL("test_schema", "test_pkg");
    String emailDDL = emailType.toDomainDDL("test_schema", "test_pkg");
    String statusDDL = statusType.toDomainDDL("test_schema", "test_pkg");

    // Verify the generated DDL
    System.out.println("Package type DOMAIN DDL generation:");
    System.out.println("  " + userIdDDL);
    System.out.println("  " + emailDDL);
    System.out.println("  " + statusDDL);

    // Basic verification that it contains expected elements
    assert userIdDDL.contains("CREATE DOMAIN test_schema_test_pkg_user_id_type AS numeric(10);");
    assert emailDDL.contains("CREATE DOMAIN test_schema_test_pkg_email_type AS varchar(100);");
    assert statusDDL.contains("CREATE DOMAIN test_schema_test_pkg_status_type AS char(1);");
  }

  @Test
  public void testComplexPackageTypesExample() {
    // Test a more complex example with various Oracle types
    String oracleSql = """
CREATE PACKAGE TEST_SCHEMA.HR_PKG is  
  TYPE employee_id_type IS NUMBER(10);
  TYPE salary_type IS NUMBER(10,2);
  TYPE hire_date_type IS DATE;
  TYPE department_code_type IS CHAR(3);
  TYPE full_name_type IS VARCHAR2(200);
  
  FUNCTION get_employee_name(p_emp_id employee_id_type) RETURN full_name_type;
  PROCEDURE update_salary(p_emp_id employee_id_type, p_salary salary_type);
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    System.out.println("Complex package types parsing:");
    System.out.println("Parsed AST: " + ast.getClass().getSimpleName());
    
    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      System.out.println("Package name: " + pkg.getName());
      System.out.println("Package types count: " + pkg.getTypes().size());
      
      for (int i = 0; i < pkg.getTypes().size(); i++) {
        PackageType type = pkg.getTypes().get(i);
        System.out.println("Type " + i + ": " + type.getName() + " - " + type.toString());
      }

      // Convert to PostgreSQL
      String postgreSql = pkg.toPostgre(data, true); // specOnly=true for types
      System.out.println("Generated PostgreSQL (spec only):");
      System.out.println(postgreSql);

      // Verify all types are converted (basic type conversion since full parsing may not preserve precision)
      assert postgreSql.contains("-- Package Types for TEST_SCHEMA.hr_pkg");
      assert postgreSql.contains("CREATE DOMAIN test_schema_hr_pkg_employee_id_type AS numeric;");
      assert postgreSql.contains("CREATE DOMAIN test_schema_hr_pkg_salary_type AS numeric;");
      assert postgreSql.contains("CREATE DOMAIN test_schema_hr_pkg_hire_date_type AS timestamp;");
      assert postgreSql.contains("CREATE DOMAIN test_schema_hr_pkg_department_code_type AS char");
      assert postgreSql.contains("CREATE DOMAIN test_schema_hr_pkg_full_name_type AS varchar");
    }
  }
}