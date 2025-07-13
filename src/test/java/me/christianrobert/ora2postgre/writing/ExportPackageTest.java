package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExportPackage functionality, specifically the mergeSpecAndBody method.
 * Validates that package specifications and bodies are properly merged.
 */
public class ExportPackageTest {

    @Test
    public void testMergeSpecAndBodyVariables() {
        // Create package spec with variables
        Variable specVar1 = new Variable("g_spec_var", new DataTypeSpec("NUMBER", null, null, null), null);
        Variable specVar2 = new Variable("g_shared_var", new DataTypeSpec("VARCHAR2", null, null, null), null);
        
        OraclePackage spec = new OraclePackage(
            "TEST_PKG", 
            "TEST_SCHEMA", 
            Arrays.asList(specVar1, specVar2), // spec variables
            null, null, null, null, null, null, null
        );

        // Create package body with variables (including one with same name)
        Variable bodyVar1 = new Variable("g_body_var", new DataTypeSpec("BOOLEAN", null, null, null), null);
        Variable bodyVar2 = new Variable("g_shared_var", new DataTypeSpec("VARCHAR2", null, null, null), null); // duplicate name
        
        Function bodyFunction = new Function("test_func", null, null, "NUMBER", null);
        
        OraclePackage body = new OraclePackage(
            "TEST_PKG", 
            "TEST_SCHEMA", 
            Arrays.asList(bodyVar1, bodyVar2), // body variables
            null, null, null, null,
            Arrays.asList(bodyFunction), // body functions
            null, null
        );

        // Test merging via reflection (since mergeSpecAndBody is private)
        try {
            var method = ExportPackage.class.getDeclaredMethod("mergeSpecAndBody", List.class, List.class);
            method.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<OraclePackage> merged = (List<OraclePackage>) method.invoke(null, 
                Arrays.asList(spec), Arrays.asList(body));
            
            // Verify merged package
            assertEquals(1, merged.size());
            OraclePackage mergedPkg = merged.get(0);
            
            assertEquals("TEST_PKG", mergedPkg.getName());
            assertEquals("TEST_SCHEMA", mergedPkg.getSchema());
            
            // Verify variables are merged (should have 3 unique variables: g_spec_var, g_shared_var, g_body_var)
            assertEquals(3, mergedPkg.getVariables().size());
            
            // Check that all expected variables are present
            List<String> varNames = mergedPkg.getVariables().stream()
                .map(Variable::getName)
                .toList();
            
            assertTrue(varNames.contains("g_spec_var"));
            assertTrue(varNames.contains("g_shared_var"));
            assertTrue(varNames.contains("g_body_var"));
            
            // Verify functions are included
            assertEquals(1, mergedPkg.getFunctions().size());
            assertEquals("test_func", mergedPkg.getFunctions().get(0).getName());
            
            System.out.println("Merged package variables:");
            for (Variable var : mergedPkg.getVariables()) {
                System.out.println("  - " + var.getName() + " (" + var.getDataType().getNativeDataType() + ")");
            }
            
        } catch (Exception e) {
            fail("Failed to test mergeSpecAndBody: " + e.getMessage());
        }
    }

    @Test 
    public void testMergeSpecOnlyPackage() {
        // Test package spec without matching body
        Variable specVar = new Variable("g_spec_only", new DataTypeSpec("NUMBER", null, null, null), null);
        
        OraclePackage spec = new OraclePackage(
            "SPEC_ONLY_PKG", 
            "TEST_SCHEMA", 
            Arrays.asList(specVar),
            null, null, null, null, null, null, null
        );

        try {
            var method = ExportPackage.class.getDeclaredMethod("mergeSpecAndBody", List.class, List.class);
            method.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<OraclePackage> merged = (List<OraclePackage>) method.invoke(null, 
                Arrays.asList(spec), Arrays.asList()); // empty body list
            
            // Verify spec-only package is preserved
            assertEquals(1, merged.size());
            OraclePackage mergedPkg = merged.get(0);
            
            assertEquals("SPEC_ONLY_PKG", mergedPkg.getName());
            assertEquals(1, mergedPkg.getVariables().size());
            assertEquals("g_spec_only", mergedPkg.getVariables().get(0).getName());
            
        } catch (Exception e) {
            fail("Failed to test spec-only package: " + e.getMessage());
        }
    }

    @Test
    public void testMergeBodyOnlyPackage() {
        // Test package body without matching spec
        Variable bodyVar = new Variable("g_body_only", new DataTypeSpec("BOOLEAN", null, null, null), null);
        
        OraclePackage body = new OraclePackage(
            "BODY_ONLY_PKG", 
            "TEST_SCHEMA", 
            Arrays.asList(bodyVar),
            null, null, null, null, null, null, null
        );

        try {
            var method = ExportPackage.class.getDeclaredMethod("mergeSpecAndBody", List.class, List.class);
            method.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<OraclePackage> merged = (List<OraclePackage>) method.invoke(null, 
                Arrays.asList(), Arrays.asList(body)); // empty spec list
            
            // Verify body-only package is preserved
            assertEquals(1, merged.size());
            OraclePackage mergedPkg = merged.get(0);
            
            assertEquals("BODY_ONLY_PKG", mergedPkg.getName());
            assertEquals(1, mergedPkg.getVariables().size());
            assertEquals("g_body_only", mergedPkg.getVariables().get(0).getName());
            
        } catch (Exception e) {
            fail("Failed to test body-only package: " + e.getMessage());
        }
    }
}