# TODO.md - Oracle to PostgreSQL/Java Migration Tasks

This document collects all existing TODO items from the codebase and additional strategic tasks needed to achieve successful PL/SQL transpilation to PostgreSQL and Java.

## 🔥 CRITICAL PATH - Core Migration Goals

### 1. **Complete Expression System** (Blocking Multiple Features)
- [ ] Schema lookup for functions within statements (or even within expessions)?!
- [ ] **`Expression.java:28`** - Implement proper `toJava()` conversion beyond raw text
- [ ] **`Expression.java:32`** - Implement complete `toPostgre()` method  
- [ ] **`PlSqlAstBuilder.java:234`** - Handle complex expression parsing (currently marked as "INSANELY complicated")
- [ ] **`Everything.java:189`** - Handle literals, operators, and other expression types beyond functions
- [ ] **`PlSqlAstBuilder.java:352`** - Replace simplified expression parsing with proper AST generation

### 2. **SQL Query Processing** (Essential for View Transpilation)
- [ ] **`SelectStatement.java:18`** - Implement WHERE clause processing
- [ ] **`SelectStatement.java:18`** - Add UNION and WITH clause support
- [ ] **`SelectStatement.java:18`** - Handle CONNECT BY hierarchy queries (Oracle-specific)
- [ ] **`SelectStatement.java:61`** - Implement ORDER BY clause conversion
- [ ] **`SelectStatement.java:63`** - Add FOR UPDATE clause support
- [ ] **`SelectQueryBlock.java:10`** - Complete FROM and WHERE clause parsing
- [ ] **`SelectSubQuery.java:38`** - Implement UNION, MINUS, INTERSECT set operators

### 3. **Data Type System Overhaul** (Foundation for All Conversions)
- [ ] **`DataTypeSpec.java:42`** - Implement custom type lookup with `%TYPE` and `%ROWTYPE` support
- [ ] **`NestedTableType.java:7-8`** - Replace DataTypeSpec with unified TypeSpec object across codebase
- [ ] **`VarrayType.java:9-10`** - Same DataTypeSpec replacement
- [ ] **`TypeConverter.java:9`** - Replace generic "Object" fallback with proper type mapping
- [ ] **`TypeConverter.java:59`** - Improve `%ROWTYPE` handling beyond simple "text"

## 🎯 JAVA TRANSPILATION (Main Focus)

### 4. **HTP Package Replacement System** (Web Output Conversion)
- [ ] **`ToExportJava.java:97,134,142`** - Develop sophisticated HTP detection rules beyond simple string contains
- [ ] **`Procedure.java:105`** - Implement proper web procedure detection (HTP usage analysis)
- [ ] Create comprehensive HTP-to-Quarkus mapping:
  - [ ] `htp.p()` → JAX-RS response writing
  - [ ] `htp.formOpen()` → HTML form generation
  - [ ] `htp.tableOpen()` → HTML table structures
  - [ ] Session management conversion
  - [ ] Parameter handling conversion
- [ ] Design template engine integration (Qute/Freemarker) for complex HTML generation
- [ ] Implement REST microservice endpoint generation for HTP-based procedures

### 5. **Java Code Generation Enhancement**
- [ ] **`Function.java:72`** - Fix return type generation in `toJava()`
- [ ] **`Parameter.java:38`** - Implement proper type mapping for web parameters (beyond String)
- [ ] **`ExportTable.java:19`** - Generate Jakarta Persistence entities from table metadata
- [ ] **`TableMetadata.java:52`** - Complete Java entity generation
- [ ] **`SelectStatement.java:74`** - Improve anonymous entity naming in `getJavaClassName()`
- [ ] Add Quarkus/Jakarta annotations generation:
  - [ ] `@Path` and JAX-RS annotations for packages with HTP procedures
  - [ ] `@ApplicationScoped` for business logic packages
  - [ ] `@Repository` for data access patterns
  - [ ] `@Entity` and `@Table` (Jakarta Persistence) for database objects

### 6. **Advanced Java Features**
- [ ] Implement CDI dependency injection mapping (Oracle package dependencies → Quarkus beans)
- [ ] Create transaction boundary detection and `@Transactional` annotation placement
- [ ] Generate Panache repositories or Jakarta Persistence repositories for CRUD operations
- [ ] Implement exception handling translation (Oracle exceptions → Jakarta/Quarkus exceptions)
- [ ] Add Jakarta validation annotations (`@Valid`, `@NotNull`, etc.) based on database constraints
- [ ] Integration with existing Quarkus microservices architecture

## 🐘 POSTGRESQL TRANSPILATION (View Support)

### 7. **PostgreSQL Function Generation** 
- [ ] **`Function.java:104`** - Implement PostgreSQL function body generation with statement processing
- [ ] **`Procedure.java:99`** - Add statement implementation for PostgreSQL procedures
- [ ] **`Constructor.java:72`** - Complete PostgreSQL constructor generation
- [ ] **`OraclePackage.java:101`** - Implement PostgreSQL package-to-schema conversion

### 8. **PostgreSQL SQL Conversion**
- [ ] **`AssignmentStatement.java:32`** - Implement PostgreSQL assignment statement generation
- [ ] **`Cursor.java:37`** - Complete PostgreSQL cursor implementation
- [ ] **`SubType.java:36`** - Add PostgreSQL subtype generation
- [ ] **`PackageType.java:39`** - Complete PostgreSQL type generation
- [ ] Oracle-specific SQL features conversion:
  - [ ] `CONNECT BY` → PostgreSQL recursive CTEs
  - [ ] Oracle date functions → PostgreSQL equivalents
  - [ ] `ROWNUM` → PostgreSQL `ROW_NUMBER()`
  - [ ] Oracle analytic functions → PostgreSQL window functions

