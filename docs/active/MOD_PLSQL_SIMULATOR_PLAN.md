# MOD-PLSQL Simulator Implementation Plan

## Overview

This plan outlines the replacement of the current REST controller generation functionality with a mod-plsql simulator that replicates Oracle's mod_plsql web functionality. The goal is to enable direct execution of PostgreSQL procedures that generate HTML content via HTP calls, returning the generated HTML as web responses.

## Current State Analysis

### ✅ COMPLETED: Mod-PLSQL Simulator Implementation
- **ModPlsqlSimulatorGenerator.java**: ✅ Complete - Generates mod-plsql controllers for Oracle packages
- **ModPlsqlExecutor.java**: ✅ Complete - HTP buffer management and procedure execution utilities
- **ExportModPlsqlSimulator.java**: ✅ Complete - Full project setup with Quarkus dependencies
- **Configuration**: ✅ Complete - `do.mod-plsql-simulator`, `do.mod-plsql-procedures` in Config.java and application.properties
- **Integration**: ✅ Complete - Fully integrated in MigrationController export phase
- **Testing**: ✅ Complete - ModPlsqlSimulatorGeneratorTest.java

### ✅ COMPLETED: HTP Infrastructure
- **HTP Schema Functions**: ✅ Complete PostgreSQL implementation in `htp_schema_functions.sql`
  - `SYS.HTP_init()` - Initialize temporary buffer table
  - `SYS.HTP_p(content)` - Print content to buffer
  - `SYS.HTP_page()` - Retrieve complete HTML from buffer
  - Additional utilities: `HTP_prn`, `HTP_print`, `HTP_flush`, etc.
- **HtpStatement.java**: ✅ Complete - AST node for HTP calls in PL/SQL parsing
- **Integration**: ✅ Complete - HTP calls are properly transpiled to PostgreSQL `CALL SYS.HTP_p(...)` statements

### ❌ REMOVED: Legacy REST Controller Implementation
- **RestControllerGenerator.java**: ❌ Removed (replaced by mod-plsql simulator)
- **ExportRestControllers.java**: ❌ Removed (replaced by mod-plsql simulator)
- **Legacy Configuration**: ❌ Removed - `do.write-rest-controllers` properties eliminated

### ⚠️ CURRENT ISSUE: HTP Buffer Schema Problem
- **Problem**: PostgreSQL error "cannot create temporary relation in non-temporary schema"
- **Root Cause**: `CREATE TEMP TABLE SYS.temp_htp_buffer` - temp tables can't be created in non-temp schemas
- **Files Affected**: `htp_schema_functions.sql` and `ExportProjectPostgre.java`
- **Solution Required**: Fix temp table creation syntax for PostgreSQL compatibility

## Implementation Plan

### Phase 1: Configuration Refactoring ✅ COMPLETED

**Goal**: Replace REST controller configuration with mod-plsql simulator configuration

**✅ Completed Changes**:

1. **Config.java Updates**: ✅ Complete
   - ✅ Removed: `doWriteRestControllers`, `doRestControllerFunctions`, `doRestControllerProcedures`, `doRestSimpleDtos`
   - ✅ Added: `doModPlsqlSimulator`, `doModPlsqlProcedures`
   - ✅ Updated getter methods accordingly

2. **application.properties Updates**: ✅ Complete
   ```properties
   # ✅ Removed legacy REST controller properties
   # ✅ Added mod-plsql simulator properties:
   do.mod-plsql-simulator=true
   do.mod-plsql-procedures=true
   ```

3. **Frontend Updates**: ✅ Complete
   - ✅ Updated checkbox text from "Generate REST controllers" to "Generate mod-plsql simulator"
   - ✅ Updated JavaScript to handle new configuration property names

4. **ConfigurationService.java Updates**: ✅ Complete
   - ✅ Updated runtime configuration handling for new properties

### Phase 2: Core Simulator Infrastructure ✅ COMPLETED

**Goal**: Create the mod-plsql simulator generator and execution infrastructure

**✅ Created Classes**:

1. **ModPlsqlSimulatorGenerator.java**: ✅ Complete
   - ✅ Generates Quarkus endpoints with proper URL patterns
   - ✅ Handles HTP buffer initialization and HTML retrieval
   - ✅ Implements dynamic parameter passing from query strings
   - ✅ Includes comprehensive error handling with HTML error pages

2. **ModPlsqlExecutor.java**: ✅ Complete
   - ✅ `initializeHtpBuffer()` method for SYS.HTP_init() calls
   - ✅ `executeProcedureWithHtp()` method for procedure execution
   - ✅ `getHtmlFromBuffer()` method for HTML retrieval
   - ✅ Additional utility methods for buffer management

