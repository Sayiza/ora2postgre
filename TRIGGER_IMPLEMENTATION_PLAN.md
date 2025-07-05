# Oracle Trigger Transformation Implementation Plan

## Overview
This document outlines the detailed implementation plan for adding Oracle trigger transformation capabilities to the Oracle2PostgreSQL migration pipeline. The implementation follows the established patterns used for views, packages, and other database objects.

## Implementation Progress Status

### ✅ COMPLETED PHASES

#### ✅ Phase 1: Configuration & Infrastructure (COMPLETED)
- **Backend Configuration**: Added `do.triggers=true` flag to `application.properties`
- **Config Classes**: Updated `Config.java`, `ConfigurationService.java`, `RuntimeConfiguration.java`
- **Pipeline Integration**: Added trigger processing calls to `Main.java` (extract/parse/export methods)
- **Data Storage**: Added `List<PlsqlCode> triggerPlsql` to `Everything.java`
- **Testing**: Application compiles and starts successfully

#### ✅ Phase 1b: Frontend Configuration (COMPLETED)  
- **Frontend Integration**: Added "Extract triggers" checkbox to `index.html`
- **JavaScript Functions**: Updated configuration save/load functions
- **Testing**: Configuration properly persists and loads

#### ✅ Phase 2: TriggerMetadata and TriggerExtractor (COMPLETED)
- **TriggerMetadata.java**: Complete metadata class with Oracle trigger properties
  - Fields: schema, triggerName, triggerType, triggeringEvent, tableName, tableOwner, status, triggerBody
  - Placeholder PostgreSQL transformation methods (`toPostgreFunctionStub()`, `toPostgreTriggerStub()`)
- **TriggerExtractor.java**: Complete Oracle extraction implementation
  - `extract()` method returns `List<PlsqlCode>` for AST processing
  - Queries `all_triggers` system table for comprehensive metadata
  - Builds complete trigger source code for ANTLR parsing
  - Excludes system triggers (SYS_.*, BIN$.*) 
- **Pipeline Integration**: Updated `Main.java` with actual `TriggerExtractor.extract()` calls
- **Testing**: Application compiles successfully, extraction infrastructure complete

### 📋 PENDING PHASES

#### 📋 Phase 3: Trigger AST Class (NEXT - Ready to start)
**Ready to implement**: Create `Trigger.java` AST class leveraging existing ANTLR grammar

#### 📋 Phase 4: PostgreSQL Transformation Logic
#### 📋 Phase 5: ExportTrigger Class and File Generation  
#### 📋 Phase 6: POST_TRANSFER_TRIGGERS Execution Phase
#### 📋 Phase 7: REST API Integration
#### 📋 Phase 8: Testing and Validation

### Next Session Resume Point: Start Phase 3 - Trigger AST Implementation

## Current State Analysis

### Existing Pipeline Elements (Reference Pattern)
- **Tables**: Metadata + Extractor + Export (step5tables)
- **Views**: Metadata + Extractor + AST + Export (step1viewspec, step4viewbody)
- **Packages**: Extractor + AST + Export (step3packagespec, step6packagebody)
- **Object Types**: Metadata + Extractor + AST + Export (step2objecttypespec)

### Trigger Position in Pipeline
Triggers will be positioned **last** in the execution order (after tables, views, packages, constraints) because:
- Triggers reference tables (must exist first)
- Triggers may call package functions (packages must exist first)
- Triggers may reference views (views must exist first)
- Constraints must be in place before trigger logic

## Implementation Plan

### Phase 1: Configuration and Infrastructure
**Goal**: Add trigger configuration support and basic infrastructure

#### Step 1.1: Configuration Properties
**File**: `src/main/resources/application.properties`
**Changes**:
```properties
# Trigger processing flags
do.trigger=true                     # Master trigger processing flag

# Trigger filtering
do.trigger-system-skip=true        # Skip system-generated triggers
do.trigger-disabled-skip=true      # Skip disabled triggers
```

#### Step 1.2: Update Config.java
**File**: `src/main/java/com/sayiza/oracle2postgre/global/Config.java`
**Changes**:
- Add trigger-related configuration properties
- Add getter methods for trigger flags
- Update configuration validation logic

#### Step 1.3: Pipeline Integration in Main.java
**File**: `src/main/java/com/sayiza/oracle2postgre/Main.java`
**Changes**:
- Add trigger processing to main pipeline (extract/parse/export phases)
- Position trigger processing after packages but before final execution
- Add trigger processing to REST endpoints

