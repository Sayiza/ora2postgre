# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Oracle-to-PostgreSQL migration tool that extracts, parses, and transpiles Oracle PL/SQL code using a **PostgreSQL-first architecture**. The project consists of multiple interconnected sub-projects:

1. **PL/SQL to PostgreSQL transpilation** - Business logic transformation for functions, procedures, and views
2. **REST controller generation** - Minimal JAX-RS endpoints that call PostgreSQL functions directly
3. **High-performance data transfer system** - Hybrid CSV/SQL approach with progress tracking
4. **Dynamic SQL transformation service** - Runtime transpilation for DBMS_SQL.PARSE calls
5. **AI-based CoFramework to Angular transformation** (future)
6. **AI-based backend transformation** (future)

## Architecture Philosophy

**PostgreSQL-First Approach**: Business logic resides exclusively in PostgreSQL functions and procedures. Java REST controllers serve as thin API proxies that call PostgreSQL functions via JDBC. This eliminates code duplication and ensures views can directly call the same business logic that REST APIs expose.

## Build and Development Commands

### Basic Maven Commands
```bash
# Compile and generate ANTLR sources
mvn compile

# Clean build
mvn clean compile

# Run as Quarkus application (current method)
mvn quarkus:dev

# Build and run as Quarkus app
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar
```

### Legacy Run Command (Deprecated)
```bash
# Old CLI approach - no longer the primary method
mvn exec:java -Dexec.mainClass="com.sayiza.oracle2postgre.Main"
```

### Database Setup
```bash
# Start PostgreSQL test container
docker rm -f pgtest; docker run --name pgtest -e POSTGRES_PASSWORD=secret -p 5432:5432 -d postgres
```

## Architecture Overview

### Core Components

**Main Processing Pipeline** (`Main.java:34-201`):
1. **Extract** - Oracle database metadata extraction
2. **Parse** - ANTLR-based AST generation  
3. **Transform** - Convert to target formats
4. **Export** - Generate Java/PostgreSQL files
5. **Execute** - Run generated SQL files

**Central Data Store** (`Everything.java`):
- Global context object holding all extracted and transformed data
- Raw database metadata (tables, views, synonyms, PL/SQL code)
- Parsed AST representations
- Schema resolution and data type lookup functionality

**Configuration System** (`Config.java`, `application.properties`):
- Feature flags control which components to process
- Database connection parameters
- Target project paths and package names
- **NEW**: REST controller generation flags (`do.write-rest-controllers`, `do.rest-controller-functions`, etc.)
- **DEPRECATED**: Complex Java generation (`do.write-java-files` now defaults to false)

**REST API Endpoints** (`Main.java` as JAX-RS controller):
- `POST /migration/extract` - Extract Oracle database metadata
- `POST /migration/parse` - Parse PL/SQL into AST
- `POST /migration/export` - Generate PostgreSQL and REST code
- `POST /migration/execute-pre` - Execute pre-transfer SQL (schema & tables)
- `POST /migration/execute-post` - Execute post-transfer SQL (constraints & objects)
- `POST /migration/transferdata` - Transfer table data Oracle → PostgreSQL
- `POST /migration/full` - Run complete migration pipeline
- `GET /migration/status` - Get migration status & statistics
- `GET /migration/jobs/{jobId}` - Check migration job status

### Key Architectural Patterns

**ANTLR Integration**:
- Grammar files in `src/main/antlr4/` define PL/SQL parsing rules
- Generated lexer/parser classes in `gen/` (via Maven plugin)
- Custom AST builder (`PlSqlAstBuilder.java`) transforms parse trees

**PostgreSQL-First Code Generation**:
- AST nodes implement `toPostgre()` methods for business logic
- **REMOVED**: Complex `toJava()` implementations (25+ methods removed)
- **NEW**: `RestControllerGenerator` creates minimal JAX-RS endpoints
- **NEW**: `ExportRestControllers` manages REST controller file generation
- Export classes handle PostgreSQL DDL and REST controller generation

**Schema Resolution Strategy**:
- Handles Oracle synonyms for cross-schema references
- Data type lookup considers table aliases and FROM clause context
- Schema mapping maintains object relationships
- **NEW**: PostgreSQL function name mapping for REST endpoints