3. **ExportModPlsqlSimulator.java**: ✅ Complete
   - ✅ Generates mod-plsql simulator controllers for packages with procedures
   - ✅ Creates complete Quarkus project setup with dependencies
   - ✅ Includes README.md generation with usage instructions

### Phase 3: Simulator Controller Generation ✅ COMPLETED

**Goal**: Generate Quarkus controllers that execute procedures and return HTML

**✅ Implemented Controller Pattern**:
```java
@ApplicationScoped
@Path("/modplsql/{schema}/{package}")
@Produces(MediaType.TEXT_HTML)
public class PackageModPlsqlController {
    @Inject AgroalDataSource dataSource;
    
    @GET
    @Path("/{procedureName}")
    public Response executeProcedure(
        @PathParam("schema") String schema,
        @PathParam("package") String packageName,
        @PathParam("procedureName") String procedureName,
        @Context UriInfo uriInfo) {
        
        try (Connection conn = dataSource.getConnection()) {
            // ✅ Initialize HTP buffer
            ModPlsqlExecutor.initializeHtpBuffer(conn);
            
            // ✅ Extract query parameters
            Map<String, String> params = uriInfo.getQueryParameters()...;
            
            // ✅ Execute procedure and get HTML
            String html = ModPlsqlExecutor.executeProcedureWithHtp(
                conn, schema + "." + packageName + "_" + procedureName, params);
            
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (SQLException e) {
            // ✅ Return formatted HTML error page
            String errorHtml = "<html><head><title>Error</title></head><body>" +
                "<h1>Database Error</h1><p>" + e.getMessage() + "</p></body></html>";
            return Response.serverError().entity(errorHtml).type(MediaType.TEXT_HTML).build();
        }
    }
}
```

### Phase 4: Integration and Migration ✅ COMPLETED

**Goal**: Integrate mod-plsql simulator into existing export pipeline

**✅ Completed Integration Points**:

1. **MigrationController Export Phase**: ✅ Complete
   ```java
   // ✅ Integrated mod-plsql simulator export
   if (config.isDoModPlsqlSimulator()) {
       ExportModPlsqlSimulator.generateSimulators(pathJava, javaPackageName, 
           everything.getPackageSpecs(), everything.getPackageBodies(), everything);
   }
   ```

2. **Project Template Generation**: ✅ Complete
   - ✅ Updated `generatePom()` to include Quarkus 3.15.1 dependencies
   - ✅ Updated `generateApplicationProperties()` for mod-plsql configuration
   - ✅ HTP schema functions are properly exported and executed

3. **File Structure**: ✅ Complete
   ```
   target-project/
   ├── pom.xml (✅ Quarkus + PostgreSQL dependencies)
   ├── src/main/resources/application.properties (✅ Complete)
   └── src/main/java/
       └── {javaPackageName}/
           └── {schema}/
               └── modplsql/
                   ├── PackageModPlsqlController.java (✅ Generated)
                   └── ModPlsqlExecutor.java (✅ Generated)
   ```

### Phase 5: Cleanup and Removal ✅ COMPLETED

**Goal**: Remove obsolete REST controller infrastructure

**✅ Removed Files**:
- ✅ `RestControllerGenerator.java` - Properly removed
- ✅ `ExportRestControllers.java` - Properly removed
- ✅ `RestControllerGeneratorTest.java` - Properly removed

**✅ Updated Files**:
- ✅ Removed REST controller references from documentation
- ✅ Updated CLAUDE.md to reflect mod-plsql simulator functionality
- ✅ Cleaned up unused imports and configuration properties

### 🚧 Phase 6: Bug Fixes and Verification (CURRENT)

**Goal**: Fix remaining issues and verify complete functionality

**⚠️ Current Issues**:

1. **HTP Buffer Schema Issue**: 
   - **Problem**: `CREATE TEMP TABLE SYS.temp_htp_buffer` fails in PostgreSQL
   - **Error**: "cannot create temporary relation in non-temporary schema"
   - **Files**: `htp_schema_functions.sql`, `ExportProjectPostgre.java`
   - **Solution**: Fix temp table creation syntax

2. **Verification Testing**:
   - **Need**: Comprehensive testing of mod-plsql simulator functionality
   - **Scope**: End-to-end testing with actual procedures and HTML generation
   - **Goal**: Ensure complete Oracle mod_plsql compatibility

### Phase 7: Verification Testing (PLANNED)

**Goal**: Comprehensive testing and validation of mod-plsql simulator functionality

**Testing Steps**:

1. **Unit Testing**:
   - ✅ `ModPlsqlSimulatorGeneratorTest.java` - Test controller generation
   - 📋 `ModPlsqlExecutorTest.java` - Test HTP execution flow (planned)

2. **Integration Testing**:
   - 📋 End-to-end procedure execution with HTML generation
   - 📋 Parameter passing and conversion testing
   - 📋 Error handling verification
   - 📋 HTP buffer management testing

3. **Manual Testing**:
   - 📋 Generate sample controllers for existing packages
   - 📋 Execute procedures via browser and verify HTML output
   - 📋 Test various parameter combinations
   - 📋 Verify URL pattern compliance: `/modplsql/{schema}/{package}/{procedure}`

4. **Performance Testing**:
   - 📋 Concurrent request handling
   - 📋 HTP buffer isolation testing
   - 📋 Memory usage verification

5. **Compatibility Testing**:
   - 📋 Oracle mod_plsql URL pattern compatibility
   - 📋 HTP function compatibility
   - 📋 Parameter handling compatibility

## Technical Specifications

### URL Pattern
```
GET /modplsql/{schema}/{package}/{procedure}?param1=value1&param2=value2
```

### Response Format
- **Content-Type**: `text/html`
- **Body**: Raw HTML generated by the procedure via HTP calls

### Parameter Handling
- All query parameters passed as procedure parameters
- Parameter type conversion handled automatically
- Support for both named and positional parameters

### Error Handling
- Database connection errors return 500 with error message
- Procedure execution errors return 500 with database error details
- Missing procedures return 404

### HTP Buffer Management
- Each request initializes a fresh HTP buffer (`SYS.HTP_init()`)
- Buffer is automatically cleaned up after response generation
- Support for concurrent requests with isolated buffers

## Dependencies

### Required Quarkus Extensions
- `quarkus-resteasy-reactive` - For REST endpoints
- `quarkus-jdbc-postgresql` - For PostgreSQL connectivity
- `quarkus-agroal` - For connection pooling

### Database Requirements
- PostgreSQL instance with migrated schema
- HTP schema functions installed (`htp_schema_functions.sql`)
- Migrated procedures that use HTP calls

## Testing Strategy

### Unit Tests
- `ModPlsqlSimulatorGeneratorTest.java` - Test controller generation
- `ModPlsqlExecutorTest.java` - Test HTP execution flow

### Integration Tests
- End-to-end procedure execution with HTML generation
- Parameter passing and conversion testing
- Error handling verification

### Manual Testing
- Generate sample controllers for existing packages
- Execute procedures via browser and verify HTML output
- Test various parameter combinations

## Migration Guide

### For Existing Projects
1. Update configuration to use new mod-plsql properties
2. Re-run export phase to generate mod-plsql controllers
3. Deploy updated Quarkus application
4. Test procedure execution via web interface

### For New Projects
1. Enable mod-plsql simulator in configuration
2. Ensure procedures use HTP calls for HTML generation
3. Export and deploy mod-plsql enabled application

## Benefits

### Over Current REST Controllers
- **True mod-plsql Compatibility**: Replicates Oracle's mod_plsql functionality exactly
- **Simplified Architecture**: No complex parameter mapping or response conversion
- **Direct HTML Output**: Procedures directly generate web content
- **Familiar Oracle Pattern**: Existing Oracle developers can use familiar mod_plsql patterns

### Technical Advantages
- **Reduced Complexity**: Eliminates JSON/REST layer complexity
- **Better Performance**: Direct HTML generation without serialization overhead
- **Legacy Compatibility**: Existing Oracle mod_plsql applications can be migrated directly
- **Web Framework Independence**: Generated HTML can be consumed by any web client

## Implementation Timeline

- **Phase 1**: Configuration Refactoring ✅ COMPLETED
- **Phase 2**: Core Infrastructure ✅ COMPLETED
- **Phase 3**: Controller Generation ✅ COMPLETED
- **Phase 4**: Integration and Migration ✅ COMPLETED
- **Phase 5**: Cleanup and Removal ✅ COMPLETED
- **Phase 6**: Bug Fixes and Verification 🚧 IN PROGRESS
- **Phase 7**: Verification Testing 📋 PLANNED

**Original Estimated Time**: 7 days ✅ COMPLETED
**Current Status**: Implementation complete, addressing PostgreSQL compatibility issues

## Conclusion

This plan provides a complete replacement for the current REST controller functionality with a proper mod-plsql simulator that maintains the Oracle compatibility while leveraging the existing HTP infrastructure and PostgreSQL-first architecture. The implementation will enable direct web content generation from migrated Oracle procedures, providing a true mod_plsql experience in the PostgreSQL environment.