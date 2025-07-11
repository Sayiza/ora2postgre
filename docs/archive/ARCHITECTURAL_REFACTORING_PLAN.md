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

### **Status**: ✅ **COMPLETED**

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

### **Status**: ✅ **COMPLETED**

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

### **Status**: ✅ **COMPLETED**

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

### **Status**: ✅ **COMPLETED**

### **Scope**
Standardize package and function transformation using strategy pattern.

### **Entry Criteria**
- Phase 5 completed successfully
- All tests passing

### **Detailed Implementation Steps**

#### **Step 6.1: Analyze Current Package/Function Architecture**
- ✅ **Current Files Analyzed**:
  - `/plsql/ast/OraclePackage.java` - `toPostgre()` method iterating over functions/procedures
  - `/plsql/ast/Function.java` - `toPostgre()` method for PostgreSQL function generation
  - `/plsql/ast/Procedure.java` - `toPostgre()` method for PostgreSQL procedure generation
  - `/writing/ExportPackage.java` - Direct calls to `package.toPostgre()`

#### **Step 6.2: Create Strategy Interfaces**
- ✅ **Files Created**:
  - `/plsql/ast/tools/strategies/PackageTransformationStrategy.java`
  - `/plsql/ast/tools/strategies/FunctionTransformationStrategy.java`
  - `/plsql/ast/tools/strategies/ProcedureTransformationStrategy.java`
- ✅ **Pattern**: Following established interface pattern with supports(), transform(), getStrategyName(), getPriority()

#### **Step 6.3: Create Strategy Implementations**
- ✅ **Files Created**:
  - `/plsql/ast/tools/strategies/StandardPackageStrategy.java`
  - `/plsql/ast/tools/strategies/StandardFunctionStrategy.java`
  - `/plsql/ast/tools/strategies/StandardProcedureStrategy.java`
- ✅ **Content**: Extracted logic from original `toPostgre()` methods with same functionality

#### **Step 6.4: Create Transformation Managers**
- ✅ **Files Created**:
  - `/plsql/ast/tools/managers/PackageTransformationManager.java`
  - `/plsql/ast/tools/managers/FunctionTransformationManager.java`
  - `/plsql/ast/tools/managers/ProcedureTransformationManager.java`
- ✅ **Pattern**: Following established manager pattern for strategy selection and execution

#### **Step 6.5: Update Export Layer**
- ✅ **File Updated**: `/writing/ExportPackage.java`
- ✅ **Changes**: Replace `package.toPostgre(data, specOnly)` with `packageManager.transform(package, data, specOnly)`

#### **Step 6.6: Maintain Backward Compatibility**
- ✅ **Files Updated**:
  - `/plsql/ast/OraclePackage.java` - Delegate to `PackageTransformationManager`, marked as `@Deprecated`
  - `/plsql/ast/Function.java` - Delegate to `FunctionTransformationManager`, marked as `@Deprecated`
  - `/plsql/ast/Procedure.java` - Delegate to `ProcedureTransformationManager`, marked as `@Deprecated`
- ✅ **Added**: Missing getter methods (`getParentType()`, `getParentPackage()`) to Function and Procedure classes

### **Validation & Testing**
- ✅ Maven compilation successful with new strategy pattern
- ✅ All 162 tests passing
- ✅ Backward compatibility maintained through deprecated methods

### **Exit Criteria**
- ✅ Package/function transformation uses strategy pattern
- ✅ All tests passing
- ✅ Backward compatibility maintained
- ✅ Original `toPostgre()` methods marked as deprecated

### **Estimated Effort**: 2-3 hours ✅ **ACTUAL**: ~2 hours

---

## Phase 7: Folder Structure Reorganization

### **Status**: ✅ **COMPLETED**

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

### **Validation & Testing**
- ✅ All files moved to appropriate subfolders (strategies/, managers/, transformers/, helpers/)
- ✅ Package declarations updated for all moved files  
- ✅ Import statements fixed across entire codebase
- ✅ Maven compilation successful with new structure
- ✅ All 162 tests passing