#### Step 1.4: Everything.java Context Updates
**File**: `src/main/java/com/sayiza/oracle2postgre/global/Everything.java`
**Changes**:
- Add `List<TriggerMetadata> triggers` field
- Add `List<PlsqlCode> triggerSpecs` field  
- Add `List<Trigger> triggerAsts` field
- Add getter/setter methods
- Add trigger lookup methods

**Validation**: Configuration loading, basic pipeline integration

---

### Phase 2: Metadata and Extraction
**Goal**: Create trigger metadata model and Oracle extraction logic

#### Step 2.1: TriggerMetadata Class
**File**: `src/main/java/com/sayiza/oracle2postgre/oracledb/TriggerMetadata.java`
**Structure**:
```java
public class TriggerMetadata {
    private String schema;              // Trigger owner
    private String triggerName;         // Trigger name
    private String tableName;           // Table/view name
    private String tableOwner;          // Table owner (may differ from trigger owner)
    private String triggerType;         // BEFORE/AFTER/INSTEAD OF/COMPOUND
    private String triggeringEvent;     // INSERT/UPDATE/DELETE/TRUNCATE
    private String columnList;          // For UPDATE OF column_list
    private String whenClause;          // WHEN condition
    private String status;              // ENABLED/DISABLED
    private String description;         // Trigger description
    private String referencing;         // NEW/OLD alias clause
    private String triggerBody;         // PL/SQL code (from all_source)
    private boolean isSystemGenerated;  // System vs user trigger
    private int baseObjectType;         // 1=TABLE, 2=VIEW, etc.
    
    // Methods
    public String toPostgre(Everything context);
    public boolean isBeforeTrigger();
    public boolean isAfterTrigger();
    public boolean isInsteadOfTrigger();
    public boolean isCompoundTrigger();
    public List<String> getTriggeringEvents();
    public String getPostgreTriggerName();
    public String getPostgreTableReference();
}
```

#### Step 2.2: TriggerExtractor Class
**File**: `src/main/java/com/sayiza/oracle2postgre/oracledb/TriggerExtractor.java`
**Functionality**:
- Extract trigger metadata from `all_triggers` system view
- Extract trigger source code from `all_source` 
- Filter system-generated and disabled triggers
- Handle different trigger types and events
- Associate triggers with their target tables

**Key Oracle System Views**:
```sql
-- Main trigger metadata
SELECT trigger_name, table_owner, table_name, trigger_type, 
       triggering_event, column_name, when_clause, status,
       description, referencing_names, trigger_body,
       base_object_type, action_type
FROM all_triggers 
WHERE owner = ?

-- Trigger source code (for complex triggers)
SELECT line, text 
FROM all_source 
WHERE owner = ? AND name = ? AND type = 'TRIGGER'
ORDER BY line
```

#### Step 2.3: Integration with Main Pipeline
**File**: `src/main/java/com/sayiza/oracle2postgre/Main.java`
**Changes**:
- Add trigger extraction to `doExtract()` method
- Call `TriggerExtractor.extract(connection, schemas)`
- Store results in `Everything.triggers`

**Validation**: Trigger metadata extraction from Oracle, data populated in Everything context

---

### Phase 3: AST and Parsing
**Goal**: Parse trigger PL/SQL code into AST and enable transformations

#### Step 3.1: Trigger AST Class
**File**: `src/main/java/com/sayiza/oracle2postgre/plsql/ast/Trigger.java`
**Structure**:
```java
public class Trigger extends PlSqlAst {
    private String triggerName;
    private String tableName;
    private String triggerType;        // BEFORE/AFTER/INSTEAD OF
    private List<String> events;       // INSERT, UPDATE, DELETE
    private String whenClause;
    private String referencingClause;
    private List<String> updateColumns; // For UPDATE OF
    private Statement triggerBody;      // Parsed trigger body
    
    // Transformation methods
    @Override
    public String toPostgre(Everything everything);
    public String toPostgreTriggerFunction(Everything everything);
    public String toPostgreTriggerDefinition(Everything everything);
    
    // Helper methods
    public String getPostgreEventList();
    public String getPostgreTimingClause();
    public String getPostgreWhenClause(Everything everything);
    public boolean needsRowLevelTrigger();
}
```

