# PL/SQL to PostgreSQL Transpilation Plan

**Project Status**: Core transpilation infrastructure complete with significant feature coverage
**Architecture**: PostgreSQL-first approach with direct toPostgre() chains and manager-strategy patterns
**Current Test Coverage**: 199+ tests passing across all implemented features

---

## ✅ **COMPLETED FEATURES** (Production Ready)

### **Core Control Flow** ✅
- **IF/ELSIF/ELSE Statements** - Complete Oracle→PostgreSQL transpilation with nested condition support
- **WHILE Loop Statements** - Full implementation with condition evaluation and statement blocks
- **Loop Statements** - Plain LOOP...END LOOP structure with EXIT statement support
- **Exception Handling** - 85% complete with full EXCEPTION block support, Oracle→PostgreSQL exception mapping, missing only RAISE statements

### **SQL DML Operations** ✅  
- **INSERT Statements** - Complete with schema resolution, column lists, and synonym support
- **UPDATE Statements** - Full SET clause and WHERE condition support with schema prefixing
- **DELETE Statements** - Complete with complex WHERE clauses and automatic schema resolution
- **SELECT INTO Statements** - Single and multiple column selection with variable assignment

### **Cursor Infrastructure** ✅
- **Cursor Declarations** - Correct PostgreSQL syntax (`cursor_name CURSOR FOR SELECT...`)
- **Cursor Loop Transformation** - Automatic OPEN→LOOP→FETCH→CLOSE to PostgreSQL FOR loop conversion
- **Cursor Attributes** - Complete support for `%FOUND`, `%NOTFOUND`, `%ROWCOUNT`, `%ISOPEN`
- **Parameterized Cursors** - Full support with parameter type specifications

### **Package Features** ✅
- **Package Variables** - Session-specific temporary tables approach with direct table access
- **Function/Procedure Stubs** - Declaration-before-implementation pattern for dependency resolution
- **Package Merging** - Spec/body integration with proper phase separation

### **Expression Architecture** ✅
- **Semantic AST Classes** - Complete grammar hierarchy with `MultisetExpression`, `RelationalExpression`, `CompoundExpression`, etc.
- **Visitor Delegation** - Fixed parsing chain routing expressions through proper AST classes
- **Complex Expression Support** - Enables analytical functions, Oracle operators, and advanced SQL constructs

---

## 🎯 **IMMEDIATE PRIORITIES** (Next Implementation Phase)

### **1. Exception Handling Completion** (HIGH PRIORITY)
**Status**: 85% complete, missing RAISE statements
**Effort**: Low (1-2 days)
**Impact**: HIGH - Completes basic error handling for production readiness

**Implementation Required**:
- Create `RaiseStatement.java` AST class
- Add `visitRaise_statement()` to `PlSqlAstBuilder.java`
- Support `RAISE exception_name;` and `RAISE;` (re-raise) patterns
- Test coverage for RAISE in exception handlers

**Expected Outcome**: Complete exception handling infrastructure

### **2. Record Types and %ROWTYPE Support** (HIGH PRIORITY)
**Status**: Not implemented
**Effort**: Medium (1 week)
**Impact**: HIGH - Enables complex data structure handling

**Implementation Required**:
- Create `RecordType.java` and `RecordTypeDeclaration.java` AST classes
- Implement `%ROWTYPE` attribute resolution using `Everything.java` schema lookup
- PostgreSQL composite type generation for Oracle record types
- Record field access transformation (`record.field` → `record_var.field`)

**Oracle Pattern** → **PostgreSQL Implementation**:
```sql
-- Oracle
TYPE emp_rec IS RECORD (
    emp_id NUMBER,
    emp_name VARCHAR2(100)
);
v_emp emp_rec;
v_emp.emp_id := 123;

-- PostgreSQL
CREATE TYPE emp_rec AS (
    emp_id NUMERIC,
    emp_name TEXT
);
DECLARE v_emp emp_rec;
v_emp.emp_id := 123;
```

### **3. Collection Types (TABLE OF/VARRAY)** (MEDIUM PRIORITY)
**Status**: Not implemented
**Effort**: Medium (1 week)
**Impact**: MEDIUM - Required for array-based logic patterns

**Implementation Required**:
- Create `CollectionType.java` for TABLE OF and VARRAY declarations
- PostgreSQL array type mapping (`TABLE OF VARCHAR2(100)` → `TEXT[]`)
- Collection method transformation (`.COUNT`, `.FIRST`, `.LAST`, `.NEXT`, `.PRIOR`)
- BULK COLLECT basic support for array population

---

## 📋 **PLANNED FEATURES** (Future Development)

### **Phase 1: Advanced Variable Support** (3-4 weeks)
**Goal**: Complete Oracle variable and type system support

#### **%TYPE Attributes**
- Column type reference resolution (`variable table.column%TYPE`)
- Integration with `Everything.java` for schema and table lookup
- Support for package variable type references

#### **Package Type Support**
- Types declared in package specifications
- Schema-level type creation in PostgreSQL
- Type visibility and scope management

#### **Advanced Package Features**
- Package initialization blocks → PostgreSQL initialization functions
- Package constants with compile-time evaluation
- Cross-package type and variable references

### **Phase 2: Advanced SQL Features** (4-5 weeks)
**Goal**: Support complex Oracle SQL constructs

