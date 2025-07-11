# STANDALONE_FUNCTIONS_PROCEDURES_IMPLEMENTATION_PLAN.md

## Oracle Standalone Functions and Procedures Implementation Plan

### Overview
This plan outlines the implementation of support for Oracle standalone functions and procedures in the ora2postgre migration tool. These are functions and procedures that exist independently of packages or object types - currently only package-based and object type-based functions/procedures are supported.

### Current Architecture Context
The refactoring has established a unified strategy-based transformation architecture with:
- `/strategies/` - Transformation strategy interfaces and implementations  
- `/managers/` - Transformation orchestration and strategy selection
- `/transformers/` - Utility transformation classes
- `/helpers/` - Supporting utility classes
- Export classes use transformation managers consistently
- Unified `TransformationStrategy<T>` base interface for all transformations

---

## Implementation Strategy: Parallel Processing Approach

### **Design Philosophy**
Treat standalone functions and standalone procedures as **separate, parallel database elements** (like tables vs views) rather than variants of the same element. This provides:

1. **Clear Separation**: Distinct extraction, transformation, and export pipelines
2. **Frontend Clarity**: Separate configuration toggles and statistics display  
3. **Maintainability**: Independent processing paths reduce complexity
4. **Scalability**: Easy to extend or modify one type without affecting the other

---

## Phase 1: Configuration Infrastructure

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Add configuration support for standalone functions and procedures as independent features.

### **Implementation Steps**

#### **Step 1.1: Update application.properties**
```properties
# Add new configuration flags
do.standalone-functions=true
do.standalone-procedures=true

# Export control (leverage existing flags)  
do.write-postgre-files=true
do.write-rest-controllers=true
do.rest-controller-functions=true
do.rest-controller-procedures=true
```

#### **Step 1.2: Update Config.java**
```java
// Add new configuration methods
public boolean isDoStandaloneFunctions() {
    return getProperty("do.standalone-functions", true);
}

public boolean isDoStandaloneProcedures() {
    return getProperty("do.standalone-procedures", true);  
}
```

### **Validation**
- Configuration flags can be toggled independently
- Integration with existing export control flags works correctly

---

## Phase 2: Extraction Infrastructure

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Create extraction classes for standalone functions and procedures following the established pattern.

### **Implementation Steps**

#### **Step 2.1: Create StandaloneFunctionExtractor.java**
- **File**: `/oracledb/StandaloneFunctionExtractor.java`
- **Pattern**: Follow `PackageExtractor.java` and `TriggerExtractor.java`
- **Query**: `SELECT * FROM ALL_OBJECTS WHERE object_type = 'FUNCTION'`
- **Source**: Query `ALL_SOURCE` for complete source code
- **Processing**: Apply `CodeCleaner.noComments()` and source preparation

```java
public class StandaloneFunctionExtractor {
    private static final Logger log = LoggerFactory.getLogger(StandaloneFunctionExtractor.class);
    
    public static List<PlsqlCode> extract(Connection connection, List<String> userNames) throws SQLException {
        // Implementation follows PackageExtractor pattern
        // Query: object_type = 'FUNCTION' AND owner IN (userNames)
        // Retrieve from ALL_SOURCE, apply code cleaning
    }
}
```

#### **Step 2.2: Create StandaloneProcedureExtractor.java**
- **File**: `/oracledb/StandaloneProcedureExtractor.java`  
- **Pattern**: Identical to StandaloneFunctionExtractor
- **Query**: `SELECT * FROM ALL_OBJECTS WHERE object_type = 'PROCEDURE'`

```java
public class StandaloneProcedureExtractor {
    private static final Logger log = LoggerFactory.getLogger(StandaloneProcedureExtractor.class);
    
    public static List<PlsqlCode> extract(Connection connection, List<String> userNames) throws SQLException {
        // Implementation follows PackageExtractor pattern
        // Query: object_type = 'PROCEDURE' AND owner IN (userNames)
        // Retrieve from ALL_SOURCE, apply code cleaning
    }
}
```

