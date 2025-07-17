# Package Variables Implementation Plan - Direct Table Access Pattern

**Date**: 2025-07-17  
**Status**: 🔄 **REFACTORING REQUIRED** - PRE/POST approach fundamentally flawed  
**Issue**: Current PRE/POST pattern has session isolation and synchronization problems  
**Proposed Solution**: Direct Table Access Pattern aligned with established codebase patterns  

---

## **Critical Issues with Current PRE/POST Implementation**

### **Problem 1: Session Isolation Violation**
The current temporary table approach uses a shared PostgreSQL session across all HTTP requests from the Quarkus server. Oracle package variables are session-scoped, but the current implementation creates globally shared state.

### **Problem 2: Synchronization Race Conditions**
Example demonstrating the timing issue:
```plsql
PACKAGE BODY minitest IS
  gX number := 1;
  
  procedure add2gXpackagevar is
  begin
    gX := gX + 1;  -- This should increment shared package variable
  end;
  
  procedure testminihtp1 is
  begin
    htp.p(gX);           -- Shows gX=1 (loaded at procedure start)
    add2gXpackagevar;    -- This increments gX to 2 in temp table
    htp.p(gX);           -- Still shows 1 (stale local variable copy)
  end;
END;
```

**Root Cause**: PRE/POST pattern loads package variables into local variables at procedure entry, but cannot handle inter-procedure calls that modify shared state within the same session.

### **Problem 3: Architectural Misalignment**
The PRE/POST pattern violates the established patterns in the codebase:
- **HTP Buffer**: Uses direct table access functions (`SYS.HTP_p()`)
- **ModPlsqlExecutor**: Per-connection state management works correctly
- **Temporary Tables**: Session isolation via connection boundaries is proven

---

## **Proposed Solution: Direct Table Access Pattern**

### **Core Strategy: Eliminate Local Variable Copies**

Instead of loading package variables into local variables, generate **accessor functions** that read/write directly to temporary tables on every access.

### **Architecture Alignment**

This pattern leverages existing successful infrastructure:
- **Connection-Based Sessions**: Each HTTP request gets isolated state through separate connections
- **Temporary Table Isolation**: PostgreSQL temporary tables provide natural session isolation
- **Accessor Function Pattern**: Similar to how `SYS.HTP_p()` works with HTP buffer

### **Transformation Example**

**Oracle Code:**
```plsql
-- Oracle package variable access
gX := gX + 1;
htp.p(gX);
```

**Current Broken PostgreSQL (PRE/POST):**
```plsql
-- Load at procedure start (PRE)
SELECT value INTO gX FROM test_schema_minitest_gX LIMIT 1;

-- Use stale local variable
gX := gX + 1;  -- Local variable only
htp.p(gX);     -- Shows stale value

-- Save at procedure end (POST) 
UPDATE test_schema_minitest_gX SET value = gX;
```

**Proposed Direct Access PostgreSQL:**
```plsql
-- Direct table access on every use
SELECT sys.set_package_var('minitest', 'gX', 
  sys.get_package_var('minitest', 'gX')::numeric + 1);
SELECT sys.htp_p(sys.get_package_var('minitest', 'gX'));
```

---

## **Implementation Plan**

### **Phase 1: Create System-Level Accessor Functions** ⏳ 
**Estimated Time**: 2 hours  
**Status**: Ready to implement

#### **1.1 Create Package Variable Accessor Functions**
Create PostgreSQL functions in the `sys` schema for reading/writing package variables:

```sql
-- Read package variable (returns text, caller handles casting)
CREATE OR REPLACE FUNCTION sys.get_package_var(
  package_name text, 
  var_name text
) RETURNS text LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
  value text;
BEGIN
  -- Build table name using existing naming convention
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Read from session temp table
  EXECUTE format('SELECT value FROM %I LIMIT 1', table_name) INTO value;
  
  RETURN value;
EXCEPTION
  WHEN undefined_table THEN
    -- Table doesn't exist, return null (will be handled by caller)
    RETURN NULL;
END;
$$;

-- Write package variable (accepts text, caller handles casting)
CREATE OR REPLACE FUNCTION sys.set_package_var(
  package_name text, 
  var_name text, 
  value text
) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
BEGIN
  -- Build table name using existing naming convention
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Update session temp table
  EXECUTE format('UPDATE %I SET value = %L', table_name, value);
END;
$$;
```

#### **1.2 Create Type-Safe Wrapper Functions**
Create convenience functions for common data types:

```sql
-- Numeric getter/setter
CREATE OR REPLACE FUNCTION sys.get_package_var_numeric(package_name text, var_name text) 
RETURNS numeric AS $$
BEGIN
  RETURN sys.get_package_var(package_name, var_name)::numeric;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sys.set_package_var_numeric(package_name text, var_name text, value numeric) 
RETURNS void AS $$
BEGIN
  PERFORM sys.set_package_var(package_name, var_name, value::text);
END;
$$ LANGUAGE plpgsql;

-- Boolean getter/setter
CREATE OR REPLACE FUNCTION sys.get_package_var_boolean(package_name text, var_name text) 
RETURNS boolean AS $$
BEGIN
  RETURN sys.get_package_var(package_name, var_name)::boolean;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sys.set_package_var_boolean(package_name text, var_name text, value boolean) 
RETURNS void AS $$
BEGIN
  PERFORM sys.set_package_var(package_name, var_name, value::text);
END;
$$ LANGUAGE plpgsql;
```

#### **1.3 Integration with Existing Infrastructure**
- **File Location**: Add to existing `sys` schema creation in `ExportPostgreSQL.java`
- **Naming Convention**: Use existing `schema_packagename_variablename` table naming
- **Error Handling**: Graceful handling of missing tables (return null, log warning)

### **Phase 2: Create Variable Reference Transformation System** ⏳
**Estimated Time**: 4 hours  
**Status**: Ready to implement

#### **2.1 Create PackageVariableReferenceTransformer**
New class to handle Oracle package variable references:

```java
// File: /src/main/java/tools/transformers/PackageVariableReferenceTransformer.java
public class PackageVariableReferenceTransformer {
    
    // Transform read access: gX → sys.get_package_var_numeric('minitest', 'gX')
    public static String transformRead(String packageName, String varName, String dataType) {
        return String.format("sys.get_package_var_%s('%s', '%s')", 
            mapDataTypeToAccessor(dataType), packageName.toLowerCase(), varName.toLowerCase());
    }
    
    // Transform write access: gX := value → sys.set_package_var_numeric('minitest', 'gX', value)
    public static String transformWrite(String packageName, String varName, String dataType, String value) {
        return String.format("SELECT sys.set_package_var_%s('%s', '%s', %s)", 
            mapDataTypeToAccessor(dataType), packageName.toLowerCase(), varName.toLowerCase(), value);
    }
    
    private static String mapDataTypeToAccessor(String oracleType) {
        switch (oracleType.toUpperCase()) {
            case "NUMBER": return "numeric";
            case "BOOLEAN": return "boolean";
            case "VARCHAR2": return "text";
            case "DATE": return "timestamp";
            default: return "text"; // Fallback
        }
    }
}
```

#### **2.2 Integrate with AST Transformation**
Modify existing AST classes to use the new transformer:

**In `Variable.java`:**
```java
@Override
public String toPostgre(Everything data) {
    // Check if this is a package variable reference
    if (isPackageVariableReference(data)) {
        OraclePackage pkg = findContainingPackage(data);
        PackageVariable var = pkg.findVariable(this.name);
        
        return PackageVariableReferenceTransformer.transformRead(
            pkg.getName(), this.name, var.getDataType()
        );
    }
    
    // Regular variable handling
    return super.toPostgre(data);
}
```