## Key Processing Steps

1. **Schema Extraction** - Fetch user schemas, tables, views, synonyms
2. **PL/SQL Code Extraction** - Object types and packages (spec/body)
3. **AST Generation** - Parse PL/SQL using ANTLR grammar
4. **Dependency Resolution** - Build object dependency trees
5. **Code Generation** - Export PostgreSQL DDL and minimal REST controllers
6. **Pre-Transfer SQL Execution** - Create schemas, tables, functions
7. **Data Transfer** - High-performance Oracle to PostgreSQL data migration
8. **Post-Transfer SQL Execution** - Apply constraints, indexes, views

## REST API Processing Pipeline

When called via REST endpoints, the migration follows this flow:

1. **POST /migration/extract** → Schema and PL/SQL extraction from Oracle
2. **POST /migration/parse** → ANTLR parsing into AST representations
3. **POST /migration/export** → Generate PostgreSQL functions + REST controllers
4. **POST /migration/execute-pre** → Execute pre-transfer SQL (schema & tables)
5. **POST /migration/transferdata** → Transfer table data with progress tracking
6. **POST /migration/execute-post** → Execute post-transfer SQL (constraints & objects)
7. **POST /migration/full** → Run all steps in sequence

Each step can be called independently or as part of the full pipeline.

## Development Configuration

The project uses feature flags in `application.properties` to control processing:

### Core Processing Flags
- `do.extract=true` - Enable database extraction
- `do.parse=true` - Enable AST parsing
- `do.write-postgre-files=true` - Generate PostgreSQL DDL
- `do.execute-postgre-files=true` - Execute generated PostgreSQL scripts
- `do.data=true` - Enable data transfer (uses new DataTransferService)

### REST Controller Generation (NEW)
- `do.write-rest-controllers=true` - Generate REST controllers
- `do.rest-controller-functions=true` - Include function endpoints
- `do.rest-controller-procedures=true` - Include procedure endpoints  
- `do.rest-simple-dtos=false` - Generate simple DTOs for complex types

### Legacy Configuration (DEPRECATED)
- `do.write-java-files=false` - Complex Java generation (now deprecated)

### Component Flags
- Individual flags for tables, views, packages, object types
- Target paths are configurable for generated code output to separate projects

**Migration Note**: The project has completed migration from complex Java business logic generation (`do.write-java-files`, deprecated) to PostgreSQL-first REST controllers (`do.write-rest-controllers=true`).

## Quarkus Web Application Status

### Current Status: Fully transformed to Quarkus web application ✅

**Background**: The project has been successfully transformed from a CLI tool to a Quarkus web service supporting both migration operations and runtime SQL transformation.

**Completed Transformation Phases**:

**✅ Phase 1 - Project Setup** (COMPLETED):
- Added Quarkus dependencies (resteasy-reactive-jackson, scheduler, arc)
- Converted Everything class to @ApplicationScoped singleton bean
- Converted Config utility class to @ApplicationScoped injectable service

**✅ Phase 2 - REST Controller Creation** (COMPLETED):
- Main.java serves as MigrationController with endpoints:
  - `POST /migration/extract` - Oracle database extraction
  - `POST /migration/parse` - ANTLR parsing to AST
  - `POST /migration/export` - PostgreSQL + REST controller generation
  - `POST /migration/execute` - PostgreSQL DDL execution
  - `GET /migration/status/{jobId}` - Job status checking
  - `POST /migration/full` - Complete migration pipeline

**✅ Phase 3 - Async Job Management** (COMPLETED):
- JobManager service for long-running operations
- Background task execution with progress tracking
- Job status and result storage

**✅ Phase 4 - PostgreSQL-First Architecture** (COMPLETED):
- Replaced complex Java code generation with minimal REST controllers
- Business logic stays exclusively in PostgreSQL functions
- REST controllers call PostgreSQL functions via JDBC
- Eliminated code duplication between Java and PostgreSQL

**✅ Phase 5 - Data Transfer System** (COMPLETED):
- High-performance data transfer with hybrid CSV/SQL strategies
- Real-time progress tracking and monitoring
- Memory-efficient streaming for large datasets
- Strategy pattern for extensible transfer methods

