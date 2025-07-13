package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Package Variables functionality.
 * Tests Oracle package-level variable declarations and their transformation to PostgreSQL equivalents.
 */
public class PackageVariableTest {

    @Test
    public void testSimplePackageSpecWithVariables() {
        String oracleSql = """
        CREATE PACKAGE TEST_SCHEMA.CONFIG_PKG AS
          g_timeout NUMBER := 300;
          g_version VARCHAR2(10) := '1.0';
          g_enabled BOOLEAN := TRUE;
        END CONFIG_PKG;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle package spec
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        assertEquals("CONFIG_PKG", parsedPackage.getName());
        assertEquals("TEST_SCHEMA", parsedPackage.getSchema());

        // Check that variables were parsed correctly
        assertEquals(3, parsedPackage.getVariables().size());
        
        Variable timeoutVar = parsedPackage.getVariables().get(0);
        assertEquals("g_timeout", timeoutVar.getName());
        assertEquals("NUMBER", timeoutVar.getDataType().getNativeDataType());
        assertNotNull(timeoutVar.getDefaultValue());

        Variable versionVar = parsedPackage.getVariables().get(1);
        assertEquals("g_version", versionVar.getName());
        assertEquals("VARCHAR2", versionVar.getDataType().getNativeDataType());
        assertNotNull(versionVar.getDefaultValue());

        Variable enabledVar = parsedPackage.getVariables().get(2);
        assertEquals("g_enabled", enabledVar.getName());
        assertEquals("BOOLEAN", enabledVar.getDataType().getNativeDataType());
        assertNotNull(enabledVar.getDefaultValue());

        System.out.println("Package: " + parsedPackage);
        System.out.println("Variables found: " + parsedPackage.getVariables().size());
        for (Variable var : parsedPackage.getVariables()) {
            System.out.println("  - " + var.getName() + " " + var.getDataType().getNativeDataType() + 
                             (var.getDefaultValue() != null ? " := " + var.getDefaultValue().toString() : ""));
        }
    }

    @Test
    public void testPackageBodyWithVariables() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.CONFIG_PKG AS
          g_cache_timeout NUMBER := 300;
          g_last_refresh DATE;
          g_config_value VARCHAR2(100) := 'DEFAULT';
          
          FUNCTION get_timeout RETURN NUMBER IS
          BEGIN
            RETURN g_cache_timeout;
          END;
        END CONFIG_PKG;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle package body
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        assertEquals("CONFIG_PKG", parsedPackage.getName());
        assertEquals("TEST_SCHEMA", parsedPackage.getSchema());

        // Check that variables were parsed correctly
        assertEquals(3, parsedPackage.getVariables().size());
        
        Variable cacheTimeoutVar = parsedPackage.getVariables().get(0);
        assertEquals("g_cache_timeout", cacheTimeoutVar.getName());
        assertEquals("NUMBER", cacheTimeoutVar.getDataType().getNativeDataType());
        assertNotNull(cacheTimeoutVar.getDefaultValue());

        Variable lastRefreshVar = parsedPackage.getVariables().get(1);
        assertEquals("g_last_refresh", lastRefreshVar.getName());
        assertEquals("DATE", lastRefreshVar.getDataType().getNativeDataType());
        assertNull(lastRefreshVar.getDefaultValue()); // No default value

        Variable configValueVar = parsedPackage.getVariables().get(2);
        assertEquals("g_config_value", configValueVar.getName());
        assertEquals("VARCHAR2", configValueVar.getDataType().getNativeDataType());
        assertNotNull(configValueVar.getDefaultValue());

        // Check that functions were also parsed
        assertEquals(1, parsedPackage.getFunctions().size());
        Function getTimeoutFunc = parsedPackage.getFunctions().get(0);
        assertEquals("get_timeout", getTimeoutFunc.getName());

        System.out.println("Package Body: " + parsedPackage);
        System.out.println("Variables found: " + parsedPackage.getVariables().size());
        System.out.println("Functions found: " + parsedPackage.getFunctions().size());
    }

    @Test
    public void testPackageVariableTransformationToPostgreSQL() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.CACHE_PKG AS
          g_timeout NUMBER := 300;
          g_enabled BOOLEAN := TRUE;
          
          FUNCTION is_enabled RETURN BOOLEAN IS
          BEGIN
            RETURN g_enabled;
          END;
        END CACHE_PKG;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle package body
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        // Test package transformation to PostgreSQL
        String pgPackage = parsedPackage.toPostgre(data, false);
        
        System.out.println("Generated PostgreSQL package:");
        System.out.println(pgPackage);

        assertNotNull(pgPackage);
        
        // The transformation should handle package variables appropriately
        // (this test will help us understand the current transformation behavior)
        assertTrue(pgPackage.length() > 0);
    }

    @Test
    public void testComplexPackageVariablesWithExpressions() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.COMPLEX_PKG AS
          g_base_timeout NUMBER := 60;
          g_extended_timeout NUMBER := g_base_timeout * 5;
          g_app_name VARCHAR2(50) := 'MyApp';
          g_version VARCHAR2(20) := g_app_name || '_v2.1';
          
          FUNCTION get_version RETURN VARCHAR2 IS
          BEGIN
            RETURN g_version;
          END;
        END COMPLEX_PKG;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle package body
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        // Check that complex variables with expressions were parsed
        assertEquals(4, parsedPackage.getVariables().size());
        
        Variable baseTimeoutVar = parsedPackage.getVariables().get(0);
        assertEquals("g_base_timeout", baseTimeoutVar.getName());
        
        Variable extendedTimeoutVar = parsedPackage.getVariables().get(1);
        assertEquals("g_extended_timeout", extendedTimeoutVar.getName());
        assertNotNull(extendedTimeoutVar.getDefaultValue()); // Should have expression
        
        Variable appNameVar = parsedPackage.getVariables().get(2);
        assertEquals("g_app_name", appNameVar.getName());
        
        Variable versionVar = parsedPackage.getVariables().get(3);
        assertEquals("g_version", versionVar.getName());
        assertNotNull(versionVar.getDefaultValue()); // Should have concatenation expression

        System.out.println("Complex Package Variables:");
        for (Variable var : parsedPackage.getVariables()) {
            System.out.println("  - " + var.getName() + " " + var.getDataType().getNativeDataType() + 
                             (var.getDefaultValue() != null ? " := " + var.getDefaultValue().toString() : ""));
        }
    }

    /**
     * Helper method to create test data context
     */
    private Everything createTestData() {
        Everything data = new Everything();
        data.getUserNames().add("TEST_SCHEMA");
        
        return data;
    }
}