### **Validation**
- Extractors can retrieve standalone functions and procedures from Oracle
- Code cleaning and preparation works correctly
- Performance is acceptable for large numbers of standalone objects

---

## Phase 3: Data Storage Extension

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Extend the `Everything.java` singleton to store standalone functions and procedures.

### **Implementation Steps**

#### **Step 3.1: Add Storage Lists to Everything.java**
```java
// Add new storage lists
private List<PlsqlCode> standaloneFunctionPlsql = new ArrayList<>();
private List<PlsqlCode> standaloneProcedurePlsql = new ArrayList<>();
private List<Function> standaloneFunctionAst = new ArrayList<>();
private List<Procedure> standaloneProcedureAst = new ArrayList<>();

// Add corresponding getters
public List<PlsqlCode> getStandaloneFunctionPlsql() { return standaloneFunctionPlsql; }
public List<PlsqlCode> getStandaloneProcedurePlsql() { return standaloneProcedurePlsql; }
public List<Function> getStandaloneFunctionAst() { return standaloneFunctionAst; }
public List<Procedure> getStandaloneProcedureAst() { return standaloneProcedureAst; }
```

#### **Step 3.2: Add Statistics Methods**
```java
// Add counting methods for statistics
public int getStandaloneFunctionCount() { 
    return standaloneFunctionAst.size(); 
}

public int getStandaloneProcedureCount() { 
    return standaloneProcedureAst.size(); 
}

public int getStandaloneFunctionPlsqlCount() { 
    return standaloneFunctionPlsql.size(); 
}

public int getStandaloneProcedurePlsqlCount() { 
    return standaloneProcedurePlsql.size(); 
}
```

### **Validation**  
- Data can be stored and retrieved correctly
- Statistics methods return accurate counts
- No impact on existing storage functionality

---

## Phase 4: AST Enhancement

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Enhance existing `Function.java` and `Procedure.java` AST classes to support standalone mode.

### **Implementation Steps**

#### **Step 4.1: Add Standalone Support to Function.java**
```java
// Add standalone flag and support
private boolean isStandalone = false;

public boolean isStandalone() { return isStandalone; }
public void setStandalone(boolean standalone) { this.isStandalone = standalone; }

// Update PostgreSQL naming for standalone functions
public String getPostgreFunctionName() {
    if (isStandalone) {
        return schema.toUpperCase() + "." + name.toLowerCase();
    }
    // Existing package-based naming logic
    return getParentPackage().getSchema().toUpperCase() + "." + 
           getParentPackage().getName().toUpperCase() + "_" + name.toLowerCase();
}
```

#### **Step 4.2: Add Standalone Support to Procedure.java**  
```java
// Add identical standalone support as Function.java
private boolean isStandalone = false;

public boolean isStandalone() { return isStandalone; }
public void setStandalone(boolean standalone) { this.isStandalone = standalone; }

// Update PostgreSQL naming for standalone procedures
public String getPostgreProcedureName() {
    if (isStandalone) {
        return schema.toUpperCase() + "." + name.toLowerCase();
    }
    // Existing package-based naming logic
    return getParentPackage().getSchema().toUpperCase() + "." + 
           getParentPackage().getName().toUpperCase() + "_" + name.toLowerCase();
}
```

### **Validation**
- Standalone flag works correctly for both functions and procedures
- PostgreSQL naming distinguishes between standalone and package-based objects
- Existing package-based functionality remains unaffected

---

## Phase 5: Parsing Integration

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Extend the parsing infrastructure to handle standalone functions and procedures.

### **Implementation Steps**

