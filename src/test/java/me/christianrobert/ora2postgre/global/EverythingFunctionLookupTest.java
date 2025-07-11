package me.christianrobert.ora2postgre.global;

import me.christianrobert.ora2postgre.oracledb.SynonymMetadata;
import me.christianrobert.ora2postgre.plsql.ast.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the function lookup functionality in Everything class.
 * Tests the complex lookUpDataType method with focus on function resolution.
 */
class EverythingFunctionLookupTest {

  private Everything everything;

  @BeforeEach
  void setUp() {
    everything = new Everything();
  }

  /**
   * Test case 1: Direct object type function lookup without synonyms.
   * Tests: objecttype.function() pattern where objecttype exists directly in the schema.
   */
  @Test
  void testDirectObjectTypeFunctionLookup() {
    // Arrange: Create an object type with a function
    ObjectType testObjectType = createTestObjectType("TestSchema", "MyObjectType",
            createTestFunction("getCustomerName", "VARCHAR2"));
    everything.getObjectTypeSpecAst().add(testObjectType);

    // Create a mock expression for "MyObjectType.getCustomerName()"
    Expression mockExpression = createExpression("MyObjectType.getCustomerName()");

    // Act: Look up the data type
    String result = everything.lookUpDataType(mockExpression, "TestSchema", new ArrayList<>());

    // Assert: Should return the function's return type
    assertEquals("VARCHAR2", result, "Should return the function's return type");
  }

  /**
   * Test case 2: Object type function lookup via synonym resolution.
   * Tests: synonym.function() pattern where synonym points to an object type in another schema.
   */
  @Test
  void testSynonymResolvedObjectTypeFunctionLookup() {
    // Arrange: Create an object type in schema1
    ObjectType realObjectType = createTestObjectType("Schema1", "RealObjectType",
            createTestFunction("calculateTotal", "NUMBER"));
    everything.getObjectTypeSpecAst().add(realObjectType);

    // Create a synonym in schema2 pointing to the object type in schema1
    SynonymMetadata synonym = new SynonymMetadata(
            "Schema2", "TypeAlias", "Schema1", "RealObjectType", "TYPE");
    everything.getSynonyms().add(synonym);

    // Create a mock expression for "TypeAlias.calculateTotal()"
    Expression mockExpression = createExpression("TypeAlias.calculateTotal()");

    // Act: Look up the data type from schema2 context
    String result = everything.lookUpDataType(mockExpression, "Schema2", new ArrayList<>());

    // Assert: Should resolve through synonym and return the function's return type
    assertEquals("NUMBER", result, "Should resolve through synonym and return the function's return type");
  }

  /**
   * Test case 3: Package function lookup.
   * Tests: package.function() pattern.
   */
  @Test
  void testPackageFunctionLookup() {
    // Arrange: Create a package with a function
    OraclePackage testPackage = createTestPackage("TestSchema", "UtilPackage",
            createTestFunction("formatDate", "VARCHAR2"));
    everything.getPackageSpecAst().add(testPackage);

    // Create a mock expression for "UtilPackage.formatDate()"
    Expression mockExpression = createExpression("UtilPackage.formatDate()");

    // Act: Look up the data type
    String result = everything.lookUpDataType(mockExpression, "TestSchema", new ArrayList<>());

    // Assert: Should return the function's return type
    assertEquals("VARCHAR2", result, "Should return the package function's return type");
  }

  /**
   * Test case 4: Chained function calls.
   * Tests: obj1.getObj2().getField() pattern.
   */
  @Test
  void testChainedFunctionCalls() {
    // Arrange: Create first object type that returns second object type
    ObjectType customerType = createTestObjectType("TestSchema", "Customer",
            createTestFunction("getAddress", "Address"));
    ObjectType addressType = createTestObjectType("TestSchema", "Address",
            createTestFunction("getStreet", "VARCHAR2"));

    everything.getObjectTypeSpecAst().add(customerType);
    everything.getObjectTypeSpecAst().add(addressType);

    // Create a mock expression for chained call "Customer.getAddress().getStreet()"
    Expression mockExpression = createExpression("Customer.getAddress().getStreet()");

    // Act: Look up the data type
    String result = everything.lookUpDataType(mockExpression, "TestSchema", new ArrayList<>());

    // Assert: Should return the final function's return type
    assertEquals("VARCHAR2", result, "Should return the final function's return type in chain");
  }

  /**
   * Test case 5: Function not found.
   * Tests that non-existent functions return null.
   */
  @Test
  void testFunctionNotFound() {
    // Arrange: Create object type without the requested function
    ObjectType testObjectType = createTestObjectType("TestSchema", "MyObjectType",
            createTestFunction("existingFunction", "VARCHAR2"));
    everything.getObjectTypeSpecAst().add(testObjectType);

    // Create a mock expression for non-existent function
    Expression mockExpression = createExpression("MyObjectType.nonExistentFunction()");

    // Act: Look up the data type
    String result = everything.lookUpDataType(mockExpression, "TestSchema", new ArrayList<>());

    // Assert: Should fall back to default
    assertEquals("varchar2", result, "Should fall back to default when function not found");
  }

