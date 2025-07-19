# PlSqlAstBuilder Refactoring Plan

## Overview

This document outlines the systematic refactoring of the `PlSqlAstBuilder` class to follow the visitor pattern, moving method implementations to separate visitor classes in the `builderfncs` package. This refactoring reduces the size of the main class and improves code organization.

## Progress Status

### ✅ **Completed Refactoring (Previous Sessions)**
- visitLoop_statement → VisitLoopStatement
- visitSelect_statement → VisitSelectStatement  
- visitSubquery → VisitSubquery
- visitWith_clause → VisitWithClause
- visitSelect_list_elements → VisitSelectListElements
- visitIf_statement → VisitIfStatement
- visitType_declaration → VisitTypeDeclaration
- visitType_definition → VisitTypeDefinition
- visitRecord_type_def → VisitRecordTypeDef
- visitField_spec → VisitFieldSpec
- visitFetch_statement → VisitFetchStatement
- visitObject_member_spec → VisitObjectMemberSpec
- visitType_elements_parameter → VisitTypeElementsParameter
- visitFunc_decl_in_type → VisitFuncDeclInType
- visitProc_decl_in_type → VisitProcDeclInType
- visitCursor_declaration → VisitCursorDeclaration
- visitUpdate_statement → VisitUpdateStatement
- visitSingle_table_insert → VisitSingleTableInsert
- visitQuery_block → VisitQueryBlock
- visitSubquery_factoring_clause → VisitSubqueryFactoringClause
- visitTable_ref_aux_internal_two → VisitTableRefAuxInternalTwo
- visitDml_table_expression_clause → VisitDmlTableExpressionClause

### ✅ **Current Session Progress (2025-07-19)**
- visitUnary_expression → VisitUnaryExpression ✅ (70 lines removed)
- visitOther_function → VisitOtherFunction ✅ (55 lines removed)
- visitFunction_body → VisitFunctionBody ✅ (42 lines removed)
- visitProcedure_body → VisitProcedureBody ✅ (41 lines removed)
- visitCreate_function_body → VisitCreateFunctionBody ✅ (40 lines removed)
- visitCreate_package_body → VisitCreatePackageBody ✅ (39 lines removed)
- visitCreate_procedure_body → VisitCreateProcedureBody ✅ (39 lines removed)

### 🚧 **Remaining Methods to Refactor (36 methods)**

## Tier 1: High Priority - Complex Methods (40+ lines)

### ✅ 1. **visitUnary_expression** → VisitUnaryExpression **COMPLETED** ✅
- **Lines Removed**: ~70 lines
- **Complexity**: Very high - collection method parsing (COUNT, FIRST, LAST, etc.)
- **Status**: Fully refactored with all helper methods moved to visitor class

### ✅ 2. **visitOther_function** → VisitOtherFunction **COMPLETED** ✅  
- **Lines Removed**: ~55 lines
- **Complexity**: High - cursor attributes and analytical functions  
- **Status**: Fully refactored with all analytical function parsing moved to visitor class

### ✅ 3. **visitFunction_body** → VisitFunctionBody **COMPLETED** ✅
- **Lines Removed**: ~42 lines
- **Complexity**: High - complete function parsing with declarations
- **Status**: Fully refactored using DeclarationParsingUtils.extractDeclarations()

### ✅ 4. **visitProcedure_body** → VisitProcedureBody **COMPLETED** ✅
- **Lines Removed**: ~41 lines
- **Complexity**: High - complete procedure parsing with declarations and exception handling
- **Status**: Fully refactored using DeclarationParsingUtils.extractDeclarations()

### ✅ 5. **visitCreate_function_body** → VisitCreateFunctionBody **COMPLETED** ✅
- **Lines Removed**: ~40 lines
- **Complexity**: High - standalone function parsing with declarations
- **Status**: Fully refactored using DeclarationParsingUtils.extractDeclarations() with standalone flags

### ✅ 6. **visitCreate_package_body** → VisitCreatePackageBody **COMPLETED** ✅
- **Lines Removed**: ~39 lines
- **Complexity**: High - package body parsing with member classification
- **Status**: Fully refactored with package context management and member type classification

### ✅ 7. **visitCreate_procedure_body** → VisitCreateProcedureBody **COMPLETED** ✅
- **Lines Removed**: ~39 lines
- **Complexity**: High - standalone procedure parsing with declarations
- **Status**: Fully refactored using DeclarationParsingUtils.extractDeclarations() with standalone flags

## 🎉 **TIER 1 COMPLETE!** All 7 complex methods (40+ lines) have been successfully refactored! 🎉

## Tier 2: Medium Priority - Moderate Complexity (20-40 lines)

### 8. **visitCreate_package** (Lines 742-774, ~33 lines)
- **Implementation**: Package spec parsing with variables and types
- **Refactor to**: VisitCreatePackage

### 9. **visitDelete_statement** (Lines 391-420, ~30 lines)
- **Implementation**: DELETE statement with table name and WHERE clause
- **Refactor to**: VisitDeleteStatement

### 10. **visitConstructor_declaration** (Lines 524-552, ~29 lines)
- **Implementation**: Constructor parsing with parameters and body
- **Refactor to**: VisitConstructorDeclaration

