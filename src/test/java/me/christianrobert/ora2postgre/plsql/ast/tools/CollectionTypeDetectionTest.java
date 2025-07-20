package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test collection type detection for package variables.
 * This test verifies that collection package variables use the correct accessor functions.
 */
public class CollectionTypeDetectionTest {

  @Test
  public void testMapDataTypeToAccessor() {
    // Test Oracle collection types
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("VARRAY(10) OF NUMBER"));
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("TABLE OF VARCHAR2"));
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("NESTED TABLE OF NUMBER"));
    
    // Test PostgreSQL array syntax
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("text[]"));
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("numeric[]"));
    
    // Test regular data types
    assertEquals("numeric", PackageVariableReferenceTransformer.mapDataTypeToAccessor("NUMBER"));
    assertEquals("text", PackageVariableReferenceTransformer.mapDataTypeToAccessor("VARCHAR2"));
    assertEquals("boolean", PackageVariableReferenceTransformer.mapDataTypeToAccessor("BOOLEAN"));
    
    System.out.println("✅ Collection type detection test passed");
  }
  
  @Test
  public void testTransformReadWithCollectionTypes() {
    String targetSchema = "user_robert";
    String packageName = "pkg_varray_example";
    String varName = "g_numbers";
    
    // Test with Oracle VARRAY type - should use collection accessor
    String varrayResult = PackageVariableReferenceTransformer.transformRead(
        targetSchema, packageName, varName, "VARRAY(10) OF NUMBER");
    assertTrue(varrayResult.contains("sys.get_package_collection("), 
        "VARRAY type should use collection accessor");
    assertFalse(varrayResult.contains("sys.get_package_var_text("), 
        "VARRAY type should not use text accessor");
    
    // Test with PostgreSQL array type - should use collection accessor 
    String arrayResult = PackageVariableReferenceTransformer.transformRead(
        targetSchema, packageName, varName, "text[]");
    assertTrue(arrayResult.contains("sys.get_package_collection("), 
        "Array type should use collection accessor");
    
    // Test with regular type - should use typed accessor
    String numberResult = PackageVariableReferenceTransformer.transformRead(
        targetSchema, packageName, varName, "NUMBER");
    assertTrue(numberResult.contains("sys.get_package_var_numeric("), 
        "NUMBER type should use numeric accessor");
    
    System.out.println("✅ Transform read with collection types test passed");
    System.out.println("VARRAY result: " + varrayResult);
    System.out.println("Array result: " + arrayResult);
    System.out.println("Number result: " + numberResult);
  }
  
  @Test
  public void testCollectionMethodsWithProperType() {
    String targetSchema = "user_robert";
    String packageName = "pkg_varray_example";
    String collectionName = "g_numbers";
    
    // Test collection COUNT method
    String countResult = PackageVariableReferenceTransformer.transformCollectionMethod(
        targetSchema, packageName, collectionName, "COUNT");
    assertTrue(countResult.contains("sys.get_package_collection_count("), 
        "COUNT method should use collection function");
    
    System.out.println("✅ Collection methods test passed");
    System.out.println("COUNT result: " + countResult);
  }
  
  @Test 
  public void testCaseSensitiveTypeDetection() {
    // Test case variations
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("varray(10) of number"));
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("VARRAY(10) OF NUMBER"));
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("table of varchar2"));
    assertEquals("collection", PackageVariableReferenceTransformer.mapDataTypeToAccessor("TABLE OF VARCHAR2"));
    
    System.out.println("✅ Case sensitive type detection test passed");
  }
}