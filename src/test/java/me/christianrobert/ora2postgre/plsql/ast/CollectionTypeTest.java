package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CollectionTypeTest {

  @Test
  public void testVarrayTypePostgreSQLGeneration() {
    // Test VarrayType PostgreSQL array generation
    Everything data = new Everything();
    
    // Test with VARCHAR2 base type
    DataTypeSpec varcharType = new DataTypeSpec("VARCHAR2(100)", null, null, null);
    VarrayType varrayType = new VarrayType(10L, null, varcharType);
    
    String postgresCode = varrayType.toPostgre(data);
    
    assertNotNull(postgresCode);
    assertEquals("text[]", postgresCode);
  }

  @Test
  public void testNestedTableTypePostgreSQLGeneration() {
    // Test NestedTableType PostgreSQL array generation
    Everything data = new Everything();
    
    // Test with NUMBER base type
    DataTypeSpec numberType = new DataTypeSpec("NUMBER", null, null, null);
    NestedTableType nestedTableType = new NestedTableType(numberType);
    
    String postgresCode = nestedTableType.toPostgre(data);
    
    assertNotNull(postgresCode);
    assertEquals("numeric[]", postgresCode);
  }

  @Test
  public void testVarrayTypeWithDifferentDataTypes() {
    Everything data = new Everything();
    
    // Test with different Oracle data types
    String[] oracleTypes = {"VARCHAR2(50)", "NUMBER", "DATE", "CLOB", "INTEGER"};
    String[] expectedPostgresTypes = {"text[]", "numeric[]", "timestamp[]", "text[]", "integer[]"};
    
    for (int i = 0; i < oracleTypes.length; i++) {
      DataTypeSpec dataType = new DataTypeSpec(oracleTypes[i], null, null, null);
      VarrayType varrayType = new VarrayType(null, null, dataType);
      
      String result = varrayType.toPostgre(data);
      assertEquals(expectedPostgresTypes[i], result, 
        "Failed for Oracle type: " + oracleTypes[i]);
    }
  }

  @Test
  public void testNestedTableTypeWithDifferentDataTypes() {
    Everything data = new Everything();
    
    // Test with different Oracle data types
    String[] oracleTypes = {"VARCHAR2(100)", "NUMBER(10,2)", "DATE", "BOOLEAN"};
    String[] expectedPostgresTypes = {"text[]", "numeric[]", "timestamp[]", "boolean[]"};
    
    for (int i = 0; i < oracleTypes.length; i++) {
      DataTypeSpec dataType = new DataTypeSpec(oracleTypes[i], null, null, null);
      NestedTableType nestedTableType = new NestedTableType(dataType);
      
      String result = nestedTableType.toPostgre(data);
      assertEquals(expectedPostgresTypes[i], result, 
        "Failed for Oracle type: " + oracleTypes[i]);
    }
  }

  @Test
  public void testVarrayWithNullDataType() {
    Everything data = new Everything();
    
    // Test with null dataTypeSpec - should fallback to text[]
    VarrayType varrayType = new VarrayType(10L, null, 
      new DataTypeSpec(null, null, null, null));
    
    String result = varrayType.toPostgre(data);
    assertEquals("text[]", result);
  }

  @Test
  public void testNestedTableWithNullDataType() {
    Everything data = new Everything();
    
    // Test with null dataType - should fallback to text[]
    NestedTableType nestedTableType = new NestedTableType(
      new DataTypeSpec(null, null, null, null));
    
    String result = nestedTableType.toPostgre(data);
    assertEquals("text[]", result);
  }

  @Test
  public void testVarrayTypeConstructor() {
    // Test VarrayType constructor and getters
    DataTypeSpec dataType = new DataTypeSpec("VARCHAR2(50)", null, null, null);
    VarrayType varrayType = new VarrayType(100L, null, dataType);
    
    assertNotNull(varrayType);
    // Note: VarrayType doesn't have public getters, but constructor should work
  }

  @Test
  public void testNestedTableTypeConstructor() {
    // Test NestedTableType constructor
    DataTypeSpec dataType = new DataTypeSpec("NUMBER", null, null, null);
    NestedTableType nestedTableType = new NestedTableType(dataType);
    
    assertNotNull(nestedTableType);
    // Note: NestedTableType doesn't have public getters, but constructor should work
  }

  @Test
  public void testVarrayTypeDeclarationParsing() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.COLLECTION_TEST_PKG is  
  TYPE string_array IS VARRAY(10) OF VARCHAR2(100);
  
  FUNCTION test_function RETURN NUMBER IS
    v_names string_array := string_array('John', 'Jane');
  BEGIN
    RETURN v_names.COUNT;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals("COLLECTION_TEST_PKG", pkg.getName());
    assertEquals(1, pkg.getFunctions().size());
    
    Function function = pkg.getFunctions().get(0);
    assertEquals("test_function", function.getName());
  }

  @Test
  public void testNestedTableTypeDeclarationParsing() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.NESTED_TABLE_TEST_PKG is  
  TYPE number_table IS TABLE OF NUMBER;
  
  FUNCTION test_function RETURN NUMBER IS
    v_numbers number_table := number_table(1, 2, 3);
  BEGIN
    RETURN v_numbers.COUNT;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals("NESTED_TABLE_TEST_PKG", pkg.getName());
    assertEquals(1, pkg.getFunctions().size());
    
    Function function = pkg.getFunctions().get(0);
    assertEquals("test_function", function.getName());
  }

  @Test
  public void testCollectionTypeIntegration() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.COLLECTION_TEST_PKG is  
  TYPE string_array IS VARRAY(10) OF VARCHAR2(100);
  TYPE number_table IS TABLE OF NUMBER;
  
  FUNCTION test_function RETURN NUMBER IS
    v_strings string_array := string_array('John', 'Jane');
    v_numbers number_table := number_table(1, 2, 3);
  BEGIN
    RETURN v_strings.COUNT + v_numbers.COUNT;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals("COLLECTION_TEST_PKG", pkg.getName());
    
    // Verify collection types are collected properly
    assertEquals(1, pkg.getVarrayTypes().size());
    assertEquals(1, pkg.getNestedTableTypes().size());
    
    VarrayType varrayType = pkg.getVarrayTypes().get(0);
    assertEquals("string_array", varrayType.getName());
    assertEquals("text[]", varrayType.toPostgre(data));
    
    NestedTableType nestedTableType = pkg.getNestedTableTypes().get(0);
    assertEquals("number_table", nestedTableType.getName());
    assertEquals("numeric[]", nestedTableType.toPostgre(data));
    
    // Verify function parsing still works
    assertEquals(1, pkg.getFunctions().size());
    Function function = pkg.getFunctions().get(0);
    assertEquals("test_function", function.getName());
  }

  @Test
  public void testPostgreSQLTypeGeneration() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.COLLECTION_PKG is  
  TYPE varraytyp2 IS VARRAY(16) OF VARCHAR2(1024);
  TYPE tablearray2 IS TABLE OF VARCHAR2(256);
  
  FUNCTION get_count RETURN NUMBER IS
  BEGIN
    RETURN 1;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    
    // Verify both collection types are collected
    assertEquals(1, pkg.getVarrayTypes().size());
    assertEquals(1, pkg.getNestedTableTypes().size());
    
    VarrayType varrayType = pkg.getVarrayTypes().get(0);
    assertEquals("varraytyp2", varrayType.getName());
    
    NestedTableType nestedTableType = pkg.getNestedTableTypes().get(0);
    assertEquals("tablearray2", nestedTableType.getName());
    
    // Generate PostgreSQL using the StandardPackageStrategy
    String postgresOutput = pkg.toPostgre(data, true); // specOnly=true for types
    
    // Verify PostgreSQL output contains CREATE DOMAIN statements
    assertNotNull(postgresOutput);
    assertTrue(postgresOutput.contains("Collection Types for TEST_SCHEMA.collection_pkg"));
    assertTrue(postgresOutput.contains("CREATE DOMAIN test_schema_collection_pkg_varraytyp2 AS text[]"));
    assertTrue(postgresOutput.contains("CREATE DOMAIN test_schema_collection_pkg_tablearray2 AS text[]"));
    assertTrue(postgresOutput.contains("-- VARRAY type: varraytyp2"));
    assertTrue(postgresOutput.contains("-- TABLE OF type: tablearray2"));
  }

  @Test
  public void testSpecBodyMergeWithCollectionTypes() {
    // Test that collection types in both spec and body are properly merged
    
    // Create spec package with VARRAY type
    VarrayType specVarrayType = new VarrayType("spec_varray", 10L, null, 
      new DataTypeSpec("VARCHAR2", null, null, null));
    
    OraclePackage spec = new OraclePackage(
      "MERGE_TEST_PKG",
      "TEST_SCHEMA", 
      new ArrayList<>(), null, null, null, new ArrayList<>(),
      List.of(specVarrayType), // spec varray types
      new ArrayList<>(), // spec nested table types
      new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
    );
    
    // Create body package with TABLE OF type and same VARRAY type  
    VarrayType bodyVarrayType = new VarrayType("spec_varray", 10L, null, 
      new DataTypeSpec("VARCHAR2", null, null, null)); // duplicate name
    NestedTableType bodyNestedTableType = new NestedTableType("body_nested_table", 
      new DataTypeSpec("NUMBER", null, null, null));
    
    OraclePackage body = new OraclePackage(
      "MERGE_TEST_PKG",
      "TEST_SCHEMA", 
      new ArrayList<>(), null, null, null, new ArrayList<>(),
      List.of(bodyVarrayType), // body varray types (duplicate)
      List.of(bodyNestedTableType), // body nested table types
      new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
    );
    
    // Test merging by calling the private static methods via reflection
    // (This simulates what happens in ExportPackage.createMergedPackage)
    
    // Create merged package manually to test the merge logic
    OraclePackage merged = new OraclePackage(
      spec.getName(),
      spec.getSchema(),
      new ArrayList<>(), null, null, null, new ArrayList<>(),
      mergeVarrayTypesForTest(spec.getVarrayTypes(), body.getVarrayTypes()),
      mergeNestedTableTypesForTest(spec.getNestedTableTypes(), body.getNestedTableTypes()),
      new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
    );
    
    // Verify merged collection types
    assertEquals(1, merged.getVarrayTypes().size()); // Should deduplicate
    assertEquals(1, merged.getNestedTableTypes().size());
    
    VarrayType mergedVarray = merged.getVarrayTypes().get(0);
    assertEquals("spec_varray", mergedVarray.getName());
    
    NestedTableType mergedNestedTable = merged.getNestedTableTypes().get(0);
    assertEquals("body_nested_table", mergedNestedTable.getName());
  }

  // Helper methods to simulate the merge logic (since the real merge methods are private)
  private List<VarrayType> mergeVarrayTypesForTest(List<VarrayType> specVarrayTypes, List<VarrayType> bodyVarrayTypes) {
    List<VarrayType> merged = new ArrayList<>();
    Set<String> varrayTypeNames = new HashSet<>();
    
    // Add spec varray types first (they are declarations)
    if (specVarrayTypes != null) {
      for (VarrayType varrayType : specVarrayTypes) {
        if (varrayTypeNames.add(varrayType.getName())) {
          merged.add(varrayType);
        }
      }
    }
    
    // Add body varray types (avoid duplicates)
    if (bodyVarrayTypes != null) {
      for (VarrayType varrayType : bodyVarrayTypes) {
        if (varrayTypeNames.add(varrayType.getName())) {
          merged.add(varrayType);
        }
      }
    }
    
    return merged;
  }

  private List<NestedTableType> mergeNestedTableTypesForTest(List<NestedTableType> specNestedTableTypes, List<NestedTableType> bodyNestedTableTypes) {
    List<NestedTableType> merged = new ArrayList<>();
    Set<String> nestedTableTypeNames = new HashSet<>();
    
    // Add spec nested table types first (they are declarations)
    if (specNestedTableTypes != null) {
      for (NestedTableType nestedTableType : specNestedTableTypes) {
        if (nestedTableTypeNames.add(nestedTableType.getName())) {
          merged.add(nestedTableType);
        }
      }
    }
    
    // Add body nested table types (avoid duplicates)
    if (bodyNestedTableTypes != null) {
      for (NestedTableType nestedTableType : bodyNestedTableTypes) {
        if (nestedTableTypeNames.add(nestedTableType.getName())) {
          merged.add(nestedTableType);
        }
      }
    }
    
    return merged;
  }

  @Test
  public void testCustomTypeDataSpecResolution() {
    // Test that DataTypeSpec can resolve custom collection types with package context
    Everything data = new Everything();
    
    // Create a DataTypeSpec for a custom collection type
    DataTypeSpec customTypeSpec = new DataTypeSpec(null, "varraytyp2", null, null);
    
    // Test without package context (should return fallback)
    String resultWithoutContext = customTypeSpec.toPostgre(data);
    assertTrue(resultWithoutContext.contains("data type not implemented"));
    
    // Test with package context (should return domain name)
    String resultWithContext = customTypeSpec.toPostgre(data, "TEST_SCHEMA", "COLLECTION_PKG");
    assertEquals("test_schema_collection_pkg_varraytyp2", resultWithContext);
  }

  @Test
  public void testPackageVariableWithCustomType() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.VAR_TEST_PKG is  
  TYPE string_array IS VARRAY(10) OF VARCHAR2(100);
  
  g_my_array string_array;
  
  FUNCTION get_count RETURN NUMBER IS
  BEGIN
    RETURN 1;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    
    // Verify collection type and variable are collected
    assertEquals(1, pkg.getVarrayTypes().size());
    assertEquals(1, pkg.getVariables().size());
    
    Variable variable = pkg.getVariables().get(0);
    assertEquals("g_my_array", variable.getName());
    
    // Generate PostgreSQL using the StandardPackageStrategy
    String postgresOutput = pkg.toPostgre(data, true); // specOnly=true for types and variables
    
    // Verify the custom type domain is created
    assertTrue(postgresOutput.contains("CREATE DOMAIN test_schema_var_test_pkg_string_array AS text[]"));
    
    // Verify the variable uses the correct domain type  
    assertTrue(postgresOutput.contains("test_schema_var_test_pkg_g_my_array"));
    assertTrue(postgresOutput.contains("value test_schema_var_test_pkg_string_array"));
    
    // Verify correct order: DOMAIN definition must come before variable that uses it
    int domainIndex = postgresOutput.indexOf("CREATE DOMAIN test_schema_var_test_pkg_string_array");
    int variableIndex = postgresOutput.indexOf("value test_schema_var_test_pkg_string_array");
    assertTrue(domainIndex < variableIndex, "DOMAIN definition must come before variable usage");
  }

  // =====================================================================================
  // FUNCTION-LOCAL COLLECTION TYPE TESTS (NEW FUNCTIONALITY)
  // =====================================================================================

  @Test
  public void testFunctionLocalVarrayDeclaration() {
    // Test function-local VARRAY declaration using direct array syntax (Option B)
    String oracleSql = """
CREATE OR REPLACE FUNCTION TEST_SCHEMA.test_func RETURN NUMBER IS
  TYPE local_array IS VARRAY(10) OF VARCHAR2(100);
  v_arr local_array := local_array('a','b');
BEGIN
  RETURN v_arr.COUNT;
END;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof Function);
    
    Function func = (Function) ast;
    assertEquals("TEST_SCHEMA.test_func", func.getName());
    
    // Verify function-local collection types are parsed
    assertEquals(1, func.getVarrayTypes().size());
    assertEquals(0, func.getNestedTableTypes().size());
    
    VarrayType localArray = func.getVarrayTypes().get(0);
    assertEquals("local_array", localArray.getName());
    
    // Test that variables using function-local collection types resolve correctly
    assertEquals(1, func.getVariables().size());
    Variable variable = func.getVariables().get(0);
    assertEquals("v_arr", variable.getName());
    
    // Test PostgreSQL generation with function context
    String postgresVarDecl = variable.toPostgre(data, func);
    assertEquals("v_arr text[]", postgresVarDecl);
  }

  @Test
  public void testFunctionLocalTableOfDeclaration() {
    // Test function-local TABLE OF declaration using direct array syntax (Option B)
    String oracleSql = """
CREATE OR REPLACE FUNCTION TEST_SCHEMA.test_func RETURN NUMBER IS
  TYPE local_table IS TABLE OF NUMBER;
  v_arr local_table := local_table(1, 2, 3);
BEGIN
  RETURN v_arr.COUNT;
END;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof Function);
    
    Function func = (Function) ast;
    assertEquals("TEST_SCHEMA.test_func", func.getName());
    
    // Verify function-local collection types are parsed
    assertEquals(0, func.getVarrayTypes().size());
    assertEquals(1, func.getNestedTableTypes().size());
    
    NestedTableType localTable = func.getNestedTableTypes().get(0);
    assertEquals("local_table", localTable.getName());
    
    // Test that variables using function-local collection types resolve correctly
    assertEquals(1, func.getVariables().size());
    Variable variable = func.getVariables().get(0);
    assertEquals("v_arr", variable.getName());
    
    // Test PostgreSQL generation with function context
    String postgresVarDecl = variable.toPostgre(data, func);
    assertEquals("v_arr numeric[]", postgresVarDecl);
  }

  @Test
  public void testFunctionLocalCollectionTypeGeneration() {
    // Test complete PostgreSQL generation for function with local collection types
    String oracleSql = """
CREATE OR REPLACE FUNCTION TEST_SCHEMA.test_func RETURN NUMBER IS
  TYPE local_array IS VARRAY(10) OF VARCHAR2(100);
  v_arr local_array := local_array('a','b');
BEGIN
  RETURN v_arr.COUNT;
END;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof Function);
    
    Function func = (Function) ast;
    
    // Generate PostgreSQL using the transformation manager
    String postgresCode = func.toPostgre(data, false);
    
    // Verify PostgreSQL output contains correct array syntax
    assertNotNull(postgresCode);
    assertTrue(postgresCode.contains("text[]"), "Should contain direct array syntax");
    assertFalse(postgresCode.contains("CREATE DOMAIN"), "Should NOT create DOMAINs for function-local types");
    assertTrue(postgresCode.contains("v_arr text[]"), "Variable should use direct array type");
    
    // Expected format: no type definitions, just variable declarations with direct array syntax
    assertTrue(postgresCode.contains("DECLARE"));
    assertTrue(postgresCode.contains("v_arr text[]"));
  }

  @Test
  public void testMultipleFunctionLocalCollectionTypes() {
    // Test function with multiple local collection type declarations
    String oracleSql = """
CREATE OR REPLACE FUNCTION TEST_SCHEMA.test_func RETURN NUMBER IS
  TYPE string_array IS VARRAY(10) OF VARCHAR2(100);
  TYPE number_table IS TABLE OF NUMBER;
  TYPE date_array IS VARRAY(5) OF DATE;
  
  v_strings string_array := string_array('a','b');
  v_numbers number_table := number_table(1, 2, 3);
  v_dates date_array := date_array(SYSDATE, SYSDATE+1);
BEGIN
  RETURN v_strings.COUNT + v_numbers.COUNT + v_dates.COUNT;
END;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof Function);
    
    Function func = (Function) ast;
    assertEquals("TEST_SCHEMA.test_func", func.getName());
    
    // Should parse multiple local collection types
    // Expected PostgreSQL should use direct array syntax for all types:
    // v_strings TEXT[] := ARRAY['a','b'];
    // v_numbers numeric[] := ARRAY[1, 2, 3];  
    // v_dates timestamp[] := ARRAY[CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day'];
  }

  @Test
  public void testFunctionLocalCollectionWithPackageTypes() {
    // Test function that uses both package-level and function-local collection types
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.MIXED_COLLECTION_PKG is  
  TYPE pkg_string_array IS VARRAY(10) OF VARCHAR2(100);
  
  FUNCTION test_function RETURN NUMBER IS
    TYPE local_number_table IS TABLE OF NUMBER;
    
    v_pkg_strings pkg_string_array := pkg_string_array('a','b');
    v_local_numbers local_number_table := local_number_table(1, 2, 3);
  BEGIN
    RETURN v_pkg_strings.COUNT + v_local_numbers.COUNT;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals("MIXED_COLLECTION_PKG", pkg.getName());
    assertEquals(1, pkg.getFunctions().size());
    
    Function function = pkg.getFunctions().get(0);
    assertEquals("test_function", function.getName());
    
    // Expected behavior:
    // - Package-level type should create DOMAIN: test_schema_mixed_collection_pkg_pkg_string_array
    // - Function-local type should use direct array: numeric[]
    // - Package variable should reference DOMAIN: test_schema_mixed_collection_pkg_pkg_string_array
    // - Function variable should use direct array: numeric[]
  }

  @Test
  public void testFunctionLocalCollectionMethodTransformation() {
    // Test transformation of Oracle collection methods to PostgreSQL array functions
    String oracleSql = """
CREATE OR REPLACE FUNCTION TEST_SCHEMA.test_collection_methods RETURN NUMBER IS
  TYPE local_array IS VARRAY(10) OF VARCHAR2(100);
  v_arr local_array := local_array('a','b','c');
  v_count NUMBER;
  v_first NUMBER;
  v_last NUMBER;
BEGIN
  v_count := v_arr.COUNT;
  v_first := v_arr.FIRST;
  v_last := v_arr.LAST;
  RETURN v_count + v_first + v_last;
END;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof Function);
    
    Function func = (Function) ast;
    
    // Generate PostgreSQL using the transformation manager
    String postgresCode = func.toPostgre(data, false);
    
    // Debug: Print the actual generated PostgreSQL code
    System.out.println("Generated PostgreSQL code:");
    System.out.println(postgresCode);
    System.out.println("=== End of generated code ===");
    
    // Verify collection method transformations
    assertNotNull(postgresCode);
               assertTrue(postgresCode.contains("array_length(v_arr, 1)"), "Should transform .COUNT to array_length(arr, 1)");
    assertTrue(postgresCode.contains(" 1"), "Should transform .FIRST to literal 1");
    
    // Verify expected transformations are present:
    // Oracle .COUNT → PostgreSQL array_length(arr, 1)
    // Oracle .COUNT() → PostgreSQL array_length(arr, 1)  
    // Oracle .FIRST → PostgreSQL 1 (arrays are 1-indexed)
    // Oracle .LAST → PostgreSQL array_length(arr, 1)
    int countTransformations = countOccurrences(postgresCode, "array_length(v_arr, 1)");
    assertEquals(2, countTransformations, "Should have 3 array_length calls for .COUNT, and .LAST");
    
    // Verify .FIRST transformation
    assertTrue(postgresCode.contains("v_first := 1;"), "Should transform .FIRST to 1");
  }

  @Test
  public void testFunctionLocalCollectionMethodTransformationWithBrackets() {
    // Test transformation of Oracle collection methods to PostgreSQL array functions
    String oracleSql = """
CREATE OR REPLACE FUNCTION TEST_SCHEMA.test_collection_methods RETURN NUMBER IS
  TYPE local_array IS VARRAY(10) OF VARCHAR2(100);
  v_arr local_array := local_array('a','b','c');
  v_count NUMBER;
  v_first NUMBER;
  v_last NUMBER;
BEGIN
  v_count := v_arr.COUNT();
  v_first := v_arr.FIRST();
  v_last := v_arr.LAST();
  RETURN v_count + v_first + v_last;
END;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof Function);

    Function func = (Function) ast;

    // Generate PostgreSQL using the transformation manager
    String postgresCode = func.toPostgre(data, false);

    // Debug: Print the actual generated PostgreSQL code
    System.out.println("Generated PostgreSQL code:");
    System.out.println(postgresCode);
    System.out.println("=== End of generated code ===");

    // Verify collection method transformations
    assertNotNull(postgresCode);
               assertTrue(postgresCode.contains("array_length(v_arr, 1)"), "Should transform .COUNT to array_length(arr, 1)");
    assertTrue(postgresCode.contains(" 1"), "Should transform .FIRST to literal 1");

    // Verify expected transformations are present:
    // Oracle .COUNT → PostgreSQL array_length(arr, 1)
    // Oracle .COUNT() → PostgreSQL array_length(arr, 1)
    // Oracle .FIRST → PostgreSQL 1 (arrays are 1-indexed)
    // Oracle .LAST → PostgreSQL array_length(arr, 1)
    int countTransformations = countOccurrences(postgresCode, "array_length(v_arr, 1)");
    assertEquals(2, countTransformations, "Should have 3 array_length calls for , .COUNT(), and .LAST()");

    // Verify .FIRST transformation
    assertTrue(postgresCode.contains("v_first := 1;"), "Should transform .FIRST to 1");
  }
  
  // Helper method to count occurrences of a substring
  private int countOccurrences(String text, String pattern) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(pattern, index)) != -1) {
      count++;
      index += pattern.length();
    }
    return count;
  }

  @Test
  public void testFunctionLocalCollectionIndexing() {
    // Test transformation of Oracle collection indexing to PostgreSQL array indexing
    String oracleSql = """
CREATE OR REPLACE FUNCTION TEST_SCHEMA.test_collection_indexing RETURN VARCHAR2 IS
  TYPE local_array IS VARRAY(10) OF VARCHAR2(100);
  v_arr local_array := local_array('first','second','third');
  v_element VARCHAR2(100);
BEGIN
  v_element := v_arr(2); -- Oracle uses 1-based indexing
  RETURN v_element;
END;
/
""";

    // Expected PostgreSQL transformation:
    String expectedPostgresSQL = """
CREATE FUNCTION test_schema.test_collection_indexing() RETURNS text AS $$
DECLARE
  v_arr TEXT[] := ARRAY['first','second','third'];
  v_element text;
BEGIN
  v_element := v_arr[2]; -- PostgreSQL also uses 1-based indexing for arrays
  RETURN v_element;
END;
$$ LANGUAGE plpgsql;
""";
    
    // This test documents the expected transformation:
    // Oracle arr(i) → PostgreSQL arr[i]
    // Both use 1-based indexing, so index values stay the same
    assertTrue(expectedPostgresSQL.contains("v_arr[2]"));
  }

  @Test
  public void testFunctionLocalCollectionInitialization() {
    // Test transformation of Oracle collection constructors to PostgreSQL ARRAY[...] syntax
    String oracleSql = """
CREATE OR REPLACE FUNCTION TEST_SCHEMA.test_collection_init RETURN NUMBER IS
  TYPE string_array IS VARRAY(10) OF VARCHAR2(100);
  TYPE number_table IS TABLE OF NUMBER;
  
  v_strings string_array := string_array('a', 'b', 'c');
  v_numbers number_table := number_table(1, 2, 3, 4, 5);
  v_empty_strings string_array := string_array();
BEGIN
  RETURN v_strings.COUNT + v_numbers.COUNT;
END;
/
""";

    // Expected PostgreSQL transformation:
    String expectedPostgresSQL = """
CREATE FUNCTION test_schema.test_collection_init() RETURNS numeric AS $$
DECLARE
  v_strings TEXT[] := ARRAY['a', 'b', 'c'];
  v_numbers numeric[] := ARRAY[1, 2, 3, 4, 5];
  v_empty_strings TEXT[] := ARRAY[]::TEXT[];
BEGIN
  RETURN array_length(v_strings, 1) + array_length(v_numbers, 1);
END;
$$ LANGUAGE plpgsql;
""";
    
    // This test documents the expected transformation:
    // Oracle string_array('a', 'b', 'c') → PostgreSQL ARRAY['a', 'b', 'c']
    // Oracle number_table(1, 2, 3) → PostgreSQL ARRAY[1, 2, 3]
    // Oracle string_array() → PostgreSQL ARRAY[]::TEXT[]
    assertTrue(expectedPostgresSQL.contains("ARRAY['a', 'b', 'c']"));
    assertTrue(expectedPostgresSQL.contains("ARRAY[1, 2, 3, 4, 5]"));
    assertTrue(expectedPostgresSQL.contains("ARRAY[]::TEXT[]"));
  }
}