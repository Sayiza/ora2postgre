# Oracle Constraint Migration - IMPLEMENTATION PLAN

## Overview
This document outlines the implementation plan for Oracle constraint migration capabilities in the Oracle2PostgreSQL migration pipeline. The plan builds upon the existing constraint infrastructure and extends it to support comprehensive constraint migration.

## Current State Analysis

### Existing Constraint Infrastructure ✅
- **ConstraintMetadata.java**: Basic constraint metadata model with name, type, and columns
- **TableExtractor.java**: Oracle constraint extraction (PRIMARY KEY only)
- **TableMetadata.java**: PostgreSQL constraint DDL generation (PRIMARY KEY only)
- **PostgresExecuter.java**: Multi-phase execution pipeline with POST_TRANSFER phase

### Current Constraint Support ✅
- **Primary Key Constraints**: Complete extraction, transformation, and execution
- **Basic Infrastructure**: Extensible design ready for additional constraint types
- **Execution Framework**: POST_TRANSFER phase available for constraint execution

### Missing Constraint Features ❌
- **Foreign Key Constraints**: No extraction, transformation, or execution
- **Check Constraints**: No support for Oracle check constraints
- **Unique Constraints**: No unique constraint handling
- **Not Null Constraints**: Basic column-level support only
- **Constraint Export**: No dedicated constraint file generation
- **Constraint Execution**: No integration with POST_TRANSFER phase

## Architecture Goals

### PostgreSQL-First Approach
- **Constraint Separation**: Generate constraints as separate DDL files for execution after data transfer
- **Execution Order**: Tables → Data → Constraints for optimal foreign key handling
- **Clean DDL**: PostgreSQL-native constraint syntax with proper identifier quoting

### Extensible Design
- **Constraint Types**: Support for all Oracle constraint types with PostgreSQL equivalents
- **Phased Implementation**: Incremental development allowing manual testing between phases
- **Error Handling**: Graceful constraint creation failures with detailed logging

## Implementation Plan

### Phase 1: Enhanced Constraint Extraction Infrastructure
**Goal**: Extend existing constraint extraction to support all Oracle constraint types

#### Step 1.1: Enhanced ConstraintMetadata Class
**File**: `src/main/java/me/christianrobert/ora2postgre/oracledb/ConstraintMetadata.java`
**Changes**:
```java
public class ConstraintMetadata {
    // Existing fields
    private String constraintName;
    private String constraintType;         // P, R, U, C, N
    private List<String> columnNames;
    
    // New fields for comprehensive constraint support
    private String referencedSchema;       // For foreign keys
    private String referencedTable;        // For foreign keys  
    private List<String> referencedColumns; // For foreign keys
    private String deleteRule;             // CASCADE, SET NULL, RESTRICT
    private String updateRule;             // CASCADE, SET NULL, RESTRICT
    private String checkCondition;         // For check constraints
    private String status;                 // ENABLED, DISABLED
    private boolean deferrable;            // Deferrable constraint
    private boolean initiallyDeferred;     // Initially deferred
    private String indexName;              // Associated index
    
    // Enhanced methods
    public boolean isPrimaryKey();
    public boolean isForeignKey();
    public boolean isUniqueConstraint();
    public boolean isCheckConstraint();
    public boolean isNotNullConstraint();
    public String getConstraintTypeName();
    public String toPostgreConstraintDDL();
    public String toPostgreAlterTableDDL(String schemaName, String tableName);
}
```

#### Step 1.2: Enhanced TableExtractor Class
**File**: `src/main/java/me/christianrobert/ora2postgre/oracledb/TableExtractor.java`
**Changes**:
- Extend `fetchConstraints()` method to extract all constraint types
- Add comprehensive Oracle constraint queries
- Handle constraint dependencies and references