### **Exit Criteria**
- ✅ Clean, organized folder structure
- ✅ All files in logical locations
- ✅ All tests passing

### **Estimated Effort**: 1-2 hours ✅ **ACTUAL**: ~1.5 hours

---

## Phase 8: Cleanup and Optimization

### **Status**: ✅ **COMPLETED**

### **Scope**
Remove deprecated methods, optimize imports, and finalize the refactored architecture.

### **Entry Criteria**
- Phase 7 completed successfully
- All transformation logic using strategy pattern

### **Implementation Steps**

#### **Step 8.1: Analyze Deprecated Methods** ✅
- ✅ Identified deprecated `toPostgre()` methods in metadata and AST classes
- ✅ Confirmed these methods properly delegate to transformation managers
- ✅ Verified tests still use some deprecated methods as compatibility layer
- ✅ **Decision**: Keep deprecated methods since they provide proper delegation and compatibility

#### **Step 8.2: Create Unified TransformationStrategy Interface** ✅
- ✅ **File Created**: `/plsql/ast/tools/strategies/TransformationStrategy.java`
- ✅ **Content**: Base interface with common contract (supports(), getStrategyName(), getPriority())
- ✅ **Updated**: All existing strategy interfaces now extend TransformationStrategy
- ✅ **Benefit**: Consistent architecture across all transformation types

#### **Step 8.3: Optimize Export Layer and Remove Redundant Code** ✅
- ✅ **Analysis**: Export classes already use transformation managers properly
- ✅ **Imports**: Cleaned up redundant method declarations in strategy interfaces
- ✅ **Standardization**: All Export classes follow consistent pattern with managers
- ✅ **Code Quality**: Removed duplicate methods now inherited from base interface

#### **Step 8.4: Comprehensive Testing and Validation** ✅
- ✅ **Test Results**: All 162 tests passing successfully
- ✅ **Compilation**: Clean compilation with no errors
- ✅ **Architecture**: Unified strategy pattern implemented across all transformations
- ✅ **Backward Compatibility**: All existing functionality preserved

### **Validation & Testing**
- ✅ All 162 tests passing successfully
- ✅ Clean Maven compilation with no errors
- ✅ Unified TransformationStrategy interface created and adopted
- ✅ All strategy interfaces properly extend base interface
- ✅ Export layer properly standardized with transformation managers

### **Exit Criteria**
- ✅ Deprecated methods analyzed (kept as compatibility layer with proper delegation)
- ✅ Unified transformation interface in place
- ✅ All tests passing
- ✅ Architecture documentation updated in this plan

### **Estimated Effort**: 1-2 hours ✅ **ACTUAL**: ~1 hour

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

## ✅ **REFACTORING COMPLETE - ALL PHASES SUCCESSFUL**

### **Final Architecture Achievement**
All 8 phases of the architectural refactoring have been successfully completed. The ora2postgre project now has:

1. **✅ Unified Strategy Pattern**: All database elements (tables, constraints, triggers, views, packages, functions, procedures, indexes) use consistent strategy-based transformation
2. **✅ Clean Architecture**: Organized folder structure with logical separation (/strategies/, /managers/, /transformers/, /helpers/)
3. **✅ Base Interface**: TransformationStrategy<T> provides common contract for all transformation types
4. **✅ Export Standardization**: All Export classes use transformation managers consistently  
5. **✅ Backward Compatibility**: Deprecated methods properly delegate to new strategy managers
6. **✅ Test Coverage**: All 162 tests continue to pass with new architecture

### **Transformation Completed**
- **From**: Mixed transformation patterns (self-contained, strategy, hybrid)
- **To**: Unified strategy pattern across all database element types
- **Result**: Consistent, maintainable, and extensible transformation architecture

### **Developer Experience**
- **Clear Patterns**: All transformation follows predictable strategy→manager→export flow
- **Easy Extension**: New transformation strategies can be added following established patterns
- **Code Quality**: Reduced duplication, better separation of concerns, organized structure

**🎉 Project successfully refactored to achieve architectural symmetry and maintainability goals!**