**In `AssignmentStatement.java`:**
```java
@Override
public String toPostgre(Everything data) {
    // Check if left side is package variable
    if (leftSide.isPackageVariableReference(data)) {
        OraclePackage pkg = findContainingPackage(data);
        PackageVariable var = pkg.findVariable(leftSide.name);
        
        return PackageVariableReferenceTransformer.transformWrite(
            pkg.getName(), leftSide.name, var.getDataType(), rightSide.toPostgre(data)
        );
    }
    
    // Regular assignment handling
    return super.toPostgre(data);
}
```

#### **2.3 Package Variable Detection Logic**
Create helper methods to identify package variable references:

```java
// In Variable.java or new helper class
private boolean isPackageVariableReference(Everything data) {
    // Check if variable name exists in any package in current schema
    for (OraclePackage pkg : data.getPackages()) {
        if (pkg.hasVariable(this.name)) {
            return true;
        }
    }
    return false;
}

private OraclePackage findContainingPackage(Everything data) {
    // Find which package contains this variable
    for (OraclePackage pkg : data.getPackages()) {
        if (pkg.hasVariable(this.name)) {
            return pkg;
        }
    }
    return null;
}
```

### **Phase 3: Remove PRE/POST Infrastructure** ⏳
**Estimated Time**: 2 hours  
**Status**: Ready to implement

#### **3.1 Remove PackageVariableHelper**
- **Delete**: `/src/main/java/tools/helpers/PackageVariableHelper.java`
- **Reason**: No longer needed with direct access pattern

#### **3.2 Remove PRE/POST Integration**
**In `StandardFunctionStrategy.java`:**
- **Remove**: Lines 72-84 (variable declarations)
- **Remove**: Lines 132-144 (PRE phase)
- **Remove**: Lines 157-160 (POST phase)

**In `StandardProcedureStrategy.java`:**
- **Remove**: Lines 67-79 (variable declarations)
- **Remove**: Lines 134-137 (PRE phase)
- **Remove**: Lines 150-153 (POST phase)

#### **3.3 Keep Package Variable Table Creation**
**In `StandardPackageStrategy.java`:**
- **Keep**: Lines 140-180 (temporary table creation)
- **Reason**: Still needed for storage, just accessed differently

### **Phase 4: Update Tests and Validation** ⏳
**Estimated Time**: 2 hours  
**Status**: Ready to implement

#### **4.1 Update PackageVariableTest**
**In `PackageVariableTest.java`:**
- **Update**: Expected output to use direct access functions
- **Add**: Test cases for inter-procedure variable sharing
- **Add**: Test cases for different data types

**Expected Test Output:**
```sql
-- Old PRE/POST output (to be removed)
DECLARE
  g_timeout numeric;
  g_enabled boolean;
BEGIN
  SELECT value INTO g_timeout FROM test_schema_cache_pkg_g_timeout LIMIT 1;
  -- ... procedure body ...
  UPDATE test_schema_cache_pkg_g_timeout SET value = g_timeout;
END;

-- New direct access output
DECLARE
BEGIN
  return sys.get_package_var_boolean('cache_pkg', 'g_enabled');
END;
```

#### **4.2 Create Integration Tests**
**New test file**: `PackageVariableIntegrationTest.java`
- **Test**: Inter-procedure variable sharing (the `minitest` example)
- **Test**: Session isolation between different connections
- **Test**: Data type conversions and edge cases

### **Phase 5: Documentation and Cleanup** ⏳
**Estimated Time**: 1 hour  
**Status**: Ready to implement

#### **5.1 Update Architecture Documentation**
- **Update**: CLAUDE.md with new Direct Table Access Pattern
- **Update**: Package variable section in project documentation
- **Remove**: References to PRE/POST pattern

#### **5.2 Code Cleanup**
- **Remove**: Unused imports and helper classes
- **Update**: Comments to reflect new architecture
- **Verify**: No remaining references to old PRE/POST pattern

---

## **Benefits of Direct Table Access Pattern**