**Oracle Constraint Extraction Queries**:
```sql
-- Main constraint metadata
SELECT ac.constraint_name, ac.constraint_type, ac.status,
       ac.deferrable, ac.deferred, ac.validated, ac.invalid,
       ac.r_owner, ac.r_constraint_name, ac.delete_rule,
       ac.search_condition, ac.index_name
FROM all_constraints ac 
WHERE ac.owner = ? AND ac.table_name = ?
AND ac.constraint_type IN ('P', 'R', 'U', 'C')
ORDER BY ac.constraint_type, ac.constraint_name;

-- Constraint column details
SELECT acc.column_name, acc.position
FROM all_cons_columns acc
WHERE acc.owner = ? AND acc.table_name = ? AND acc.constraint_name = ?
ORDER BY acc.position;

-- Foreign key referenced columns
SELECT acc.column_name, acc.position
FROM all_cons_columns acc
JOIN all_constraints ac ON acc.constraint_name = ac.r_constraint_name
WHERE ac.owner = ? AND ac.constraint_name = ?
ORDER BY acc.position;
```

#### Step 1.3: Constraint Type Mapping
**Oracle → PostgreSQL Constraint Mapping**:
```java
// Constraint type mapping
Map<String, String> CONSTRAINT_TYPE_MAP = Map.of(
    "P", "PRIMARY KEY",
    "R", "FOREIGN KEY", 
    "U", "UNIQUE",
    "C", "CHECK"
);

// Delete rule mapping
Map<String, String> DELETE_RULE_MAP = Map.of(
    "CASCADE", "CASCADE",
    "SET NULL", "SET NULL",
    "RESTRICT", "RESTRICT",
    "NO ACTION", "NO ACTION"
);
```

**Validation**: All Oracle constraint types properly extracted and stored in enhanced ConstraintMetadata

---

### Phase 2: Constraint File Generation and Export
**Goal**: Generate separate PostgreSQL constraint DDL files for execution after data transfer

#### Step 2.1: ExportConstraint Class
**File**: `src/main/java/me/christianrobert/ora2postgre/writing/ExportConstraint.java`
**Structure**:
```java
public class ExportConstraint {
    public static void saveConstraints(String basePath, Everything everything);
    public static void savePrimaryKeyConstraints(String basePath, Everything everything);
    public static void saveForeignKeyConstraints(String basePath, Everything everything);
    public static void saveUniqueConstraints(String basePath, Everything everything);
    public static void saveCheckConstraints(String basePath, Everything everything);
    
    private static void generateConstraintDDL(ConstraintMetadata constraint, Everything everything);
    private static String getConstraintFilePath(String basePath, String schema, String constraintType);
    private static void validateConstraintReferences(ConstraintMetadata constraint, Everything everything);
}
```

#### Step 2.2: Constraint File Organization Strategy
**Directory Structure**:
```
target-project/postgre/autoddl/
├── schema1/
│   ├── step1viewspec/         # Views (empty)
│   ├── step2objecttypespec/   # Object types
│   ├── step3packagespec/      # Package specs
│   ├── step4viewbody/         # Views (full)
│   ├── step5tables/           # Tables (WITHOUT constraints)
│   ├── step6packagebody/      # Package bodies
│   ├── step7atriggerfunctions/ # Trigger functions
│   ├── step7btriggerdefinitions/ # Trigger definitions
│   └── step8constraints/       # Constraints (NEW)
│       ├── primary_keys/       # Primary key constraints
│       ├── foreign_keys/       # Foreign key constraints
│       ├── unique_constraints/ # Unique constraints
│       └── check_constraints/  # Check constraints
```

#### Step 2.3: Constraint DDL Generation
**Primary Key Constraints**:
```sql
-- Generated DDL format
ALTER TABLE schema.table_name 
ADD CONSTRAINT constraint_name PRIMARY KEY (column1, column2);
```

**Foreign Key Constraints**:
```sql
-- Generated DDL format
ALTER TABLE schema.table_name 
ADD CONSTRAINT constraint_name FOREIGN KEY (column1, column2) 
REFERENCES referenced_schema.referenced_table (ref_column1, ref_column2)
ON DELETE CASCADE ON UPDATE RESTRICT;
```

**Unique Constraints**:
```sql
-- Generated DDL format
ALTER TABLE schema.table_name 
ADD CONSTRAINT constraint_name UNIQUE (column1, column2);
```

**Check Constraints**:
```sql
-- Generated DDL format (with Oracle → PostgreSQL condition transformation)
ALTER TABLE schema.table_name 
ADD CONSTRAINT constraint_name CHECK (transformed_condition);
```

#### Step 2.4: Integration with Main Pipeline
**File**: `src/main/java/me/christianrobert/ora2postgre/Main.java`
**Changes**:
- Add constraint export to `doExport()` method
- Call `ExportConstraint.saveConstraints()` after table export
- Position constraint export after data transfer preparation