#### **Step 5.1: Add Parser Methods to PlSqlAstMain.java**
```java
// Add standalone function parser
public static Function buildStandaloneFunctionAst(PlsqlCode plsqlCode) throws Exception {
    // Implementation follows buildPackageAst pattern
    // Parse as standalone function, set isStandalone = true
    Function function = parseFunction(plsqlCode);
    function.setStandalone(true);
    return function;
}

// Add standalone procedure parser  
public static Procedure buildStandaloneProcedureAst(PlsqlCode plsqlCode) throws Exception {
    // Implementation follows buildPackageAst pattern
    // Parse as standalone procedure, set isStandalone = true
    Procedure procedure = parseProcedure(plsqlCode);
    procedure.setStandalone(true);
    return procedure;
}
```

#### **Step 5.2: Integrate with MigrationController**
```java
// Add to performParsing() method in Main.java (MigrationController)
if (config.isDoStandaloneFunctions()) {
    for (PlsqlCode code : data.getStandaloneFunctionPlsql()) {
        try {
            Function func = PlSqlAstMain.buildStandaloneFunctionAst(code);
            data.getStandaloneFunctionAst().add(func);
        } catch (Exception e) {
            log.error("Error parsing standalone function: " + code.getName(), e);
        }
    }
}

if (config.isDoStandaloneProcedures()) {
    for (PlsqlCode code : data.getStandaloneProcedurePlsql()) {
        try {
            Procedure proc = PlSqlAstMain.buildStandaloneProcedureAst(code);
            data.getStandaloneProcedureAst().add(proc);
        } catch (Exception e) {
            log.error("Error parsing standalone procedure: " + code.getName(), e);
        }
    }
}
```

### **Validation**
- Standalone functions and procedures parse correctly
- Error handling works for malformed PL/SQL code
- Parsed AST objects have correct standalone flags set

---

## Phase 6: Export Infrastructure

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Create export classes for standalone functions and procedures with two-phase approach.

### **Implementation Steps**

#### **Step 6.1: Create ExportStandaloneFunction.java**
- **File**: `/writing/ExportStandaloneFunction.java`
- **Pattern**: Follow `ExportPackage.java` structure
- **Export Path**: `step3afunctions/` (after package specs, before views)

```java
public class ExportStandaloneFunction {
    private static final FunctionTransformationManager functionManager = new FunctionTransformationManager();
    
    public static void saveStandaloneFunctionsToPostgre(String path, List<Function> functions, Everything data) {
        for (Function function : functions) {
            if (!function.isStandalone()) continue; // Safety check
            
            String fullPathAsString = path + File.separator + 
                                    function.getSchema().toLowerCase() + File.separator + 
                                    "step3afunctions";
            
            String fileName = function.getName().toLowerCase() + ".sql";
            String transformedContent = functionManager.transform(function, data, false);
            
            FileWriter.write(Paths.get(fullPathAsString), fileName, transformedContent);
        }
    }
}
```

#### **Step 6.2: Create ExportStandaloneProcedure.java**  
- **File**: `/writing/ExportStandaloneProcedure.java`
- **Pattern**: Identical structure to ExportStandaloneFunction
- **Export Path**: `step3bprocedures/` (after standalone functions, before views)

```java
public class ExportStandaloneProcedure {
    private static final ProcedureTransformationManager procedureManager = new ProcedureTransformationManager();
    
    public static void saveStandaloneProceduresToPostgre(String path, List<Procedure> procedures, Everything data) {
        for (Procedure procedure : procedures) {
            if (!procedure.isStandalone()) continue; // Safety check
            
            String fullPathAsString = path + File.separator + 
                                    procedure.getSchema().toLowerCase() + File.separator + 
                                    "step3bprocedures";
                                    
            String fileName = procedure.getName().toLowerCase() + ".sql";
            String transformedContent = procedureManager.transform(procedure, data, false);
            
            FileWriter.write(Paths.get(fullPathAsString), fileName, transformedContent);
        }
    }
}
```

### **Validation**
- Export creates files in correct directory structure  
- File naming follows established conventions
- Generated PostgreSQL code is valid and executable

---

## Phase 7: Pipeline Integration

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Integrate standalone functions and procedures into the complete migration pipeline.

### **Implementation Steps**

