# ARCHITECTURAL_REFACTORING_PLAN.md

## Oracle to PostgreSQL Architectural Symmetry Refactoring Plan

### Overview
This plan provides a step-by-step roadmap to refactor the ora2postgre project from its current mixed transformation patterns to a unified, symmetrical strategy-based architecture. The refactoring can be executed incrementally across multiple sessions while maintaining backward compatibility and functionality.

### Current Architecture Assessment (Baseline)

#### **Completed (Session 2025-07-09)**
- ✅ **PostgreSQLIndexDDL relocation** from `/writing/` to `/plsql/ast/tools/`
- ✅ **Import statement updates** across 9 affected files
- ✅ **Test validation** - all 162 tests passing
- ✅ **Architectural analysis** complete

#### **Current Transformation Patterns**
1. **Self-Contained Pattern** (43 classes): `TableMetadata.toPostgre()`, `ConstraintMetadata.toPostgre()`, etc.
2. **Strategy Pattern** (11 classes): `IndexMigrationStrategy` with multiple implementations
3. **Hybrid Pattern** (10+ classes): Export classes with varying complexity

#### **Identified Inconsistencies**
- Mixed transformation approaches across database elements
- Helper classes scattered across packages
- Export complexity varies from simple (tables) to complex (indexes, constraints)
- PostgreSQLIndexDDL was misplaced in `/writing/` instead of `/plsql/ast/tools/`

---

## Refactoring Strategy: Incremental Migration to Strategy Pattern

### **Phase Structure**
Each phase is designed for independent execution with clear entry/exit criteria, allowing seamless resumption across sessions.

---

## Phase 2: Table Transformation Strategy Implementation

### **Status**: ✅ **COMPLETED**

### **Scope**
Transform table transformation from `TableMetadata.toPostgre()` to strategy pattern.

### **Entry Criteria**
- Phase 1 completed (PostgreSQLIndexDDL relocation)
- All tests passing
- Clean git state

### **Detailed Implementation Steps**

#### **Step 2.1: Create Table Strategy Interface**
- **File**: `/plsql/ast/tools/strategies/TableTransformationStrategy.java`
- **Dependencies**: None
- **Content**: Generic transformation interface for table operations
```java
public interface TableTransformationStrategy {
    boolean supports(TableMetadata table);
    String transform(TableMetadata table, Everything context);
    String getStrategyName();
    int getPriority();
}
```

#### **Step 2.2: Create Table Strategy Implementations**
- **Files**:
  - `/plsql/ast/tools/strategies/StandardTableStrategy.java`
  - `/plsql/ast/tools/strategies/PartitionedTableStrategy.java` (if needed)
- **Dependencies**: Step 2.1
- **Content**: Extract logic from `TableMetadata.toPostgre()`

#### **Step 2.3: Create Table Transformation Manager**
- **File**: `/plsql/ast/tools/managers/TableTransformationManager.java`
- **Dependencies**: Steps 2.1, 2.2
- **Content**: Orchestrates strategy selection and execution
- **Pattern**: Follow `IndexMigrationStrategyManager` pattern

#### **Step 2.4: Update Export Layer**
- **File**: `/writing/ExportTable.java`
- **Dependencies**: Step 2.3
- **Changes**: Replace `table.toPostgre(data)` with `tableManager.transform(table, data)`

#### **Step 2.5: Maintain Backward Compatibility**
- **File**: `/oracledb/TableMetadata.java`
- **Dependencies**: Step 2.4
- **Changes**: Keep `toPostgre()` method but delegate to strategy manager
- **Note**: Mark as `@Deprecated` for future removal

### **Validation & Testing**
- Run `mvn clean test` after each step
- Ensure all 162+ tests continue to pass
- Verify table generation produces identical output

### **Exit Criteria**
- Table transformation uses strategy pattern
- All tests passing
- Backward compatibility maintained
- `TableMetadata.toPostgre()` marked as deprecated

### **Estimated Effort**: 1-2 hours

---

## Phase 3: Constraint Transformation Strategy Implementation

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Transform constraint transformation from `ConstraintMetadata.toPostgre()` to strategy pattern.

### **Entry Criteria**
- Phase 2 completed successfully
- All tests passing

### **Detailed Implementation Steps**