## 🔧 INFRASTRUCTURE & TOOLING

### 9. **Parser Enhancement**
- [ ] The grammar is not yet fully functional (outdated) for all used features !
- [ ] Standalone functions need to be supported though all steps of the process...
- [ ] **`PlSqlAstBuilder.java:75`** - Handle unclear statement structures properly
- [ ] **`PlSqlAstBuilder.java:90`** - Clean up HTP statement processing
- [ ] **`PlSqlAstBuilder.java:97`** - Process expression children in return statements
- [ ] **`PlSqlAstBuilder.java:201`** - Handle SELECT * by expanding to actual field lists
- [ ] **`PlSqlAstBuilder.java:215`** - Fix FROM clause parsing
- [ ] **`PlSqlAstBuilder.java:254,262,272,273`** - Implement JOIN, PIVOT, UNPIVOT, flashback clauses

### 10. **Code Organization & Quality**
- [ ] **`ExportObjectType.java:39,51`** - Implement proper file naming strategies
- [ ] **`ExportPackage.java:40,52`** - Same file naming improvements
- [ ] **`ExportObjectType.java:74`** - Fine-tune object spec/body merging
- [ ] **`ExportPackage.java:75`** - Improve package merging logic
- [ ] **`ExportPackage.java:87`** - Implement SubType merging
- [ ] **`ExportPackage.java:70`** - Add cursor merging for private/public visibility

### 11. **Advanced SQL Features**
- [ ] **`TableExpressionClause.java:7`** - Add subquery support in table expressions
- [ ] **`TableReferenceAux.java:28`** - Implement flashback and other clause handling
- [ ] **`TableReferenceAuxInternal.java:58`** - Complete subquery type handling
- [ ] **`SelectSubQueryBasicElement.java:51`** - Add validation logic for subquery elements
- [ ] **`TableReference.java:28`** - Append JOIN, PIVOT, UNPIVOT clauses

## 📋 REMAINING PARSER TASKS

### 12. **Language Feature Completion**
- [ ] **`PlSqlAstBuilder.java:642`** - Add support for `$IF` conditionals, subtypes, package types, cursors
- [ ] **`PlSqlAstBuilder.java:576`** - Distinguish between public and private procedures
- [ ] **`PlSqlAstBuilder.java:605`** - Improve function return type parsing
- [ ] **`PlSqlAstBuilder.java:410`** - Add type definition support
- [ ] **`Main.java:138`** - Parse additional PL/SQL elements

### 13. **Code Generation Polish**
- [ ] **`ForLoopStatement.java:71`** - Fix indentation handling
- [ ] **`SelectListElement.java:20`** - Handle asterisk expressions with field expansion
- [ ] **`SelectListElement.java:36`** - Improve alias handling in reference names
- [ ] **`SelectListElement.java:45`** - Implement schema prefix and case correction

## 🎯 STRATEGIC MILESTONES

### Phase 1: Foundation (Weeks 1-2)
- [ ] Complete Expression system (#1)
- [ ] Implement basic SQL query processing (#2)
- [ ] Overhaul data type system (#3)

### Phase 2: Java Focus (Weeks 3-4)
- [ ] Enhanced HTP replacement system (#4)
- [ ] Java code generation improvements (#5)
- [ ] Basic Quarkus/Jakarta integration (#6)

### Phase 3: PostgreSQL Support (Weeks 5-6)
- [ ] PostgreSQL function generation (#7)
- [ ] SQL conversion features (#8)
- [ ] Advanced PostgreSQL compatibility

### Phase 4: Production Ready (Weeks 7-8)
- [ ] Parser robustness (#9)
- [ ] Code quality improvements (#10)
- [ ] Advanced SQL features (#11)
- [ ] Complete language support (#12-13)

## 🚀 SUCCESS CRITERIA

**PostgreSQL Transpilation Success:**
- [ ] All views successfully transpile and execute in PostgreSQL
- [ ] Complex Oracle SQL features (CONNECT BY, analytics) properly converted
- [ ] Function calls in views resolve correctly through new function lookup system

**Java Transpilation Success:**
- [ ] HTP-based procedures generate working Quarkus REST microservice endpoints
- [ ] Database operations convert to Jakarta Persistence/Panache patterns
- [ ] Generated code is maintainable and follows Java best practices
- [ ] Web forms and HTML generation work through template engines
- [ ] Seamless integration with existing Quarkus microservices architecture

**Overall Migration Success:**
- [ ] Zero manual code modification required for basic PL/SQL patterns
- [ ] Complex business logic preserves semantic equivalence
- [ ] Performance characteristics maintained or improved
- [ ] Generated code passes automated testing suite

## 📝 NOTES

- **Target Framework**: Java transpilation targets **Quarkus with Jakarta EE** (not Spring). The software already has Quarkus containers running that communicate with the PL/SQL legacy program via REST microservices.
- **HTP Priority**: The HTP package replacement is critical since the current PL/SQL software directly exports HTML. The existing rudimentary Java implementation needs significant expansion to integrate with the Quarkus microservices architecture.
- **Microservices Integration**: Generated code must seamlessly integrate with existing Quarkus containers and REST communication patterns.
- **View Dependencies**: PostgreSQL view generation depends heavily on the function lookup system (already implemented) and expression processing.
- **Test Coverage**: Each major component should have comprehensive unit tests following the pattern established in `EverythingFunctionLookupTest.java`.
- **Incremental Approach**: Focus on getting simple cases working end-to-end before tackling complex Oracle-specific features.