**🚧 Phase 6 - Object Type Special Cases** (IN PROGRESS):
- Oracle object type to PostgreSQL JSON/JSONB conversion
- Integration with existing AST infrastructure
- Enhanced data type mapping for complex structures

**📋 Phase 7 - Runtime SQL Transformation** (PLANNED):
- POST /transform/sql endpoint for dynamic SQL transformation
- Uses full Everything context for synonym/schema resolution
- Target for external applications to call at runtime

### Current Run Commands
```bash
# Primary development method
mvn quarkus:dev

# Production build and run
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar

# Legacy CLI method (deprecated, but still supported)
mvn exec:java -Dexec.mainClass="com.sayiza.oracle2postgre.Main"
```

## Generated Code Architecture

### REST Controllers (NEW)
Generated REST controllers follow this pattern:
```java
@ApplicationScoped
@Path("/schema/package")
@Produces(MediaType.APPLICATION_JSON)
public class PackageController {
    @Inject DataSource dataSource;
    
    @GET @Path("/functionName")
    public Response functionName(@QueryParam("param") String param) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT * FROM SCHEMA.PACKAGE_functionname(?)";
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setString(1, param);
                var resultSet = stmt.executeQuery();
                return Response.ok("Function result").build();
            }
        } catch (SQLException e) {
            return Response.serverError()
                .entity("Database error: " + e.getMessage()).build();
        }
    }
}
```

### PostgreSQL Functions
Oracle functions become PostgreSQL functions with this naming pattern:
- Oracle: `SCHEMA.PACKAGE.FUNCTION_NAME` 
- PostgreSQL: `SCHEMA.PACKAGE_function_name`

### Key Files and Classes

**REST Generation**:
- `RestControllerGenerator.java` - Core REST endpoint generation
- `ExportRestControllers.java` - REST controller file management  
- `SimpleDtoGenerator.java` - Basic DTO generation for complex types

**Configuration**:
- `CONFIG_MIGRATION.md` - Detailed migration guide
- `ConfigTest.java` - Configuration verification

**Tests**:
- `RestControllerGeneratorTest.java` - Demonstrates generated controller output

## Data Transfer System Status

### Current Implementation: High-Performance Data Migration ✅

**Completed Features (Phases 1-3)**:
- ✅ **Hybrid Transfer Strategy**: CSV streaming for simple tables, SQL generation for complex tables
- ✅ **Memory-Efficient Processing**: Constant memory usage regardless of table size
- ✅ **Real-Time Progress Tracking**: Table-level and session-level monitoring
- ✅ **Intelligent Strategy Selection**: Automatic detection of optimal transfer method
- ✅ **Complex Data Type Support**: CLOB, BLOB, RAW, timestamps handled properly
- ✅ **Integration with REST API**: `/migration/transferdata` endpoint with job tracking

**Current Architecture**:
```
DataTransferService (Orchestrator)
├── TableAnalyzer → Strategy Selection
├── StreamingCsvStrategy → PostgreSQL COPY FROM (simple tables)
├── SQL Generation Fallback → Complex data types
├── TransferProgress → Real-time monitoring
└── TransferResult → Detailed reporting
```

**Current Focus: Phase 4 - Object Type Special Cases** 🚧
- **Goal**: Handle Oracle Object Types using existing AST infrastructure
- **Approach**: Convert Oracle objects to PostgreSQL JSON/JSONB format
- **Integration**: Leverage `Everything.getObjectTypeSpecAst()` for type definitions
- **Status**: Ready for implementation

**Implementation Path**:
1. Enhance TableAnalyzer to detect Oracle object types in table columns
2. Create ObjectTypeMappingStrategy for tables with user-defined object types
3. Implement ObjectTypeMapper for Oracle object → JSON conversion
4. Generate appropriate PostgreSQL DDL with JSONB columns

**Configuration**:
```properties
# Data transfer settings
data.transfer.batch.size=10000
data.transfer.fetch.size=5000
data.transfer.strategy.fallback=true
```

**Key Classes**:
- `DataTransferService.java` - Main orchestrator
- `TableAnalyzer.java` - Strategy selection logic
- `StreamingCsvStrategy.java` - High-performance CSV transfer
- `TransferProgress.java` - Progress tracking
- `TransferResult.java` - Results and metrics