#### **Step 3.1: Create Constraint Strategy Interface**
- **File**: `/plsql/ast/tools/strategies/ConstraintTransformationStrategy.java`
- **Dependencies**: Table strategy pattern established
- **Content**: Generic constraint transformation interface

#### **Step 3.2: Create Constraint Strategy Implementations**
- **Files**:
  - `/plsql/ast/tools/strategies/PrimaryKeyConstraintStrategy.java`
  - `/plsql/ast/tools/strategies/ForeignKeyConstraintStrategy.java`
  - `/plsql/ast/tools/strategies/UniqueConstraintStrategy.java`
  - `/plsql/ast/tools/strategies/CheckConstraintStrategy.java`
- **Dependencies**: Step 3.1
- **Content**: Extract logic from `ConstraintMetadata.toPostgre()` and `toPostgreAlterTableDDL()`

#### **Step 3.3: Create Constraint Transformation Manager**
- **File**: `/plsql/ast/tools/managers/ConstraintTransformationManager.java`
- **Dependencies**: Steps 3.1, 3.2
- **Content**: Handle dependency ordering for foreign keys

#### **Step 3.4: Update Export Layer**
- **File**: `/writing/ExportConstraint.java`
- **Dependencies**: Step 3.3
- **Changes**: Replace direct `toPostgre()` calls with manager usage
- **Complexity**: Higher than tables due to dependency management

#### **Step 3.5: Maintain Backward Compatibility**
- **File**: `/oracledb/ConstraintMetadata.java`
- **Dependencies**: Step 3.4
- **Changes**: Delegate to strategy manager, mark as deprecated

### **Validation & Testing**
- Focus on constraint dependency ordering
- Verify foreign key constraints create in correct order
- Ensure all constraint types generate correctly

### **Exit Criteria**
- Constraint transformation uses strategy pattern
- Dependency ordering preserved
- All tests passing
- Backward compatibility maintained

### **Estimated Effort**: 2-3 hours

---

## Phase 4: Trigger Transformation Strategy Implementation

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Unify trigger transformation logic from `TriggerMetadata.toPostgre()`, `Trigger.toPostgre()`, and `TriggerTransformer` utilities.

### **Entry Criteria**
- Phase 3 completed successfully
- All tests passing

### **Detailed Implementation Steps**

#### **Step 4.1: Analyze Current Trigger Architecture**
- **Current Files**:
  - `/oracledb/TriggerMetadata.java` - `toPostgre()` method
  - `/plsql/ast/Trigger.java` - `toPostgre()` method
  - `/plsql/ast/tools/TriggerTransformer.java` - transformation utilities
  - `/writing/ExportTrigger.java` - two-phase export (functions + definitions)

#### **Step 4.2: Create Trigger Strategy Interface**
- **File**: `/plsql/ast/tools/strategies/TriggerTransformationStrategy.java`
- **Dependencies**: Established strategy patterns
- **Content**: Interface supporting two-phase trigger transformation

#### **Step 4.3: Create Trigger Strategy Implementations**
- **Files**:
  - `/plsql/ast/tools/strategies/BeforeAfterTriggerStrategy.java`
  - `/plsql/ast/tools/strategies/InsteadOfTriggerStrategy.java`
  - `/plsql/ast/tools/strategies/CompoundTriggerStrategy.java` (if needed)
- **Dependencies**: Step 4.2
- **Content**: Consolidate logic from metadata, AST, and transformer classes

#### **Step 4.4: Move TriggerTransformer to Transformers Package**
- **From**: `/plsql/ast/tools/TriggerTransformer.java`
- **To**: `/plsql/ast/tools/transformers/TriggerTransformer.java`
- **Dependencies**: Step 4.3
- **Updates**: Integrate with strategy classes

#### **Step 4.5: Create Trigger Transformation Manager**
- **File**: `/plsql/ast/tools/managers/TriggerTransformationManager.java`
- **Dependencies**: Steps 4.2-4.4
- **Content**: Handle two-phase transformation (functions + definitions)

#### **Step 4.6: Update Export Layer**
- **File**: `/writing/ExportTrigger.java`
- **Dependencies**: Step 4.5
- **Changes**: Use transformation manager while preserving two-phase export

#### **Step 4.7: Maintain Backward Compatibility**
- **Files**: 
  - `/oracledb/TriggerMetadata.java`
  - `/plsql/ast/Trigger.java`