#### Step 3.2: AST Parsing Integration
**File**: `src/main/java/com/sayiza/oracle2postgre/plsql/PlSqlAstMain.java`
**Changes**:
- Add trigger parsing support in `processPlsqlCode()`
- Handle trigger-specific syntax elements
- Create `Trigger` AST nodes from parsed content

#### Step 3.3: Trigger Body Processing
**File**: `src/main/java/com/sayiza/oracle2postgre/plsql/ast/tools/TriggerProcessor.java` (new)
**Functionality**:
- Parse trigger timing (BEFORE/AFTER/INSTEAD OF)
- Parse triggering events (INSERT/UPDATE/DELETE)
- Handle WHEN clause transformation
- Process OLD/NEW references → PostgreSQL equivalents
- Transform Oracle-specific trigger features

#### Step 3.4: Integration with Main Pipeline
**File**: `src/main/java/com/sayiza/oracle2postgre/Main.java`
**Changes**:
- Add trigger parsing to `doParse()` method
- Convert `TriggerMetadata` → `PlsqlCode` → `Trigger` AST
- Store results in `Everything.triggerAsts`

**Validation**: Oracle trigger PL/SQL code successfully parsed into AST, basic transformations working

---

### Phase 4: PostgreSQL Transformation
**Goal**: Transform Oracle trigger syntax to PostgreSQL equivalents

#### Step 4.1: Oracle to PostgreSQL Trigger Mapping
**Transformations**:

| Oracle Feature | PostgreSQL Equivalent |
|----------------|----------------------|
| `BEFORE INSERT` | `BEFORE INSERT` |
| `AFTER UPDATE` | `AFTER UPDATE` |
| `INSTEAD OF` | `INSTEAD OF` (views only) |
| `FOR EACH ROW` | `FOR EACH ROW` |
| `FOR EACH STATEMENT` | `FOR EACH STATEMENT` |
| `WHEN (condition)` | `WHEN (condition)` |
| `:NEW.column` | `NEW.column` |
| `:OLD.column` | `OLD.column` |
| `INSERTING` | `TG_OP = 'INSERT'` |
| `UPDATING` | `TG_OP = 'UPDATE'` |
| `DELETING` | `TG_OP = 'DELETE'` |
| `UPDATING('column')` | `TG_OP = 'UPDATE' AND OLD.column IS DISTINCT FROM NEW.column` |

#### Step 4.2: Trigger Function Generation
**PostgreSQL Pattern**:
```sql
-- Generate trigger function
CREATE OR REPLACE FUNCTION schema.triggername_function()
RETURNS TRIGGER AS $$
BEGIN
    -- Transformed trigger body
    IF TG_OP = 'INSERT' THEN
        -- INSERT logic
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        -- UPDATE logic  
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        -- DELETE logic
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
CREATE TRIGGER triggername
    BEFORE INSERT OR UPDATE OR DELETE ON schema.tablename
    FOR EACH ROW
    WHEN (condition)
    EXECUTE FUNCTION schema.triggername_function();
```

#### Step 4.3: Complex Trigger Handling
- **Compound Triggers**: Split into separate BEFORE/AFTER triggers
- **Multiple Events**: Generate single function with TG_OP branching
- **UPDATE OF columns**: Transform to column comparison logic
- **WHEN clauses**: Convert Oracle syntax to PostgreSQL
- **Autonomous Transactions**: Warning comments (not supported)

**Validation**: Oracle triggers successfully transformed to valid PostgreSQL syntax

---

### Phase 5: Export and File Generation
**Goal**: Generate PostgreSQL trigger DDL files

#### Step 5.1: ExportTrigger Class
**File**: `src/main/java/com/sayiza/oracle2postgre/writing/ExportTrigger.java`
**Structure**:
```java
public class ExportTrigger {
    public static void saveTriggerFunctions(String basePath, Everything everything);
    public static void saveTriggerDefinitions(String basePath, Everything everything);
    public static void saveAllTriggers(String basePath, Everything everything);
    
    private static void generateTriggerFunction(Trigger trigger, Everything everything);
    private static void generateTriggerDefinition(Trigger trigger, Everything everything);
    private static String getTriggerFilePath(String basePath, String schema, String triggerName);
}
```