  /**
   * Test case 6: Schema-qualified function call.
   * Tests: schema.objecttype.function() pattern.
   */
  @Test
  void testSchemaQualifiedFunctionCall() {
    // Arrange: Create object type in specific schema
    ObjectType testObjectType = createTestObjectType("SpecificSchema", "MyObjectType",
            createTestFunction("getStatus", "VARCHAR2"));
    everything.getObjectTypeSpecAst().add(testObjectType);

    // Create a mock expression for schema-qualified call
    Expression mockExpression = createExpression("SpecificSchema.MyObjectType.getStatus()");

    // Act: Look up the data type from different schema context
    String result = everything.lookUpDataType(mockExpression, "DifferentSchema", new ArrayList<>());

    // Assert: Should find function in specified schema
    assertEquals("VARCHAR2", result, "Should find function in specified schema");
  }

  /**
   * Test case 7: Direct package function lookup without synonyms.
   * Tests: package.function() pattern where package exists directly in the schema.
   */
  @Test
  void testDirectPackageFunctionLookup() {
    // Arrange: Create a package with a function
    OraclePackage testPackage = createTestPackage("TestSchema", "UtilityPackage",
            createTestFunction("formatDate", "VARCHAR2"));
    everything.getPackageSpecAst().add(testPackage);

    // Create expression for "UtilityPackage.formatDate()"
    Expression mockExpression = createExpression("UtilityPackage.formatDate()");

    // Act: Look up the data type
    String result = everything.lookUpDataType(mockExpression, "TestSchema", new ArrayList<>());

    // Assert: Should return the function's return type
    assertEquals("VARCHAR2", result, "Should return the package function's return type");
  }

  /**
   * Test case 8: Package function lookup via synonym resolution.
   * Tests: synonym.function() pattern where synonym points to a package in another schema.
   */
  @Test
  void testSynonymResolvedPackageFunctionLookup() {
    // Arrange: Create a package in schema1
    OraclePackage realPackage = createTestPackage("Schema1", "RealPackage",
            createTestFunction("calculateTax", "NUMBER"));
    everything.getPackageSpecAst().add(realPackage);

    // Create a synonym in schema2 pointing to the package in schema1
    // Note: For packages, we might use "PACKAGE" as the object type
    SynonymMetadata synonym = new SynonymMetadata(
            "Schema2", "PackageAlias", "Schema1", "RealPackage", "PACKAGE");
    everything.getSynonyms().add(synonym);

    // Create expression for "PackageAlias.calculateTax()"
    Expression mockExpression = createExpression("PackageAlias.calculateTax()");

    // Act: Look up the data type from schema2 context
    String result = everything.lookUpDataType(mockExpression, "Schema2", new ArrayList<>());

    // Assert: Should resolve through synonym and return the function's return type
    // Note: This might fail currently since our synonym resolution is only for object types
    // This test documents the expected behavior for package synonyms
    assertEquals("NUMBER", result, "Should resolve package through synonym and return function's return type");
  }

  /**
   * Test case 9: Multiple functions in same package.
   * Tests: package.function() pattern with multiple functions to ensure correct selection.
   */
  @Test
  void testMultipleFunctionsInPackage() {
    // Arrange: Create a package with multiple functions
    Function formatDate = createTestFunction("formatDate", "VARCHAR2");
    Function calculateAge = createTestFunction("calculateAge", "NUMBER");
    Function validateEmail = createTestFunction("validateEmail", "BOOLEAN");

    OraclePackage testPackage = createTestPackageWithMultipleFunctions("TestSchema", "UtilsPackage",
            formatDate, calculateAge, validateEmail);
    everything.getPackageSpecAst().add(testPackage);

    // Test each function
    Expression dateExpr = createExpression("UtilsPackage.formatDate()");
    Expression ageExpr = createExpression("UtilsPackage.calculateAge()");
    Expression emailExpr = createExpression("UtilsPackage.validateEmail()");

    // Act & Assert
    assertEquals("VARCHAR2", everything.lookUpDataType(dateExpr, "TestSchema", new ArrayList<>()),
            "Should return correct type for formatDate function");
    assertEquals("NUMBER", everything.lookUpDataType(ageExpr, "TestSchema", new ArrayList<>()),
            "Should return correct type for calculateAge function");
    assertEquals("BOOLEAN", everything.lookUpDataType(emailExpr, "TestSchema", new ArrayList<>()),
            "Should return correct type for validateEmail function");
  }