### **Problem Resolution**
1. **✅ Session Isolation**: Each connection gets isolated temporary tables (leverages existing pattern)
2. **✅ Synchronization**: No stale local variables, every access goes to current table state
3. **✅ Architecture Alignment**: Consistent with HTP buffer and ModPlsqlExecutor patterns

### **Technical Benefits**
1. **Simpler Architecture**: No PRE/POST synchronization complexity
2. **Proven Pattern**: Leverages existing successful temporary table infrastructure
3. **Correct Oracle Semantics**: Package variables behave like shared session state
4. **Easier Debugging**: All variable access goes through traceable functions

### **Performance Considerations**
1. **Acceptable Overhead**: Each variable access becomes a function call (similar to HTP)
2. **Connection Pooling**: Existing connection pooling handles session management
3. **Temporary Table Performance**: PostgreSQL temporary tables are memory-backed and fast

---

## **Implementation Timeline**

| Phase | Task | Estimated Time | Dependencies |
|-------|------|----------------|--------------|
| 1 | System accessor functions | 2 hours | None |
| 2 | AST transformation system | 4 hours | Phase 1 |
| 3 | Remove PRE/POST infrastructure | 2 hours | Phase 2 |
| 4 | Update tests and validation | 2 hours | Phase 3 |
| 5 | Documentation and cleanup | 1 hour | Phase 4 |
| **Total** | **Complete refactoring** | **11 hours** | Sequential |

---

## **Risk Analysis**

### **Low Risk**
- **Temporary table infrastructure**: Already working and tested
- **Connection management**: Established and reliable
- **AST transformation**: Well-understood codebase patterns

### **Medium Risk**
- **Variable reference detection**: Need robust logic to identify package variables
- **Data type mapping**: Ensure correct casting between Oracle and PostgreSQL types
- **Test coverage**: Need comprehensive tests for edge cases

### **Mitigation Strategies**
1. **Incremental Implementation**: Implement and test each phase separately
2. **Backward Compatibility**: Keep existing tests passing during transition
3. **Error Handling**: Graceful degradation when package variables don't exist
4. **Performance Monitoring**: Monitor function call overhead in production

---

## **Success Criteria**

### **Primary Goals**
1. **✅ Fix synchronization issues**: `minitest` example should work correctly
2. **✅ Maintain session isolation**: Each HTTP request gets isolated package variables
3. **✅ Remove PRE/POST complexity**: Simpler, more maintainable architecture
4. **✅ All tests pass**: Existing and new tests validate functionality

### **Secondary Goals**
1. **✅ Performance acceptable**: No significant performance degradation
2. **✅ Architecture consistency**: Follows established codebase patterns
3. **✅ Maintainable code**: Clear, well-documented transformation logic
4. **✅ Robust error handling**: Graceful handling of edge cases

---

## **Future Enhancements** (Not in Current Scope)

### **Potential Optimizations**
1. **Variable Caching**: Cache frequently accessed variables in connection-local memory
2. **Bulk Operations**: Optimize multiple variable access patterns
3. **Type Safety**: Enhanced compile-time type checking for variable references

### **Advanced Features**
1. **Package Constants**: Distinguish between mutable variables and immutable constants
2. **Cross-Package References**: Support for Package A referencing Package B variables
3. **Package Initialization**: Special handling for package startup procedures

---

## **Conclusion**

The Direct Table Access Pattern represents a fundamental architectural shift from the flawed PRE/POST approach to a proven, session-isolated pattern that aligns with existing codebase infrastructure. This refactoring addresses the core synchronization and session isolation issues while maintaining the PostgreSQL-first architecture philosophy.

The implementation leverages existing successful patterns (HTP buffer, ModPlsqlExecutor, temporary tables) and eliminates the complex and error-prone PRE/POST synchronization mechanism. The result will be a simpler, more reliable, and more maintainable package variable system that correctly implements Oracle package variable semantics in a PostgreSQL environment.