#### Step 5.2: File Organization Strategy
**Directory Structure**:
```
target-project/postgre/autoddl/
├── schema1/
│   ├── step1viewspec/         # Views (empty)
│   ├── step2objecttypespec/   # Object types
│   ├── step3packagespec/      # Package specs
│   ├── step4viewbody/         # Views (full)
│   ├── step5tables/           # Tables
│   ├── step6packagebody/      # Package bodies
│   └── step7triggers/         # Triggers (NEW)
│       ├── functions/         # Trigger functions
│       └── definitions/       # Trigger definitions
```

#### Step 5.3: Two-Phase Trigger Export
1. **Trigger Functions** (`functions/`): CREATE FUNCTION statements
2. **Trigger Definitions** (`definitions/`): CREATE TRIGGER statements

**Rationale**: Functions must exist before triggers that reference them

#### Step 5.4: Integration with Main Pipeline
**File**: `src/main/java/com/sayiza/oracle2postgre/Main.java`
**Changes**:
- Add trigger export to `doExport()` method
- Call `ExportTrigger.saveAllTriggers()`
- Position after package export but before execution

**Validation**: PostgreSQL trigger files generated in correct directory structure

---

### Phase 6: Execution Integration
**Goal**: Execute generated trigger DDL in correct order

#### Step 6.1: PostgreSQL Execution Order
**Current Order**:
1. PRE_TRANSFER_TYPES: Object types, schemas
2. PRE_TRANSFER_TABLES: Tables, basic structures  
3. POST_TRANSFER: Views, constraints, packages

**New Order with Triggers**:
1. PRE_TRANSFER_TYPES: Object types, schemas
2. PRE_TRANSFER_TABLES: Tables, basic structures
3. POST_TRANSFER: Views, constraints, packages
4. **POST_TRANSFER_TRIGGERS**: Trigger functions, then trigger definitions

#### Step 6.2: PostgreSQLExecutor Updates
**File**: `src/main/java/com/sayiza/oracle2postgre/writing/PostgreSQLExecutor.java`
**Changes**:
- Add `POST_TRANSFER_TRIGGERS` execution phase
- Execute trigger functions before trigger definitions
- Handle trigger execution errors gracefully
- Add trigger-specific logging

#### Step 6.3: Execution Phase Logic
```java
// Execute trigger functions first
executeFilesInDirectory(basePath + "/step7triggers/functions/");

// Execute trigger definitions second  
executeFilesInDirectory(basePath + "/step7triggers/definitions/");
```

#### Step 6.4: Error Handling
- **Missing Tables**: Skip triggers for non-existent tables
- **Missing Functions**: Report function dependency errors
- **Syntax Errors**: Log and continue with remaining triggers
- **Rollback Support**: Track created triggers for cleanup

**Validation**: Triggers successfully created in PostgreSQL database in correct execution order

---

### Phase 7: REST API Integration
**Goal**: Add trigger support to REST endpoints

#### Step 7.1: REST Endpoint Updates
**File**: `src/main/java/com/sayiza/oracle2postgre/Main.java`
**Changes**:
- Add trigger processing to `/migration/extract` endpoint
- Add trigger processing to `/migration/parse` endpoint  
- Add trigger processing to `/migration/export` endpoint
- Include trigger statistics in `/migration/status` endpoint

#### Step 7.2: Migration Status Response
**Updates**:
```json
{
  "triggers": {
    "total": 25,
    "before_triggers": 8,
    "after_triggers": 12,
    "instead_of_triggers": 3,
    "compound_triggers": 2,
    "parsed": 23,
    "export_errors": 2
  }
}
```

#### Step 7.3: Job Progress Tracking
- Add trigger processing steps to job progress
- Track trigger extraction, parsing, and export phases
- Include trigger execution in job completion

**Validation**: REST API properly reports trigger processing status and progress

---

### Phase 8: Testing and Validation
**Goal**: Comprehensive testing of trigger transformation

#### Step 8.1: Unit Tests
**Files**:
- `TriggerMetadataTest.java` - Metadata model validation
- `TriggerExtractorTest.java` - Oracle extraction testing
- `TriggerAstTest.java` - AST parsing and transformation
- `TriggerExportTest.java` - PostgreSQL file generation

#### Step 8.2: Integration Tests
**Files**:
- `TriggerPipelineIntegrationTest.java` - End-to-end pipeline
- `TriggerPostgreSQLExecutionTest.java` - Database execution
- `TriggerRestApiTest.java` - REST endpoint validation

