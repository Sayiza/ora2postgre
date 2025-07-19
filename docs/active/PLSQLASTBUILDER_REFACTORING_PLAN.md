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

### 🚧 **Remaining Methods to Refactor (45 methods)**

## Tier 1: High Priority - Complex Methods (40+ lines)

### 1. **visitUnary_expression** (Lines 933-1002, ~70 lines) - 🎯 START HERE
- **Complexity**: Very high - collection method parsing (COUNT, FIRST, LAST, etc.)
- **Implementation**: Handles standard functions, atoms, collection methods
- **Refactor to**: VisitUnaryExpression
- **Dependencies**: Uses schema field, extensive method parsing logic

### 2. **visitOther_function** (Lines 1341-1395, ~55 lines)
- **Complexity**: High - cursor attributes and analytical functions  
- **Implementation**: %FOUND, %NOTFOUND, %ISOPEN, %ROWCOUNT parsing
- **Refactor to**: VisitOtherFunction
- **Dependencies**: Analytical function parsing, cursor attribute logic

### 3. **visitFunction_body** (Lines 666-707, ~42 lines)
- **Complexity**: High - complete function parsing
- **Implementation**: Declaration extraction, statement parsing
- **Refactor to**: VisitFunctionBody
- **Dependencies**: DeclarationParsingUtils (can be reused)

### 4. **visitProcedure_body** (Lines 623-663, ~41 lines)
- **Complexity**: High - complete procedure parsing
- **Implementation**: Similar to function body with exception handling
- **Refactor to**: VisitProcedureBody
- **Dependencies**: DeclarationParsingUtils (can be reused)

### 5. **visitCreate_function_body** (Lines 818-857, ~40 lines)
- **Complexity**: High - standalone function parsing
- **Implementation**: Sets standalone flag and schema
- **Refactor to**: VisitCreateFunctionBody
- **Dependencies**: Similar patterns to visitFunction_body

### 6. **visitCreate_package_body** (Lines 777-815, ~39 lines)
- **Complexity**: High - package body parsing
- **Implementation**: Member classification and parsing
- **Refactor to**: VisitCreatePackageBody
- **Dependencies**: Complex member parsing logic

### 7. **visitCreate_procedure_body** (Lines 860-898, ~39 lines)
- **Complexity**: High - standalone procedure parsing
- **Implementation**: Similar to function body without return type
- **Refactor to**: VisitCreateProcedureBody
- **Dependencies**: Similar patterns to procedure body parsing

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

- [ ] Tier 1: 7 complex methods (40+ lines each)
- [ ] Tier 2: 7 medium methods (20-40 lines each)  
- [ ] Tier 3: 9 smaller methods (5-20 lines each)
- [ ] Tier 4: 22 simple methods (2-10 lines each)

**Total**: 45 methods remaining

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
**Status**: Ready to begin Tier 1 refactoring
**Priority**: Start with visitUnary_expression