**Validation**: PostgreSQL constraint files generated in correct directory structure with proper DDL syntax

---

### Phase 3: Constraint Execution Integration
**Goal**: Execute generated constraint DDL in correct order after data transfer

#### Step 3.1: PostgreSQL Execution Order Enhancement
**Current Order**:
1. PRE_TRANSFER_TYPES: Object types, schemas
2. PRE_TRANSFER_TABLES: Tables (WITHOUT constraints)
3. POST_TRANSFER: Views, packages
4. POST_TRANSFER_TRIGGERS: Triggers

**New Order with Constraints**:
1. PRE_TRANSFER_TYPES: Object types, schemas
2. PRE_TRANSFER_TABLES: Tables (WITHOUT constraints)
3. POST_TRANSFER: Views, packages
4. **POST_TRANSFER_CONSTRAINTS**: Constraints (NEW)
5. POST_TRANSFER_TRIGGERS: Triggers

#### Step 3.2: PostgresExecuter Updates
**File**: `src/main/java/me/christianrobert/ora2postgre/postgre/PostgresExecuter.java`
**Changes**:
- Add `POST_TRANSFER_CONSTRAINTS` execution phase
- Execute constraints in dependency order: PRIMARY KEY → UNIQUE → CHECK → FOREIGN KEY
- Handle constraint execution errors gracefully
- Add constraint-specific logging and statistics

#### Step 3.3: Constraint Execution Logic
```java
// Execute constraints in dependency order
private void executeConstraints(String basePath) {
    // 1. Primary key constraints (no dependencies)
    executeFilesInDirectory(basePath + "/step8constraints/primary_keys/");
    
    // 2. Unique constraints (no dependencies)
    executeFilesInDirectory(basePath + "/step8constraints/unique_constraints/");
    
    // 3. Check constraints (no dependencies)
    executeFilesInDirectory(basePath + "/step8constraints/check_constraints/");
    
    // 4. Foreign key constraints (depend on target tables and their primary keys)
    executeFilesInDirectory(basePath + "/step8constraints/foreign_keys/");
}
```

#### Step 3.4: Constraint Error Handling
**Error Handling Strategies**:
- **Missing Tables**: Skip foreign key constraints for non-existent tables
- **Missing Columns**: Log column reference errors and skip constraint
- **Constraint Violations**: Log data violations and provide remediation suggestions
- **Duplicate Constraints**: Detect and skip duplicate constraint definitions
- **Rollback Support**: Track created constraints for cleanup on failure

**Validation**: Constraints successfully created in PostgreSQL database in correct execution order

---

### Phase 4: Advanced Constraint Features
**Goal**: Support complex Oracle constraint features and transformations

#### Step 4.1: Check Constraint Transformation
**Oracle → PostgreSQL Check Constraint Mapping**:
```java
// Transform Oracle check constraint conditions
public String transformCheckCondition(String oracleCondition) {
    String postgresCondition = oracleCondition;
    
    // Transform Oracle functions to PostgreSQL equivalents
    postgresCondition = postgresCondition.replace("SYSDATE", "CURRENT_TIMESTAMP");
    postgresCondition = postgresCondition.replace("USER", "CURRENT_USER");
    postgresCondition = postgresCondition.replace("UPPER(", "UPPER(");
    postgresCondition = postgresCondition.replace("LOWER(", "LOWER(");
    
    // Transform Oracle operators
    postgresCondition = postgresCondition.replace("||", "||");  // Concatenation (same)
    postgresCondition = postgresCondition.replace("LIKE", "LIKE");  // Pattern matching (same)
    
    // Transform Oracle data types in conditions
    postgresCondition = postgresCondition.replace("VARCHAR2", "TEXT");
    postgresCondition = postgresCondition.replace("NUMBER", "NUMERIC");
    
    return postgresCondition;
}
```

#### Step 4.2: Foreign Key Constraint Enhancements
**Advanced Foreign Key Features**:
- **Composite Foreign Keys**: Multi-column foreign key support
- **Self-Referencing Foreign Keys**: Table references to itself
- **Cross-Schema Foreign Keys**: References to tables in different schemas
- **Cascading Actions**: ON DELETE/UPDATE CASCADE, SET NULL, RESTRICT
- **Deferred Constraints**: PostgreSQL equivalent of Oracle deferred constraints