- **Dependencies**: Step 4.6
- **Changes**: Delegate to strategy manager, mark as deprecated

### **Validation & Testing**
- Verify two-phase trigger export still works
- Ensure trigger functions generate before definitions
- Test complex trigger transformation cases

### **Exit Criteria**
- Trigger transformation unified under strategy pattern
- Two-phase export functionality preserved
- All tests passing
- TriggerTransformer relocated to transformers package

### **Estimated Effort**: 2-3 hours

---

## Phase 5: View Transformation Strategy Implementation

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Standardize view transformation from current hybrid approach to strategy pattern.

### **Entry Criteria**
- Phase 4 completed successfully
- All tests passing

### **Detailed Implementation Steps**

#### **Step 5.1: Analyze Current View Architecture**
- **Current Files**:
  - `/oracledb/ViewMetadata.java` - `toPostgre()` method
  - `/writing/ExportView.java` - hybrid metadata + AST transformation
  - Various view-related AST classes with `toPostgre()` methods

#### **Step 5.2: Create View Strategy Interface**
- **File**: `/plsql/ast/tools/strategies/ViewTransformationStrategy.java`
- **Dependencies**: Established strategy patterns
- **Content**: Interface supporting view DDL transformation

#### **Step 5.3: Create View Strategy Implementations**
- **Files**:
  - `/plsql/ast/tools/strategies/SimpleViewStrategy.java`
  - `/plsql/ast/tools/strategies/ComplexViewStrategy.java` (with subqueries)
  - `/plsql/ast/tools/strategies/MaterializedViewStrategy.java` (if applicable)
- **Dependencies**: Step 5.2
- **Content**: Extract logic from ViewMetadata and ExportView

#### **Step 5.4: Create View Transformation Manager**
- **File**: `/plsql/ast/tools/managers/ViewTransformationManager.java`
- **Dependencies**: Steps 5.2, 5.3
- **Content**: Coordinate view metadata and AST transformation

#### **Step 5.5: Update Export Layer**
- **File**: `/writing/ExportView.java`
- **Dependencies**: Step 5.4
- **Changes**: Simplify to use transformation manager

#### **Step 5.6: Maintain Backward Compatibility**
- **File**: `/oracledb/ViewMetadata.java`
- **Dependencies**: Step 5.5
- **Changes**: Delegate to strategy manager

### **Exit Criteria**
- View transformation uses strategy pattern
- Hybrid approach eliminated
- All tests passing

### **Estimated Effort**: 1-2 hours

---

## Phase 6: Package/Function Transformation Strategy Implementation

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Standardize package and function transformation using strategy pattern.

### **Entry Criteria**
- Phase 5 completed successfully
- All tests passing

### **Implementation Steps**
[Similar structure to previous phases - focused on package/function AST transformation]

### **Estimated Effort**: 2-3 hours

---

## Phase 7: Folder Structure Reorganization

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Reorganize `/plsql/ast/tools/` into logical subfolders with all strategy files properly categorized.

### **Entry Criteria**
- Phases 2-6 completed successfully
- All transformation logic uses strategy pattern

### **Detailed Implementation Steps**

#### **Step 7.1: Create Subfolder Structure**
```
/plsql/ast/tools/
├── strategies/
│   ├── TableTransformationStrategy.java + implementations
│   ├── ConstraintTransformationStrategy.java + implementations
│   ├── TriggerTransformationStrategy.java + implementations
│   ├── ViewTransformationStrategy.java + implementations
│   ├── PackageTransformationStrategy.java + implementations
│   └── IndexMigrationStrategy.java + implementations (existing)
├── managers/
│   ├── TableTransformationManager.java
│   ├── ConstraintTransformationManager.java
│   ├── TriggerTransformationManager.java
│   ├── ViewTransformationManager.java
│   ├── PackageTransformationManager.java
│   └── IndexMigrationStrategyManager.java (existing)
├── transformers/
│   ├── PostgreSQLIndexDDL.java (moved in Phase 1)
│   ├── TriggerTransformer.java (moved in Phase 4)
│   ├── OracleFunctionMapper.java (existing)
│   └── TypeConverter.java (existing)
└── helpers/
    ├── StatementDeclarationCollector.java (existing)
    ├── RestControllerGenerator.java (existing)
    ├── SimpleDtoGenerator.java (existing)
    └── ToExportJava.java, ToExportPostgre.java (existing)
```