#### **Step 7.1: Add Extraction to MigrationController**
```java
// Add to performExtraction() method in Main.java
if (config.isDoStandaloneFunctions()) {
    data.getStandaloneFunctionPlsql().addAll(
        StandaloneFunctionExtractor.extract(oracleConn, data.getUserNames())
    );
    log.info("Extracted {} standalone functions", data.getStandaloneFunctionPlsqlCount());
}

if (config.isDoStandaloneProcedures()) {
    data.getStandaloneProcedurePlsql().addAll(
        StandaloneProcedureExtractor.extract(oracleConn, data.getUserNames())
    );
    log.info("Extracted {} standalone procedures", data.getStandaloneProcedurePlsqlCount());
}
```

#### **Step 7.2: Add Export to Export Pipeline**
```java
// Add to performExport() method - after package specs (step 3A), before views (step 4)
if (config.isDoStandaloneFunctions() && config.isDoWritePostgreFiles()) {
    ExportStandaloneFunction.saveStandaloneFunctionsToPostgre(
        targetProjectPathPostgre, data.getStandaloneFunctionAst(), data
    );
    log.info("Exported {} standalone functions to step3afunctions/", data.getStandaloneFunctionCount());
}

if (config.isDoStandaloneProcedures() && config.isDoWritePostgreFiles()) {
    ExportStandaloneProcedure.saveStandaloneProceduresToPostgre(
        targetProjectPathPostgre, data.getStandaloneProcedureAst(), data
    );
    log.info("Exported {} standalone procedures to step3bprocedures/", data.getStandaloneProcedureCount());
}
```

#### **Step 7.3: Add REST Controller Generation**
```java
// Add to REST controller export (if enabled)
if (config.isDoWriteRestControllers()) {
    // Standalone functions
    if (config.isDoStandaloneFunctions() && config.isDoRestControllerFunctions()) {
        ExportRestControllers.generateStandaloneFunctionControllers(
            targetProjectPathJava, javaPackageName, data.getStandaloneFunctionAst(), data
        );
    }
    
    // Standalone procedures  
    if (config.isDoStandaloneProcedures() && config.isDoRestControllerProcedures()) {
        ExportRestControllers.generateStandaloneProcedureControllers(
            targetProjectPathJava, javaPackageName, data.getStandaloneProcedureAst(), data
        );
    }
}
```

### **Validation**
- Integration with existing pipeline works smoothly
- Logging provides clear visibility into processing
- Configuration flags control each step appropriately

---

## Phase 8: Statistics and Frontend Integration

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Add frontend statistics display and configuration options for standalone functions and procedures.

### **Implementation Steps**

#### **Step 8.1: Verify PostgresStatsService.java**
The existing `PostgresStatsService.java` already supports counting functions and procedures:
```java
private int countFunctions(Connection conn) throws SQLException {
    // Uses information_schema.routines WHERE routine_type = 'FUNCTION'
}

private int countProcedures(Connection conn) throws SQLException {
    // Uses information_schema.routines WHERE routine_type = 'PROCEDURE'
}
```
**Note**: These will automatically count standalone functions/procedures once they're migrated to PostgreSQL.

#### **Step 8.2: Add Source Statistics to MigrationController**
```java
// Add to /migration/status endpoint response
public class MigrationStatus {
    // Add new fields for source statistics
    private int sourceStandaloneFunctions;
    private int sourceStandaloneProcedures;
    
    // Add new fields for target statistics (handled by PostgresStatsService)
    // Functions and procedures are already counted separately
}

// Update status collection in getMigrationStatus()
status.setSourceStandaloneFunctions(data.getStandaloneFunctionCount());
status.setSourceStandaloneProcedures(data.getStandaloneProcedureCount());
```

#### **Step 8.3: Frontend Configuration Integration**
**Note**: The user will need to add frontend components for:

1. **Configuration Panel**: Add checkboxes for:
   - "Extract standalone functions" (do.standalone-functions)
   - "Extract standalone procedures" (do.standalone-procedures)

