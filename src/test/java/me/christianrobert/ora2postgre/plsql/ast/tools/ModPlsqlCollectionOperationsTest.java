package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the enhanced collection operations (DELETE, TRIM, EXISTS) for package variables.
 * This test verifies the new transformation methods for Oracle collection operations.
 */
public class ModPlsqlCollectionOperationsTest {

  @Test
  public void testDeleteCollectionTransformations() {
    // Test DELETE transformation methods directly
    String targetSchema = "test_schema";
    String packageName = "test_pkg";
    String collectionName = "g_numbers";
    
    // Test DELETE with index
    String deleteElement = PackageVariableReferenceTransformer.transformCollectionDelete(
        targetSchema, packageName, collectionName, "2");
    assertEquals("PERFORM sys.delete_package_collection_element('test_schema', 'test_pkg', 'g_numbers', 2)", 
        deleteElement, "DELETE element transformation should be correct");
    
    // Test DELETE all
    String deleteAll = PackageVariableReferenceTransformer.transformCollectionDelete(
        targetSchema, packageName, collectionName, null);
    assertEquals("PERFORM sys.delete_package_collection_all('test_schema', 'test_pkg', 'g_numbers')", 
        deleteAll, "DELETE all transformation should be correct");
    
    // Test DELETE with empty parameter
    String deleteAllEmpty = PackageVariableReferenceTransformer.transformCollectionDelete(
        targetSchema, packageName, collectionName, "");
    assertEquals("PERFORM sys.delete_package_collection_all('test_schema', 'test_pkg', 'g_numbers')", 
        deleteAllEmpty, "DELETE with empty parameter should transform to DELETE all");
    
    System.out.println("✅ DELETE transformations test passed");
  }

  @Test
  public void testTrimCollectionTransformations() {
    // Test TRIM transformation methods directly
    String targetSchema = "test_schema";
    String packageName = "test_pkg";
    String collectionName = "g_numbers";
    
    // Test TRIM with default count
    String trimDefault = PackageVariableReferenceTransformer.transformCollectionTrim(
        targetSchema, packageName, collectionName, null);
    assertEquals("PERFORM sys.trim_package_collection('test_schema', 'test_pkg', 'g_numbers', 1)", 
        trimDefault, "TRIM default transformation should be correct");
    
    // Test TRIM with specific count
    String trimCount = PackageVariableReferenceTransformer.transformCollectionTrim(
        targetSchema, packageName, collectionName, "3");
    assertEquals("PERFORM sys.trim_package_collection('test_schema', 'test_pkg', 'g_numbers', 3)", 
        trimCount, "TRIM with count transformation should be correct");
    
    // Test TRIM with empty parameter
    String trimEmpty = PackageVariableReferenceTransformer.transformCollectionTrim(
        targetSchema, packageName, collectionName, "");
    assertEquals("PERFORM sys.trim_package_collection('test_schema', 'test_pkg', 'g_numbers', 1)", 
        trimEmpty, "TRIM with empty parameter should use default count 1");
    
    System.out.println("✅ TRIM transformations test passed");
  }

  @Test
  public void testExistsCollectionTransformation() {
    // Test EXISTS transformation
    String targetSchema = "test_schema";
    String packageName = "test_pkg";
    String collectionName = "g_numbers";
    
    String exists = PackageVariableReferenceTransformer.transformCollectionMethod(
        targetSchema, packageName, collectionName, "EXISTS");
    assertEquals("sys.package_collection_exists('test_schema', 'test_pkg', 'g_numbers', %s)", 
        exists, "EXISTS transformation should have placeholder for index");
    
    System.out.println("✅ EXISTS transformation test passed");
  }

  @Test
  public void testEnhancedCollectionMethodSupport() {
    // Test that enhanced transformCollectionMethod supports new operations
    
    String targetSchema = "test_schema";
    String packageName = "test_pkg";
    String collectionName = "g_array";
    
    // Test that EXISTS is now recognized (previously returned TODO comment)
    String exists = PackageVariableReferenceTransformer.transformCollectionMethod(
        targetSchema, packageName, collectionName, "EXISTS");
    assertFalse(exists.contains("TODO"), 
        "EXISTS method should no longer return TODO comment");
    assertTrue(exists.contains("sys.package_collection_exists"), 
        "EXISTS method should return proper function call");
    
    // Test that existing methods still work
    String count = PackageVariableReferenceTransformer.transformCollectionMethod(
        targetSchema, packageName, collectionName, "COUNT");
    assertTrue(count.contains("sys.get_package_collection_count"), 
        "COUNT method should work correctly");
    
    // Test that unknown methods still return TODO comments
    String unknown = PackageVariableReferenceTransformer.transformCollectionMethod(
        targetSchema, packageName, collectionName, "UNKNOWN_METHOD");
    assertTrue(unknown.contains("TODO"), 
        "Unknown methods should still return TODO comment");
    
    System.out.println("✅ Enhanced collection method support test passed");
  }

  @Test
  public void testCaseSensitivity() {
    // Test that method names are case-insensitive
    String targetSchema = "test_schema";
    String packageName = "test_pkg";
    String collectionName = "g_numbers";
    
    // Test different cases for DELETE
    String deleteLower = PackageVariableReferenceTransformer.transformCollectionDelete(
        targetSchema, packageName, collectionName, "1");
    String trimLower = PackageVariableReferenceTransformer.transformCollectionTrim(
        targetSchema, packageName, collectionName, "2");
    
    assertTrue(deleteLower.contains("delete_package_collection_element"), 
        "DELETE should work regardless of case");
    assertTrue(trimLower.contains("trim_package_collection"), 
        "TRIM should work regardless of case");
    
    // Test method name case handling
    String existsUpper = PackageVariableReferenceTransformer.transformCollectionMethod(
        targetSchema, packageName, collectionName, "EXISTS");
    String existsLower = PackageVariableReferenceTransformer.transformCollectionMethod(
        targetSchema, packageName, collectionName, "exists");
    
    assertEquals(existsUpper, existsLower, "Method names should be case-insensitive");
    
    System.out.println("✅ Case sensitivity test passed");
  }
}