### 11. **visitConcatenation** (Lines 1295-1320, ~26 lines)
- **Implementation**: Binary operations and model expressions
- **Refactor to**: VisitConcatenation

### 12. **visitType_body** (Lines 577-601, ~25 lines)
- **Implementation**: Type body parsing with member parsing
- **Refactor to**: VisitTypeBody

### 13. **visitRelational_expression** (Lines 1246-1270, ~25 lines)
- **Implementation**: Relational operations and compound expressions
- **Refactor to**: VisitRelationalExpression

### 14. **visitOver_clause** (Lines 1518-1539, ~22 lines)
- **Implementation**: Analytical function OVER clause
- **Refactor to**: VisitOverClause

## Tier 3: Lower Priority - Smaller Methods (5-20 lines)

15. visitOpen_statement (16 lines) → VisitOpenStatement
16. visitExit_statement (16 lines) → VisitExitStatement  
17. visitExpression (15 lines) → VisitExpression
18. visitMultiset_expression (14 lines) → VisitMultisetExpression
19. visitModel_expression (14 lines) → VisitModelExpression
20. visitCompound_expression (14 lines) → VisitCompoundExpression
21. visitVarray_type_def (13 lines) → VisitVarrayTypeDef
22. visitWhere_clause (12 lines) → VisitWhereClause
23. visitCondition (12 lines) → VisitCondition

## Tier 4: Simple Methods (2-10 lines)

24-45. Various smaller methods including:
- visitStatement → VisitStatement
- visitReturn_statement → VisitReturnStatement
- visitTable_ref → VisitTableRef
- visitAssignment_statement → VisitAssignmentStatement
- visitRaise_statement → VisitRaiseStatement
- And 20+ other simple methods

## Refactoring Pattern

Each refactored method follows this pattern:

### 1. Create Visitor Class
```java
package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitMethodName {
  public static PlSqlAst visit(
          PlSqlParser.Method_nameContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Original method implementation moved here
    // Replace 'visit(...)' calls with 'astBuilder.visit(...)'
    // Replace 'schema' with 'astBuilder.schema'
    
    return new AstNode(...);
  }
}
```

### 2. Update PlSqlAstBuilder
- Add import: `import me.christianrobert.ora2postgre.plsql.builderfncs.VisitMethodName;`
- Replace method body: `return VisitMethodName.visit(ctx, this);`

### 3. Test Verification
- Run `mvn clean test` after each refactoring
- Ensure all 362+ tests continue to pass

## Shared Utilities

### Existing Utilities (Can Be Reused)
- **DeclarationParsingUtils**: Contains shared logic for:
  - `extractParameters()` - Parameter extraction
  - `extractDeclarations()` - Declaration bundle extraction  
  - `extractStatements()` - Statement extraction
  - `extractVariablesFromDeclareSpecs()` - Variable extraction
  - `extractCursorDeclarationsFromDeclareSpecs()` - Cursor extraction

### Potential New Utilities
- **ExpressionParsingUtils**: For complex expression parsing logic
- **CollectionMethodParsingUtils**: For collection method parsing (%COUNT, %FIRST, etc.)
- **CursorAttributeParsingUtils**: For cursor attribute parsing (%FOUND, %NOTFOUND, etc.)

## Key Considerations

### Code Duplication Elimination
- Look for repeated patterns across methods
- Extract common logic to shared utility classes
- Follow DRY principle established in DeclarationParsingUtils

### Access Level Changes
- PlSqlAstBuilder.schema is already public for visitor access
- May need to make other private methods public if used by visitors
- Use `astBuilder.methodName()` pattern for accessing PlSqlAstBuilder methods

### Testing Strategy
- Run tests after each method refactoring
- Verify functionality remains identical
- No behavioral changes, only structural improvements

## Progress Tracking

- [x] **Tier 1: 7/7 complex methods completed** ✅ 🎉 **COMPLETE!** 🎉
  - ✅ All methods: visitUnary_expression, visitOther_function, visitFunction_body, visitProcedure_body, visitCreate_function_body, visitCreate_package_body, visitCreate_procedure_body
- [ ] Tier 2: 7 medium methods (20-40 lines each)  
- [ ] Tier 3: 9 smaller methods (5-20 lines each)
- [ ] Tier 4: 22 simple methods (2-10 lines each)

**Total**: 36 methods remaining (326 lines removed so far)

## Success Criteria

1. ✅ All tests continue to pass
2. ✅ PlSqlAstBuilder class significantly reduced in size
3. ✅ Visitor pattern consistently applied
4. ✅ Code duplication minimized through shared utilities
5. ✅ Improved code organization and maintainability

## Next Steps

1. **Start with visitUnary_expression** (most complex, 70 lines)
2. Work through Tier 1 methods systematically
3. Create shared utilities as patterns emerge
4. Continue with Tier 2 and subsequent tiers
5. Final verification with complete test suite

---

**Last Updated**: 2025-07-19  
**Status**: 🎉 **Tier 1 COMPLETE!** (7/7 completed) 🎉
**Current Progress**: 326 lines removed from PlSqlAstBuilder
**Next Priority**: Begin Tier 2 with visitCreate_package (33 lines)