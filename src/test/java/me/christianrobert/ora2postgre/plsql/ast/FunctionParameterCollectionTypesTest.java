package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class FunctionParameterCollectionTypesTest {

  @Test
  public void testFunctionWithLocalCollectionParameterTypes() {
    // Test Oracle function with collection parameters using function-local types
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION process_names(input_names string_array, input_numbers number_table) 
    return number
  is 
    TYPE string_array IS VARRAY(100) OF VARCHAR2(200);
    TYPE number_table IS TABLE OF NUMBER;
    result_count number := 0;
  begin 
    result_count := input_names.COUNT + input_numbers.COUNT;
    return result_count;
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

    // Debug: Print the generated output
    System.out.println("Generated PostgreSQL:");
    System.out.println(postgreSql);

    // Validation - should contain collection parameter types
    assert postgreSql.contains("input_names") : "Should contain first parameter name";
    assert postgreSql.contains("input_numbers") : "Should contain second parameter name";
    // For function-local types, we expect TEXT[] and NUMERIC[] directly
    // (though we may need to enhance this based on what we see)
  }

  @Test
  public void testFunctionWithPackageCollectionParameterTypes() {
    // Test Oracle function with collection parameters using package-level types
    String oracleSql = """
CREATE PACKAGE TEST_SCHEMA.TESTPACKAGE is  
  TYPE global_string_array IS VARRAY(100) OF VARCHAR2(200);
  TYPE global_number_table IS TABLE OF NUMBER;
  
  FUNCTION process_global_data(input_names global_string_array, input_numbers global_number_table) 
    return number;
end;
/

CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION process_global_data(input_names global_string_array, input_numbers global_number_table) 
    return number
  is 
    result_count number := 0;
  begin 
    result_count := input_names.COUNT + input_numbers.COUNT;
    return result_count;
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

    // Debug: Print the generated output
    System.out.println("Generated PostgreSQL:");
    System.out.println(postgreSql);

    // Validation - should contain collection parameter types
    assert postgreSql.contains("input_names") : "Should contain first parameter name";
    assert postgreSql.contains("input_numbers") : "Should contain second parameter name";
    // For package-level types, we expect DOMAIN references like test_schema_testpackage_global_string_array
  }

  @Test
  public void testFunctionWithCollectionReturnType() {
    // Test Oracle function that returns a collection type
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION generate_names(count_param number) 
    return string_array
  is 
    TYPE string_array IS VARRAY(100) OF VARCHAR2(200);
    result_names string_array := string_array();
  begin 
    result_names.EXTEND(count_param);
    for i in 1..count_param loop
      result_names(i) := 'Name_' || i;
    end loop;
    return result_names;
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

    // Debug: Print the generated output
    System.out.println("Generated PostgreSQL:");
    System.out.println(postgreSql);

    // Validation - should contain collection return type
    assert postgreSql.contains("RETURNS") : "Should contain RETURNS keyword";
    // For function-local types, we expect TEXT[] as return type
  }

  @Test
  public void testDebugFunctionParameterParsing() {
    // Debug test to see how function parameters are currently parsed
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION test_params(simple_param varchar2, array_param string_array) 
    return number
  is 
    TYPE string_array IS VARRAY(100) OF VARCHAR2(200);
  begin 
    return array_param.COUNT;
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
        System.out.println("Function name: " + func.getName());
        System.out.println("Return type: " + func.getReturnType());
        System.out.println("Parameters count: " + func.getParameters().size());

        for (int i = 0; i < func.getParameters().size(); i++) {
          Parameter param = func.getParameters().get(i);
          System.out.println("Parameter " + i + ": " + param.getName() + " - " + param.getDataType());
        }

        // Convert to PostgreSQL
        String postgreSql = func.toPostgre(data, false);
        System.out.println("Generated PostgreSQL:");
        System.out.println(postgreSql);
      }
    }
  }
}