#### Step 8.3: Test Coverage Areas
- **Oracle Trigger Types**: BEFORE, AFTER, INSTEAD OF, COMPOUND
- **Triggering Events**: INSERT, UPDATE, DELETE, TRUNCATE
- **Row vs Statement Level**: FOR EACH ROW vs FOR EACH STATEMENT
- **WHEN Clauses**: Complex conditional logic
- **OLD/NEW References**: Data access patterns
- **Multiple Events**: Combined INSERT/UPDATE/DELETE triggers
- **Error Conditions**: Invalid syntax, missing tables, etc.

#### Step 8.4: Test Data
**Oracle Test Triggers**:
```sql
-- Simple BEFORE INSERT trigger
CREATE OR REPLACE TRIGGER trg_employee_before_insert
BEFORE INSERT ON employees
FOR EACH ROW
BEGIN
    :NEW.created_date := SYSDATE;
END;

-- Complex UPDATE trigger with WHEN clause
CREATE OR REPLACE TRIGGER trg_salary_audit
AFTER UPDATE OF salary ON employees
FOR EACH ROW
WHEN (NEW.salary != OLD.salary)
BEGIN
    INSERT INTO salary_audit (employee_id, old_salary, new_salary, change_date)
    VALUES (:NEW.employee_id, :OLD.salary, :NEW.salary, SYSDATE);
END;

-- INSTEAD OF trigger for view
CREATE OR REPLACE TRIGGER trg_employee_view_insert
INSTEAD OF INSERT ON employee_view
FOR EACH ROW
BEGIN
    INSERT INTO employees (name, department) 
    VALUES (:NEW.name, :NEW.department);
END;
```

**Validation**: All test cases pass, trigger transformations produce correct PostgreSQL DDL

---

## Implementation Timeline

### Step-by-Step Approach
1. **Phase 1**: Configuration and Infrastructure (1-2 steps)
2. **Phase 2**: Metadata and Extraction (2-3 steps)
3. **Phase 3**: AST and Parsing (3-4 steps)
4. **Phase 4**: PostgreSQL Transformation (2-3 steps)
5. **Phase 5**: Export and File Generation (2-3 steps)
6. **Phase 6**: Execution Integration (1-2 steps)
7. **Phase 7**: REST API Integration (1-2 steps)
8. **Phase 8**: Testing and Validation (ongoing)

### Manual Testing Points
After each phase, manual testing should verify:
- Configuration changes work correctly
- Oracle extraction produces expected metadata
- AST parsing handles trigger syntax
- PostgreSQL transformation is syntactically correct
- File generation creates proper directory structure
- Database execution creates working triggers
- REST API reports accurate status

## Expected Challenges and Solutions

### Challenge 1: Oracle Trigger Complexity
**Issue**: Oracle triggers have many advanced features
**Solution**: Implement core features first, add advanced features incrementally

### Challenge 2: PostgreSQL Trigger Differences
**Issue**: PostgreSQL trigger model differs from Oracle
**Solution**: Document unsupported features, provide transformation guidelines

### Challenge 3: Execution Order Dependencies
**Issue**: Triggers may depend on packages, views, etc.
**Solution**: Position triggers last in execution order

### Challenge 4: Error Handling
**Issue**: Trigger creation may fail due to missing dependencies
**Solution**: Graceful error handling with detailed logging

## Success Criteria

### Functional Requirements
- ✅ Extract all Oracle triggers with complete metadata
- ✅ Parse Oracle trigger PL/SQL into AST successfully
- ✅ Transform common Oracle trigger patterns to PostgreSQL
- ✅ Generate syntactically correct PostgreSQL trigger DDL
- ✅ Execute triggers in PostgreSQL without errors
- ✅ Maintain trigger functionality equivalence

### Non-Functional Requirements
- ✅ Follow existing architecture patterns consistently
- ✅ Maintain performance standards of current pipeline
- ✅ Provide comprehensive error handling and logging
- ✅ Include complete test coverage
- ✅ Support REST API integration
- ✅ Enable step-by-step manual testing

## Conclusion

This implementation plan provides a comprehensive, step-by-step approach to adding Oracle trigger transformation to the existing Oracle2PostgreSQL migration pipeline. By following the established patterns for views and packages, the implementation will integrate seamlessly with the existing architecture while providing robust trigger migration capabilities.

The plan emphasizes incremental development with manual testing points after each phase, ensuring quality and correctness at every step. The modular approach allows for gradual implementation and testing, reducing risk and enabling early detection of issues.