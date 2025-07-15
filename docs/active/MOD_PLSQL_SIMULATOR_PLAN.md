# MOD-PLSQL Simulator Implementation Plan

## Overview

This plan outlines the replacement of the current REST controller generation functionality with a mod-plsql simulator that replicates Oracle's mod_plsql web functionality. The goal is to enable direct execution of PostgreSQL procedures that generate HTML content via HTP calls, returning the generated HTML as web responses.

## Current State Analysis

### Existing REST Controller Implementation
- **RestControllerGenerator.java**: Generates JAX-RS endpoints that call PostgreSQL functions/procedures
- **ExportRestControllers.java**: Manages REST controller file generation and project setup
- **Configuration**: `do.write-rest-controllers`, `do.rest-controller-functions`, `do.rest-controller-procedures`
- **Frontend Integration**: Checkbox for "Generate REST controllers" in web UI

### Existing HTP Infrastructure
- **HTP Schema Functions**: Complete PostgreSQL implementation in `htp_schema_functions.sql`
  - `SYS.HTP_init()` - Initialize temporary buffer table
  - `SYS.HTP_p(content)` - Print content to buffer
  - `SYS.HTP_page()` - Retrieve complete HTML from buffer
  - Additional utilities: `HTP_prn`, `HTP_print`, `HTP_flush`, etc.
- **HtpStatement.java**: AST node for HTP calls in PL/SQL parsing
- **Integration**: HTP calls are properly transpiled to PostgreSQL `CALL SYS.HTP_p(...)` statements

## Implementation Plan

### Phase 1: Configuration Refactoring ✅ READY

**Goal**: Replace REST controller configuration with mod-plsql simulator configuration

**Changes Required**:

1. **Config.java Updates**:
   - Remove: `doWriteRestControllers`, `doRestControllerFunctions`, `doRestControllerProcedures`, `doRestSimpleDtos`
   - Add: `doModPlsqlSimulator`, `doModPlsqlProcedures`
   - Update getter methods accordingly

2. **application.properties Updates**:
   ```properties
   # Remove these lines:
   do.write-rest-controllers=true
   do.rest-controller-functions=true
   do.rest-controller-procedures=true
   do.rest-simple-dtos=false
   
   # Add these lines:
   do.mod-plsql-simulator=true
   do.mod-plsql-procedures=true
   ```

3. **Frontend Updates**:
   - Update checkbox text from "Generate REST controllers" to "Generate mod-plsql simulator"
   - Update JavaScript to handle new configuration property names

4. **ConfigurationService.java Updates**:
   - Update runtime configuration handling for new properties

### Phase 2: Core Simulator Infrastructure ✅ READY

**Goal**: Create the mod-plsql simulator generator and execution infrastructure

**New Classes to Create**:

1. **ModPlsqlSimulatorGenerator.java** (replaces RestControllerGenerator.java):
   ```java
   public class ModPlsqlSimulatorGenerator {
       public static String generateSimulator(OraclePackage pkg, String javaPackageName, Everything data)
       // Generates Quarkus endpoints that:
       // - Call SYS.HTP_init() to initialize buffer
       // - Execute procedure with parameters
       // - Call SYS.HTP_page() to get HTML content
       // - Return HTML as text/html response
   }
   ```

2. **ModPlsqlExecutor.java** (new utility class):
   ```java
   public class ModPlsqlExecutor {
       public static String executeProcedureWithHtp(Connection conn, String procedureName, Map<String, Object> parameters)
       // Utility method to:
       // 1. Call SYS.HTP_init()
       // 2. Execute the target procedure
       // 3. Call SYS.HTP_page() and return result
   }
   ```

3. **ExportModPlsqlSimulator.java** (replaces ExportRestControllers.java):
   ```java
   public class ExportModPlsqlSimulator {
       public static void generateSimulators(String path, String javaPackageName, List<OraclePackage> packages, Everything data)
       // Generates mod-plsql simulator controllers for packages with procedures
   }
   ```

### Phase 3: Simulator Controller Generation ✅ READY

**Goal**: Generate Quarkus controllers that execute procedures and return HTML

**Controller Pattern**:
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
            // Initialize HTP buffer
            conn.prepareCall("CALL SYS.HTP_init()").execute();
            
            // Build procedure call with query parameters
            String procedureCall = buildProcedureCall(schema, packageName, procedureName, uriInfo.getQueryParameters());
            conn.prepareCall(procedureCall).execute();
            
            // Get generated HTML
            String html = getHtmlFromBuffer(conn);
            
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        } catch (SQLException e) {
            return Response.serverError().entity("Database error: " + e.getMessage()).build();
        }
    }
}
```

### Phase 4: Integration and Migration ✅ READY

**Goal**: Integrate mod-plsql simulator into existing export pipeline

**Integration Points**:

1. **Main.java Export Phase**:
   ```java
   // Replace REST controller export with mod-plsql simulator export
   if (config.isDoModPlsqlSimulator()) {
       ExportModPlsqlSimulator.generateSimulators(pathJava, javaPackageName, everything.getPackageSpecs(), everything.getPackageBodies(), everything);
   }
   ```

2. **Project Template Generation**:
   - Update `generatePom()` to include necessary Quarkus dependencies
   - Update `generateApplicationProperties()` for mod-plsql configuration
   - Ensure HTP schema functions are properly exported and executed

3. **File Structure**:
   ```
   target-project/
   ├── pom.xml (Quarkus + PostgreSQL dependencies)
   ├── src/main/resources/application.properties
   └── src/main/java/
       └── {javaPackageName}/
           └── {schema}/
               └── modplsql/
                   ├── PackageModPlsqlController.java
                   └── ...
   ```

### Phase 5: Cleanup and Removal ✅ READY

**Goal**: Remove obsolete REST controller infrastructure

**Files to Remove**:
- `RestControllerGenerator.java`
- `ExportRestControllers.java`
- `RestControllerGeneratorTest.java`

**Files to Update**:
- Remove REST controller references from documentation
- Update CLAUDE.md to reflect mod-plsql simulator functionality
- Clean up unused imports and configuration properties

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

- **Phase 1**: Configuration Refactoring (1 day)
- **Phase 2**: Core Infrastructure (2 days)
- **Phase 3**: Controller Generation (2 days)
- **Phase 4**: Integration and Testing (1 day)
- **Phase 5**: Cleanup and Documentation (1 day)

**Total Estimated Time**: 7 days

## Conclusion

This plan provides a complete replacement for the current REST controller functionality with a proper mod-plsql simulator that maintains the Oracle compatibility while leveraging the existing HTP infrastructure and PostgreSQL-first architecture. The implementation will enable direct web content generation from migrated Oracle procedures, providing a true mod_plsql experience in the PostgreSQL environment.