#### **Step 7.2: Move Files to Subfolders**
- Move each file to appropriate subfolder
- Update package declarations
- Update all import statements across the codebase

#### **Step 7.3: Update Build and Test**
- Ensure Maven compilation works with new structure
- Run full test suite

### **Exit Criteria**
- Clean, organized folder structure
- All files in logical locations
- All tests passing

### **Estimated Effort**: 1-2 hours

---

## Phase 8: Cleanup and Optimization

### **Status**: ⏹️ **NOT STARTED**

### **Scope**
Remove deprecated methods, optimize imports, and finalize the refactored architecture.

### **Entry Criteria**
- Phase 7 completed successfully
- All transformation logic using strategy pattern

### **Implementation Steps**

#### **Step 8.1: Remove Deprecated Methods**
- Remove `toPostgre()` methods from metadata classes
- Remove `@Deprecated` annotations
- Update any remaining direct calls

#### **Step 8.2: Optimize Export Layer**
- Standardize all Export classes to use same pattern
- Remove redundant code
- Optimize imports

#### **Step 8.3: Create Unified Transformation Interface**
- **File**: `/plsql/ast/tools/strategies/TransformationStrategy.java`
- **Content**: Base interface for all transformation strategies
- **Purpose**: Provide common contract for all transformations

#### **Step 8.4: Final Testing and Documentation**
- Run comprehensive test suite
- Update architecture documentation
- Create migration notes for future developers

### **Exit Criteria**
- No deprecated methods remaining
- Unified transformation interface in place
- All tests passing
- Documentation updated

### **Estimated Effort**: 1-2 hours

---

## Session Management Guidelines

### **Starting a Refactoring Session**
1. **Check current phase status** in this document
2. **Verify entry criteria** for the phase
3. **Run tests** to ensure clean starting state
4. **Create git branch** for the phase (optional but recommended)

### **During a Session**
1. **Update phase status** to IN PROGRESS
2. **Complete steps sequentially** - don't skip ahead
3. **Run tests after each major step**
4. **Update this document** with any discoveries or changes

### **Ending a Session**
1. **Update phase status** (COMPLETED, PAUSED, or IN PROGRESS)
2. **Note current step** in the detailed implementation
3. **Ensure clean git state**
4. **Update any issues discovered** in this document

### **Resuming a Session**
1. **Review previous session notes** in this document
2. **Verify current state** matches documented status
3. **Continue from noted step** in the phase

---

## Risk Mitigation

### **Backward Compatibility**
- Keep `toPostgre()` methods during transition
- Use `@Deprecated` annotations
- Only remove deprecated methods in Phase 8

### **Testing Strategy**
- Run tests after each major step
- No step should break existing functionality
- Each phase can be reverted independently

### **Rollback Plan**
- Each phase is git-branch safe
- Clear entry/exit criteria allow rollback to any phase
- Deprecated methods provide fallback during transition

---

## Success Metrics

### **Quantitative Goals**
- **Consistent Architecture**: All database elements use strategy pattern
- **Code Organization**: All transformation files in logical locations
- **Maintainability**: Unified interfaces for all transformation types
- **Test Coverage**: All 162+ tests continue to pass

### **Qualitative Goals**
- **Developer Experience**: Clear, predictable transformation patterns
- **Extensibility**: Easy to add new transformation strategies
- **Code Quality**: Reduced duplication, better separation of concerns

---

## Estimated Total Effort

- **Phase 2 (Tables)**: 1-2 hours
- **Phase 3 (Constraints)**: 2-3 hours  
- **Phase 4 (Triggers)**: 2-3 hours
- **Phase 5 (Views)**: 1-2 hours
- **Phase 6 (Packages)**: 2-3 hours
- **Phase 7 (Organization)**: 1-2 hours
- **Phase 8 (Cleanup)**: 1-2 hours

**Total: 10-17 hours** across multiple sessions

---

## Next Session Recommendation

**Start with Phase 2 (Table Transformation Strategy Implementation)**
- Well-defined scope
- Lowest risk (simple transformation logic)
- Establishes pattern for subsequent phases
- Clear validation criteria

**Session Goal**: Complete Phase 2 Steps 2.1-2.5 and validate with full test suite.