2. **Source Statistics Display**: Add counters for:
   - "Standalone Functions: X extracted, Y parsed"  
   - "Standalone Procedures: X extracted, Y parsed"

3. **Target Statistics Display**: 
   - Functions and procedures are already counted by existing PostgresStatsService
   - No additional frontend changes needed for target statistics

### **Validation**
- Statistics accurately reflect extracted and parsed counts
- Frontend configuration toggles work correctly
- Target database statistics include standalone functions/procedures

---

## Export Directory Structure

### **New Directory Organization**
```
target-project/
├── step1viewspec/          # Existing: View signatures
├── step2objecttypespec/    # Existing: Object type specifications
├── step3packagespec/       # Existing: Package specifications (3A) 
├── step3afunctions/        # NEW: Standalone functions
├── step3bprocedures/       # NEW: Standalone procedures
├── step4viewbody/          # Existing: View implementations
├── step5objecttypebody/    # Existing: Object type bodies
├── step6packagebody/       # Existing: Package bodies (6A)
├── step6indexes/           # Existing: Index creation
├── step7atriggerfunctions/ # Existing: Trigger functions
├── step7btriggerdefinitions/ # Existing: Trigger definitions
└── step8constraints/       # Existing: Constraint creation
```

### **Execution Order**
1. **step3packagespec/** - Package specifications (dependencies for procedures/functions)
2. **step3afunctions/** - Standalone functions (can reference packages)  
3. **step3bprocedures/** - Standalone procedures (can reference packages and functions)
4. **step4viewbody/** - Views (can reference all above)
5. Continue with existing order...

---

## Implementation Timeline

### **Phase Breakdown**
- **Phase 1 (Configuration)**: 30 minutes - Basic setup
- **Phase 2 (Extraction)**: 1-2 hours - Core extraction logic
- **Phase 3 (Data Storage)**: 30 minutes - Everything.java extensions
- **Phase 4 (AST Enhancement)**: 1 hour - Function/Procedure class updates
- **Phase 5 (Parsing)**: 1-2 hours - PlSqlAstMain integration  
- **Phase 6 (Export)**: 1-2 hours - Export class creation
- **Phase 7 (Pipeline Integration)**: 1 hour - Main controller updates
- **Phase 8 (Frontend Integration)**: 1 hour - Statistics and configuration

**Total Estimated Effort**: 6-10 hours

### **Testing Strategy**
- Test each phase independently using existing test patterns
- Ensure standalone functions/procedures don't interfere with package-based ones
- Validate PostgreSQL output can be executed successfully
- Verify REST controller generation works for standalone objects

---

## Risk Mitigation

### **Separation Concerns**
- **Independent Processing**: Standalone functions/procedures have separate extraction, parsing, and export paths
- **No Cross-Contamination**: Existing package-based functionality remains completely unchanged
- **Configuration Isolation**: Each type can be enabled/disabled independently

### **Backward Compatibility**
- All existing functionality preserved
- New features are opt-in via configuration flags
- Default configuration maintains current behavior

### **Rollback Strategy**
- Each phase can be reverted independently
- Configuration flags allow disabling new functionality
- No changes to existing database migration logic

---

## Success Metrics

### **Functional Goals**
- **Extraction**: Successfully retrieve standalone functions/procedures from Oracle
- **Parsing**: Convert PL/SQL source to AST representation
- **Transformation**: Generate equivalent PostgreSQL functions/procedures
- **Export**: Create executable .sql files in correct directory structure
- **Integration**: Seamless operation with existing migration pipeline

### **Quality Goals**
- **Code Quality**: Follow established architectural patterns
- **Performance**: No degradation to existing migration performance
- **Maintainability**: Clear separation and independent testability
- **User Experience**: Intuitive configuration and clear progress feedback

**🎯 End Goal**: Complete support for Oracle standalone functions and procedures with the same quality and features as existing database elements (packages, triggers, constraints, etc.).