#### Step 4.3: Constraint Dependency Resolution
**Dependency Resolution Logic**:
```java
public class ConstraintDependencyResolver {
    public List<ConstraintMetadata> resolveExecutionOrder(List<ConstraintMetadata> constraints) {
        // 1. Build dependency graph
        Map<String, Set<String>> dependencies = buildDependencyGraph(constraints);
        
        // 2. Topological sort for execution order
        List<ConstraintMetadata> orderedConstraints = topologicalSort(constraints, dependencies);
        
        // 3. Handle circular dependencies
        handleCircularDependencies(orderedConstraints);
        
        return orderedConstraints;
    }
}
```

#### Step 4.4: Constraint Validation and Testing
**Validation Framework**:
- **Constraint Syntax Validation**: Verify generated DDL syntax
- **Dependency Validation**: Check referenced tables and columns exist
- **Data Validation**: Verify existing data meets constraint requirements
- **Performance Impact**: Analyze constraint creation performance

**Validation**: Complex Oracle constraints properly transformed and executed in PostgreSQL

---

### Phase 5: Configuration and Control
**Goal**: Add comprehensive configuration options for constraint migration

#### Step 5.1: Configuration Properties
**File**: `src/main/resources/application.properties`
**Changes**:
```properties
# Constraint processing flags
do.constraints=true                    # Master constraint processing flag
do.constraints.primary-keys=true       # Primary key constraints
do.constraints.foreign-keys=true       # Foreign key constraints  
do.constraints.unique=true             # Unique constraints
do.constraints.check=true              # Check constraints
do.constraints.not-null=true           # Not null constraints

# Constraint processing options
constraints.defer-foreign-keys=true    # Create foreign keys after data transfer
constraints.validate-data=true         # Validate data before creating constraints
constraints.skip-on-error=true         # Continue on constraint creation errors
constraints.drop-existing=false        # Drop existing constraints before creation
```

#### Step 5.2: Everything.java Context Updates
**File**: `src/main/java/me/christianrobert/ora2postgre/global/Everything.java`
**Changes**:
- Add constraint-specific lookup methods
- Add constraint validation methods
- Add constraint dependency resolution methods

#### Step 5.3: REST API Integration
**File**: `src/main/java/me/christianrobert/ora2postgre/Main.java`
**Changes**:
- Add constraint processing to REST endpoints
- Include constraint statistics in status responses
- Add constraint-specific error reporting

**Validation**: Configuration properly controls constraint processing with appropriate defaults

---

### Phase 6: Testing and Validation Framework
**Goal**: Comprehensive testing of constraint migration functionality

#### Step 6.1: Unit Tests
**Files**:
- `ConstraintMetadataTest.java` - Enhanced metadata model validation
- `ConstraintExtractionTest.java` - Oracle constraint extraction testing
- `ConstraintExportTest.java` - PostgreSQL DDL generation testing
- `ConstraintExecutionTest.java` - Database execution testing

#### Step 6.2: Integration Tests
**Files**:
- `ConstraintPipelineIntegrationTest.java` - End-to-end constraint migration
- `ConstraintDependencyTest.java` - Constraint dependency resolution
- `ConstraintValidationTest.java` - Constraint validation framework

#### Step 6.3: Test Coverage Areas
**Primary Key Constraints**:
- Single column primary keys
- Composite primary keys
- Named vs system-generated primary keys

**Foreign Key Constraints**:
- Simple foreign keys
- Composite foreign keys
- Self-referencing foreign keys
- Cross-schema foreign keys
- Cascading delete/update actions

**Unique Constraints**:
- Single column unique constraints
- Composite unique constraints
- Named vs system-generated unique constraints

**Check Constraints**:
- Simple value checks
- Complex condition checks
- Function-based checks
- Multi-column checks

#### Step 6.4: Test Data
**Oracle Test Constraints**:
```sql
-- Primary key
ALTER TABLE employees ADD CONSTRAINT pk_employees PRIMARY KEY (employee_id);

-- Foreign key with cascade
ALTER TABLE orders ADD CONSTRAINT fk_orders_customer 
FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE;

-- Unique constraint
ALTER TABLE employees ADD CONSTRAINT uk_employees_email UNIQUE (email);

-- Check constraint
ALTER TABLE employees ADD CONSTRAINT chk_employees_salary 
CHECK (salary > 0 AND salary < 1000000);

-- Complex check constraint
ALTER TABLE orders ADD CONSTRAINT chk_orders_status 
CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED'));
```