  /**
   * Test case 10: Package function not found.
   * Tests: package.nonExistentFunction() returns fallback value.
   */
  @Test
  void testPackageFunctionNotFound() {
    // Arrange: Create package with one function
    OraclePackage testPackage = createTestPackage("TestSchema", "UtilsPackage",
            createTestFunction("existingFunction", "VARCHAR2"));
    everything.getPackageSpecAst().add(testPackage);

    // Create expression for non-existent function
    Expression mockExpression = createExpression("UtilsPackage.nonExistentFunction()");

    // Act: Look up the data type
    String result = everything.lookUpDataType(mockExpression, "TestSchema", new ArrayList<>());

    // Assert: Should fall back to default
    assertEquals("varchar2", result, "Should fall back to default when package function not found");
  }

  /**
   * Test case 11: Package not found.
   * Tests: nonExistentPackage.function() returns fallback value.
   */
  @Test
  void testPackageNotFound() {
    // Arrange: Create one package but reference different one
    OraclePackage testPackage = createTestPackage("TestSchema", "ExistingPackage",
            createTestFunction("someFunction", "VARCHAR2"));
    everything.getPackageSpecAst().add(testPackage);

    // Create expression for function in non-existent package
    Expression mockExpression = createExpression("NonExistentPackage.someFunction()");

    // Act: Look up the data type
    String result = everything.lookUpDataType(mockExpression, "TestSchema", new ArrayList<>());

    // Assert: Should fall back to default
    assertEquals("varchar2", result, "Should fall back to default when package not found");
  }

  /**
   * Test case 12: Schema-qualified package function call.
   * Tests: schema.package.function() pattern.
   */
  @Test
  void testSchemaQualifiedPackageFunctionCall() {
    // Arrange: Create package in specific schema
    OraclePackage testPackage = createTestPackage("SpecificSchema", "MyPackage",
            createTestFunction("getVersion", "VARCHAR2"));
    everything.getPackageSpecAst().add(testPackage);

    // Create expression for schema-qualified call
    Expression mockExpression = createExpression("SpecificSchema.MyPackage.getVersion()");

    // Act: Look up the data type from different schema context
    String result = everything.lookUpDataType(mockExpression, "DifferentSchema", new ArrayList<>());

    // Assert: Should find function in specified schema
    assertEquals("VARCHAR2", result, "Should find package function in specified schema");
  }

  /**
   * Test case 13: Function name only (no package prefix).
   * Tests: function() pattern where function exists in current schema packages.
   */
  @Test
  void testUnqualifiedFunctionLookupInPackage() {
    // Arrange: Create multiple packages with functions
    OraclePackage package1 = createTestPackage("TestSchema", "Package1",
            createTestFunction("uniqueFunction1", "VARCHAR2"));
    OraclePackage package2 = createTestPackage("TestSchema", "Package2",
            createTestFunction("uniqueFunction2", "NUMBER"));

    everything.getPackageSpecAst().add(package1);
    everything.getPackageSpecAst().add(package2);

    // Test finding functions without package qualification
    Expression expr1 = createExpression("uniqueFunction1()");
    Expression expr2 = createExpression("uniqueFunction2()");

    // Act & Assert
    assertEquals("VARCHAR2", everything.lookUpDataType(expr1, "TestSchema", new ArrayList<>()),
            "Should find uniqueFunction1 in Package1");
    assertEquals("NUMBER", everything.lookUpDataType(expr2, "TestSchema", new ArrayList<>()),
            "Should find uniqueFunction2 in Package2");
  }

  // Helper method to create Expression from string (for testing)
  private Expression createExpression(String text) {
    LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(text));
    return new Expression(logicalExpr);
  }

  // Helper methods to create test objects

  private ObjectType createTestObjectType(String schema, String name, Function function) {
    List<Function> functions = new ArrayList<>();
    functions.add(function);

    return new ObjectType(name, schema, new ArrayList<>(), functions,
            new ArrayList<>(), new ArrayList<>(), null, null);
  }

  private OraclePackage createTestPackage(String schema, String name, Function function) {
    List<Function> functions = new ArrayList<>();
    functions.add(function);

    return new OraclePackage(name, schema, new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), functions, new ArrayList<>(), new ArrayList<>());
  }

  private Function createTestFunction(String name, String returnType) {
    return new Function(name, new ArrayList<>(),new ArrayList<>(), returnType, new ArrayList<>());
  }

  private OraclePackage createTestPackageWithMultipleFunctions(String schema, String name, Function... functions) {
    List<Function> functionList = new ArrayList<>();
    for (Function function : functions) {
      functionList.add(function);
    }

    return new OraclePackage(name, schema, new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), functionList, new ArrayList<>(), new ArrayList<>());
  }
}