#### **Common Table Expressions (WITH Clause)**
- Recursive CTE support for Oracle hierarchical patterns
- Multiple CTE definitions in single query
- Integration with existing SELECT statement infrastructure

#### **Analytical Functions**
- Window function support (`ROW_NUMBER()`, `RANK()`, `DENSE_RANK()`)
- `OVER` clause parsing and transformation
- Oracle-specific analytical functions mapping

#### **MERGE Statements**
- Oracle MERGE → PostgreSQL UPSERT transformation
- `INSERT ... ON CONFLICT` generation for simple MERGE patterns
- Complex MERGE with multiple WHEN clauses

### **Phase 3: Oracle-Specific Features** (5-6 weeks)
**Goal**: Handle Oracle-specific constructs with PostgreSQL alternatives

#### **CONNECT BY Hierarchical Queries**
- `START WITH ... CONNECT BY` → `WITH RECURSIVE` transformation
- Level pseudo-column support
- Hierarchical functions (`SYS_CONNECT_BY_PATH`, `LEVEL`)

#### **PIVOT/UNPIVOT Operations**
- Dynamic PIVOT → PostgreSQL crosstab function transformation
- Static PIVOT → CASE statement generation
- UNPIVOT transformation strategies

#### **Advanced PL/SQL Features**
- `BULK COLLECT` → array-based data collection
- `FORALL` → PostgreSQL array processing loops
- Autonomous transactions → separate function calls with transaction control

---

## 🏗️ **IMPLEMENTATION STRATEGY**

### **Development Approach**
1. **Follow Established Patterns**: Use existing AST classes as templates (`IfStatement.java`, `InsertStatement.java`)
2. **Leverage Infrastructure**: Build on `OracleFunctionMapper.java`, `TypeConverter.java`, and `Everything.java` context
3. **Test-Driven Development**: Create comprehensive test suites for each feature
4. **Incremental Enhancement**: Each phase should increase overall transpilation success rate

### **Architecture Guidelines**
- **Direct toPostgre() Chains**: For parse tree elements (statements, expressions, variables)
- **Manager-Strategy Pattern**: For main exported objects (packages, functions, procedures)
- **Everything Context**: Pass schema and context resolution throughout transformation chains
- **PostgreSQL-First**: Generate idiomatic PostgreSQL code, not literal Oracle translations

### **Quality Metrics**
- **Current Success Rate**: ~60-70% for typical Oracle PL/SQL functions/procedures
- **Phase 1 Target**: 75-80% success rate with record types and collections
- **Phase 2 Target**: 85-90% success rate with advanced SQL features
- **Phase 3 Target**: 95%+ success rate for most Oracle PL/SQL patterns

---

## 📊 **FEATURE PRIORITY MATRIX**

| Feature | Business Impact | Implementation Effort | Current Status | Priority |
|---------|----------------|----------------------|----------------|----------|
| RAISE Statements | High | Low | 15% missing | Immediate |
| Record Types | High | Medium | Not started | Phase 1 |
| Collection Types | Medium | Medium | Not started | Phase 1 |
| %TYPE Attributes | Medium | Low | Not started | Phase 1 |
| Package Types | Medium | Medium | Not started | Phase 1 |
| Analytical Functions | Low | Medium | Not started | Phase 2 |
| MERGE Statements | Low | High | Not started | Phase 2 |
| CONNECT BY | Low | High | Not started | Phase 3 |

---

## 🎯 **SUCCESS CRITERIA**

### **Immediate (Next Sprint)**
- ✅ Exception handling 100% complete with RAISE statements
- ✅ Record type declarations and %ROWTYPE support functional
- ✅ Test coverage maintains 199+ passing tests

### **Phase 1 Complete (1-2 months)**
- ✅ Oracle variable and type system 90% supported
- ✅ Complex data structures (records, collections) fully functional
- ✅ Package feature set complete for enterprise applications

### **Phase 2 Complete (3-4 months)**
- ✅ Advanced SQL features enable complex reporting function migration
- ✅ Analytical and window functions fully supported
- ✅ MERGE statement patterns working for data integration logic

### **Long-term Goal (6+ months)**
- ✅ 95%+ transpilation success rate for enterprise Oracle PL/SQL codebases
- ✅ Oracle-specific constructs have viable PostgreSQL alternatives
- ✅ Production-ready migration tool for complex business logic

---

## 🔧 **TESTING AND VALIDATION**

### **Current Test Architecture**
- **Unit Tests**: Individual AST class functionality verification
- **Integration Tests**: Complete PL/SQL block transpilation testing
- **Manual Testing**: End-to-end migration pipeline validation

### **Test Coverage Requirements**
- **Each New Feature**: Minimum 4 test scenarios (simple, complex, edge cases, integration)
- **Regression Testing**: All existing 199+ tests must continue passing
- **PostgreSQL Validation**: Generated code must be syntactically correct and executable

### **Validation Strategy**
- **Syntax Validation**: All generated PostgreSQL code must parse correctly
- **Semantic Validation**: Logic equivalence between Oracle and PostgreSQL versions
- **Performance Testing**: Generated PostgreSQL should perform comparably to Oracle originals

---

*This plan represents the current state of a mature PL/SQL transpilation system with strong foundations. The focus is now on completing advanced Oracle features and achieving enterprise-grade transpilation success rates.*