**Validation**: All test cases pass, constraint transformations produce correct PostgreSQL DDL

---

## Implementation Timeline

### Step-by-Step Manual Testing Approach
1. **Phase 1**: Enhanced Constraint Extraction (2-3 steps)
   - Manual Test: Verify all Oracle constraint types are extracted
2. **Phase 2**: Constraint File Generation (2-3 steps)
   - Manual Test: Check generated PostgreSQL DDL syntax
3. **Phase 3**: Constraint Execution Integration (2-3 steps)
   - Manual Test: Verify constraints are created in correct order
4. **Phase 4**: Advanced Constraint Features (3-4 steps)
   - Manual Test: Test complex constraint scenarios
5. **Phase 5**: Configuration and Control (1-2 steps)
   - Manual Test: Verify configuration flags work correctly
6. **Phase 6**: Testing and Validation (ongoing)
   - Manual Test: Run comprehensive test suite

### Manual Testing Checkpoints
After each phase, manual testing should verify:
- **Phase 1**: Oracle constraint extraction includes all types (P, R, U, C)
- **Phase 2**: Generated PostgreSQL DDL files are syntactically correct
- **Phase 3**: Constraints are created in PostgreSQL in correct order
- **Phase 4**: Complex constraint scenarios work correctly
- **Phase 5**: Configuration flags properly control constraint processing
- **Phase 6**: Full test suite passes with comprehensive coverage

## Expected Challenges and Solutions

### Challenge 1: Foreign Key Dependency Order
**Issue**: Foreign key constraints must be created after referenced tables exist
**Solution**: Implement constraint dependency resolution and execution ordering

### Challenge 2: Data Validation Requirements
**Issue**: Existing data may not meet constraint requirements
**Solution**: Implement pre-constraint data validation with remediation suggestions

### Challenge 3: Check Constraint Transformation
**Issue**: Oracle check constraint syntax differs from PostgreSQL
**Solution**: Implement comprehensive Oracle → PostgreSQL condition transformation

### Challenge 4: Constraint Execution Failures
**Issue**: Constraint creation may fail due to data or dependency issues
**Solution**: Graceful error handling with detailed logging and recovery options

### Challenge 5: Performance Impact
**Issue**: Creating constraints on large tables may take significant time
**Solution**: Implement constraint creation progress tracking and optimization

## Success Criteria

### Functional Requirements
- Extract all Oracle constraint types (P, R, U, C) with complete metadata
- Generate syntactically correct PostgreSQL constraint DDL
- Execute constraints in PostgreSQL in correct dependency order
- Handle constraint creation errors gracefully
- Maintain constraint functionality equivalence
- Support complex constraint scenarios (composite keys, check conditions, etc.)

### Non-Functional Requirements
- Follow existing architecture patterns consistently
- Maintain performance standards of current pipeline
- Provide comprehensive error handling and logging
- Include complete test coverage
- Support REST API integration
- Enable step-by-step manual testing
- Maintain PostgreSQL-first architecture principles

## Architecture Integration

### Consistency with Existing Patterns
The constraint implementation follows established patterns:
- **Metadata Model**: Similar to `TableMetadata`, `ViewMetadata` structure
- **Extraction Logic**: Similar to `TableExtractor`, `ViewExtractor` approach
- **Export Strategy**: Similar to `ExportTable`, `ExportView` file generation
- **Execution Integration**: Similar to trigger execution phases

### PostgreSQL-First Philosophy
- **Constraint Separation**: Constraints as separate DDL files for optimal execution
- **Clean PostgreSQL DDL**: Native PostgreSQL constraint syntax
- **Execution Order**: Tables → Data → Constraints for foreign key compatibility
- **Error Isolation**: Constraint failures don't stop migration pipeline

## Implementation Ready
This comprehensive plan provides a structured approach to implementing full Oracle constraint migration capabilities. The phased approach with manual testing checkpoints ensures quality and correctness at every step, while the architecture maintains consistency with existing patterns and the PostgreSQL-first philosophy.