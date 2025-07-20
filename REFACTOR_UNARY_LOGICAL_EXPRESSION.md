# UnaryLogicalExpression Refactoring Plan

## Problem Statement

The current `UnaryLogicalExpression` class implementation does not align with the ANTLR grammar definition, causing confusion and complexity in expression parsing.

### Grammar Definition (PlSqlParser.g4)
```antlr
unary_logical_expression
    : NOT? multiset_expression unary_logical_operation?
```

### Current Implementation Issues
1. **Type Mismatch**: `UnaryLogicalExpression` uses generic `Expression` instead of `MultisetExpression`
2. **Complex Workarounds**: Builder creates wrapper expressions instead of proper AST hierarchy
3. **Transformation Confusion**: Multiple code paths for similar functionality
4. **Architecture Violation**: Not following the grammar-driven AST design

## Impact Analysis

### Files Requiring Changes
- `UnaryLogicalExpression.java` - Core class refactoring
- `VisitUnaryLogicalExpression.java` - Builder simplification
- Any code referencing `UnaryLogicalExpression.getMultisetExpression()`
- Tests that depend on current structure

### Risk Assessment
- **HIGH**: Expression parsing could break during transition
- **MEDIUM**: Compilation errors in dependent code
- **LOW**: Tests may need updates

## Refactoring Phases

### Phase 1: Analysis (SAFE - No Code Changes) ✅ COMPLETED
**Goal**: Understand current usage patterns across codebase

**Tasks**:
- [x] Search all references to `UnaryLogicalExpression`
- [x] Identify direct usage of `getMultisetExpression()` method
- [x] Map current transformation flows
- [x] Document all test cases using this class

**Deliverables**:
- Complete usage report
- List of all dependent files
- Current test coverage analysis

**Risk**: None (analysis only)

**Analysis Results**:
- **28 files** reference `UnaryLogicalExpression`
- **NO direct usage** of `getMultisetExpression()` method (only defined in class)
- **NO instanceof checks** for `UnaryLogicalExpression`
- **Two main usage patterns**:
  1. Text-based constructor: `new UnaryLogicalExpression("text")` (tests, fallbacks)
  2. Full constructor: `new UnaryLogicalExpression(hasNot, multisetExpr, logicalOperation)` (main builder)
- **Major usage locations**:
  - **Builder functions**: `VisitUnaryLogicalExpression.java`, `VisitExpression.java`, `VisitLogicalExpression.java`
  - **Test files**: 13 test files use text-based constructor for simple expressions
  - **HtpStatement.java**: Uses text-based constructor
  - **Visitor pattern**: `PlSqlAstVisitor.java` includes visit method

### Phase 2: Core Class Update (BREAKING)
**Goal**: Update `UnaryLogicalExpression` to use `MultisetExpression`

**Tasks**:
- [ ] Change field type from `Expression` to `MultisetExpression`
- [ ] Update constructors to accept `MultisetExpression`
- [ ] Update getter method signature
- [ ] Keep old methods temporarily with `@Deprecated` annotation
- [ ] Update `toPostgre()` method to delegate properly

**Expected Compilation Errors**: All code using the old getter method

**Rollback Plan**: Git revert this commit

### Phase 3: Builder Simplification (BREAKING)
**Goal**: Simplify `VisitUnaryLogicalExpression` to use proper types

**Tasks**:
- [ ] Remove complex wrapper creation (lines 24-40)
- [ ] Use `MultisetExpression` directly from AST builder
- [ ] Simplify constructor calls
- [ ] Remove fallback text conversion logic

**Dependencies**: Phase 2 must be complete

### Phase 4: Fix Compilation Errors (ITERATIVE)
**Goal**: Update all dependent code to use new API

**Strategy**: 
- Compile and fix errors one by one
- Use deprecation warnings as guide
- Update method calls to use new signatures

**Tasks**:
- [ ] Fix all compilation errors
- [ ] Remove deprecated method usage
- [ ] Update any instanceof checks

### Phase 5: Testing and Validation (CRITICAL)
**Goal**: Ensure transformations work correctly

**Tasks**:
- [ ] Run `mvn clean test`
- [ ] Verify expression parsing with test cases
- [ ] Test Oracle→PostgreSQL transformation
- [ ] Manual testing of complex expressions

**Success Criteria**:
- All tests pass
- No regression in transformation quality
- Expression parsing maintains functionality

### Phase 6: Cleanup and Documentation (SAFE)
**Goal**: Finalize refactoring and update documentation

**Tasks**:
- [ ] Remove all `@Deprecated` methods
- [ ] Update inline documentation
- [ ] Update test documentation
- [ ] Add regression tests for the fix

## Testing Strategy

### Before Refactoring
```bash
# Capture current test results
mvn clean test > before_refactor_tests.log 2>&1
```

### During Each Phase
```bash
# Quick compilation check
mvn compile

# Full test validation
mvn clean test
```

### Regression Testing
- Keep examples of complex expressions for manual testing
- Verify transformation output matches expected PostgreSQL syntax
- Test both simple and nested unary logical expressions

## Rollback Plan

### Phase 2-3 (Major Changes)
- Git revert to commit before phase start
- Document any learned insights
- Consider alternative approaches

### Phase 4-6 (Incremental)
- Fix individual files causing issues
- Temporary workarounds if needed
- Partial completion acceptable

## Progress Tracking

### Completion Criteria
- [ ] All compilation errors resolved
- [ ] All tests passing
- [ ] No transformation regressions
- [ ] Code follows grammar structure
- [ ] Documentation updated

### Phase Status
- [x] Phase 1: Analysis - COMPLETED ✅
- [ ] Phase 2: Core Class Update - NOT STARTED  
- [ ] Phase 3: Builder Simplification - NOT STARTED
- [ ] Phase 4: Fix Compilation - NOT STARTED
- [ ] Phase 5: Testing - NOT STARTED
- [ ] Phase 6: Cleanup - NOT STARTED

## Notes and Observations

### Current Architecture Benefits
- The existing `MultisetExpression` class correctly implements the grammar
- Proper delegation chain already exists
- Type safety can be improved significantly

### Key Insights
- Grammar-driven AST design should be strictly followed
- Complex wrapper expressions indicate architectural issues
- Proper type hierarchy simplifies transformation logic

---

**Created**: 2025-07-20  
**Status**: PLANNING  
**Last Updated**: 2025-07-20