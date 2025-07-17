package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.ModPlsqlSimulatorGenerator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ModPlsqlSimulatorGenerator to verify correct controller generation.
 * 
 * Note: This is a simplified test that focuses on the core functionality.
 * Full integration tests would require proper AST object construction.
 */
public class ModPlsqlSimulatorGeneratorTest {

  @Test
  public void testGeneratorClassExists() {
    // Simple test to verify the class and method exist
    assertNotNull(ModPlsqlSimulatorGenerator.class);
    
    // Test that the main method exists (without calling it with complex objects)
    try {
      ModPlsqlSimulatorGenerator.class.getMethod("generateSimulator", 
          me.christianrobert.ora2postgre.plsql.ast.OraclePackage.class, 
          String.class, 
          Everything.class);
      assertTrue(true, "generateSimulator method exists");
    } catch (NoSuchMethodException e) {
      fail("generateSimulator method should exist: " + e.getMessage());
    }
  }

  @Test 
  public void testModPlsqlExecutorGeneration() {
    // Verify the ModPlsqlExecutor generation logic in ExportModPlsqlSimulator
    // Note: ModPlsqlExecutor is now generated code in the target project, not source code
    try {
      Class<?> exportClass = Class.forName("me.christianrobert.ora2postgre.writing.ExportModPlsqlSimulator");
      
      // Check that the generation method exists
      Method generateMethod = exportClass.getDeclaredMethod("generateModPlsqlExecutorContent", String.class);
      assertNotNull(generateMethod, "generateModPlsqlExecutorContent method should exist");
      
      // Test that the method generates content with package initialization
      generateMethod.setAccessible(true);
      String generatedContent = (String) generateMethod.invoke(null, "com.test.generated");
      
      assertNotNull(generatedContent, "Generated content should not be null");
      assertTrue(generatedContent.contains("initializePackageVariables"), 
                 "Generated ModPlsqlExecutor should contain package variable initialization");
      assertTrue(generatedContent.contains("getPackageInitializationProcedure"), 
                 "Generated ModPlsqlExecutor should contain package initialization procedure logic");
      
    } catch (Exception e) {
      fail("ModPlsqlExecutor generation test failed: " + e.getMessage());
    }
  }

  @Test
  public void testExportModPlsqlSimulatorExists() {
    // Verify the export class exists
    try {
      Class<?> exportClass = Class.forName("me.christianrobert.ora2postgre.writing.ExportModPlsqlSimulator");
      
      // Check key methods exist
      exportClass.getMethod("setupTargetProject", 
          String.class, String.class, String.class, String.class, String.class, String.class, String.class);
      exportClass.getMethod("generateSimulators", 
          String.class, String.class, java.util.List.class, java.util.List.class, Everything.class);
      
      assertTrue(true, "All required ExportModPlsqlSimulator methods exist");
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      fail("ExportModPlsqlSimulator class or methods missing: " + e.getMessage());
    }
  }

  @Test
  public void testBasicStringGeneration() {
    // Test that we can generate basic output without complex AST objects
    // This verifies the template structure works
    
    // Create a mock package that we can construct
    try {
      // Create minimal test package using the actual constructor
      java.util.List emptyList = java.util.Collections.emptyList();
      
      me.christianrobert.ora2postgre.plsql.ast.OraclePackage testPackage = 
          new me.christianrobert.ora2postgre.plsql.ast.OraclePackage(
              "TEST_PACKAGE", "TEST_SCHEMA",
              emptyList, emptyList, emptyList, emptyList, emptyList, 
              emptyList, emptyList, emptyList, emptyList, emptyList
          );
      
      Everything data = new Everything();
      
      String result = ModPlsqlSimulatorGenerator.generateSimulator(
          testPackage, "com.test.generated", data);
      
      // Verify basic template structure
      assertNotNull(result, "Generated code should not be null");
      assertTrue(result.contains("package com.test.generated.test_schema.modplsql;"), 
          "Should generate correct package declaration");
      assertTrue(result.contains("class Test_packageModPlsqlController") || 
                 result.contains("class TestPackageModPlsqlController") ||
                 result.contains("ModPlsqlController"), 
          "Should generate controller class");
      assertTrue(result.contains("@Path(\"/modplsql/test_schema/test_package\")"), 
          "Should generate correct path annotation");
      assertTrue(result.contains("@Produces(MediaType.TEXT_HTML)"), 
          "Should produce HTML content type");
      assertTrue(result.contains("AgroalDataSource dataSource"), 
          "Should inject data source");
      
      System.out.println("Generated basic controller structure:");
      System.out.println(result);
      
    } catch (Exception e) {
      fail("Should be able to generate basic controller: " + e.getMessage());
    }
  }
}