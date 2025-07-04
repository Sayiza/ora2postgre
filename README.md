# Ora to Postgre Transformation API

A comprehensive REST API for migrating a popular but expensive database that's name starts with Ora to PostgreSQL. This tool automates the extraction, parsing, and transformation of Ora schemas, PL/SQL code, and data into PostgreSQL-compatible formats.

## 🎯 Overview

This application implements a sophisticated migration pipeline that converts Ora databases to PostgreSQL while preserving business logic and ensuring data integrity. The **PostgreSQL-first approach** places all business logic in PostgreSQL functions, with an optional minimal REST controllers layer serving as thin API proxies.

### Key Features

- **🔄 Complete Migration Pipeline**: Automated 6-phase process from Ora extraction to PostgreSQL deployment
- **🧠 Intelligent PL/SQL Parsing**: Uses ANTLR4 grammar for accurate code transformation
- **⚡ High-Performance Data Transfer**: Parallel processing with intelligent type conversion
- **🔧 PostgreSQL-First Architecture**: Business logic stays in database functions, not Java code
- **📊 Real-Time Progress Tracking**: Detailed job monitoring with progress percentages
- **🌐 REST API Interface**: Comprehensive OpenAPI-documented endpoints
- **🔍 Advanced Schema Resolution**: Handles Ora synonyms and cross-schema references

## 🏗️ Architecture

### PostgreSQL-First Design Philosophy

```
Ora PL/SQL Packages → PostgreSQL Functions → Minimal REST Controllers
        ↓                        ↓                        ↓
   Business Logic         Business Logic           API Proxy Only
```

**Benefits:**
- ✅ **No Code Duplication**: Single source of truth for business logic
- ✅ **Direct View Access**: Database views can call the same functions exposed via APIs
- ✅ **Simplified Maintenance**: Logic changes only require database updates
- ✅ **Better Performance**: Reduced network overhead and data transfer

### Core Components

- **🔍 Metadata Extractor**: Connects to Ora and extracts comprehensive schema information
- **📝 ANTLR Parser**: Generates Abstract Syntax Trees from PL/SQL source code
- **🔄 Code Generator**: Transforms AST into PostgreSQL functions and REST controllers
- **📡 REST API Layer**: Quarkus-based web service with comprehensive OpenAPI documentation
- **⚙️ Job Manager**: Asynchronous task execution with progress tracking
- **🗄️ Data Transfer Engine**: High-performance bulk data migration with type conversion

## 🚀 Migration Workflow

The migration process follows a carefully orchestrated 6-phase pipeline:

### Phase 1: 📥 Extract Ora Metadata
```
POST /migration/extract
```
- Connects to Ora database using configured credentials
- Extracts schemas, tables, views, synonyms, and PL/SQL code
- Performs statistical analysis for row count estimation
- Builds comprehensive metadata repository

### Phase 2: 🔍 Parse PL/SQL to AST
```
POST /migration/parse
```
- Processes extracted PL/SQL using ANTLR4 grammar
- Generates Abstract Syntax Trees for accurate transformation
- Resolves dependencies and cross-schema references
- Prepares semantic representation for code generation

### Phase 3: ⚙️ Generate PostgreSQL Code
```
POST /migration/export
```
- Transforms AST into PostgreSQL functions (business logic)
- Generates minimal JAX-RS REST controllers (API proxies)
- Creates DDL scripts for tables, constraints, and indexes
- Handles Ora-to-PostgreSQL data type mappings

### Phase 4A: 🏗️ Execute Pre-Transfer SQL
```
POST /migration/execute-pre
```
- Creates PostgreSQL schemas and tables
- Installs generated business logic functions
- Sets up basic constraints and sequences
- Prepares database structure for data loading

### Phase 5: 📊 Transfer Data
```
POST /migration/transferdata
```
- Performs high-performance bulk data transfer
- Handles Ora ANYDATA to PostgreSQL JSONB conversion
- Uses parallel processing for optimal throughput
- Provides real-time progress tracking

### Phase 4B: 🔧 Execute Post-Transfer SQL
```
POST /migration/execute-post
```
- Applies foreign key constraints and complex indexes
- Creates PostgreSQL views with converted queries
- Finalizes database structure and relationships
- Updates PostgreSQL statistics for optimal performance

### Complete Pipeline: 🎯 Full Migration
```
POST /migration/full
```
Executes all phases in sequence with comprehensive progress tracking.

## 📋 API Endpoints

### Migration Operations
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/migration/extract` | POST | Extract Ora database metadata |
| `/migration/parse` | POST | Parse PL/SQL code to AST |
| `/migration/export` | POST | Generate PostgreSQL code & REST controllers |
| `/migration/execute-pre` | POST | Execute pre-transfer SQL (schema & tables) |
| `/migration/execute-post` | POST | Execute post-transfer SQL (constraints & objects) |
| `/migration/transferdata` | POST | Transfer table data Ora → PostgreSQL |
| `/migration/full` | POST | 🚀 Execute complete migration pipeline |

### Monitoring & Status
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/migration/status` | GET | 📊 Get current migration status & statistics |
| `/migration/jobs/{jobId}` | GET | 🔍 Get detailed job status & progress |

### Interactive Documentation
- **Swagger UI**: `http://localhost:8080/q/swagger-ui`
- **OpenAPI Spec**: `http://localhost:8080/q/openapi`

## ⚙️ Configuration

### Database Connections

Configure Ora and PostgreSQL connections in `src/main/resources/application.properties`:

```properties
# Ora Database
oracle.url=jdbc:oracle:thin:@hostname:1521:service
oracle.user=username
oracle.password=password

# PostgreSQL Database  
postgre.url=jdbc:postgresql://localhost:5432/postgres
postgre.username=postgres
postgre.password=password
```

### Migration Settings

```properties
# Processing Flags
do.extract=true
do.parse=true
do.write-postgre-files=true
do.execute-postgre-files=true
do.write-rest-controllers=true

# REST Controller Generation
do.rest-controller-functions=true
do.rest-controller-procedures=true
do.rest-simple-dtos=false

# Target Project Paths
path.target-project-root=../target-project
path.target-project-java=/src/main/java
path.target-project-resources=/src/main/resources
path.target-project-postgre=/postgre/autoddl

# Generated Code Package
java.generated-package-name=com.example.autogen
```

### Schema Selection

```properties
# Process specific schemas only
do.all-schemas=false
do.only-test-schema=SCHEMA1,SCHEMA2

# Component Processing
do.table=true
do.synonyms=true
do.data=true
do.object-type-spec=true
do.object-type-body=true
do.package-spec=true
do.package-body=true
do.view-signature=true
do.view-ddl=true
```

## 🛠️ Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Oracle Database (source)
- PostgreSQL 12+ (target)

### Quick Start

1. **Setup PostgreSQL Test Database**
   ```bash
   docker rm -f pgtest
   docker run --name pgtest -e POSTGRES_PASSWORD=secret -p 5432:5432 -d postgres
   ```

2. **Configure Database Connections**
   ```bash
   # Edit src/main/resources/application.properties
   # Set Oracle and PostgreSQL connection details
   ```

3. **Start the Application**
   ```bash
   mvn quarkus:dev
   ```

4. **Access Interactive Documentation**
   ```
   http://localhost:8080/q/swagger-ui
   ```

5. **Run Complete Migration**
   ```bash
   curl -X POST http://localhost:8080/migration/full
   ```

### Development Mode

```bash
# Start with live reload
mvn quarkus:dev

# Build production package
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## 📊 Monitoring Progress

### Real-Time Job Tracking

```bash
# Start migration and get job ID
curl -X POST http://localhost:8080/migration/full

# Monitor progress (replace with actual job ID)
curl http://localhost:8080/migration/jobs/full-1699123456789

# Get overall status
curl http://localhost:8080/migration/status
```

### Progress Information

Jobs provide detailed progress including:
- **Current Phase**: EXTRACT → PARSE → EXPORT → EXECUTE_PRE → TRANSFERDATA → EXECUTE_POST
- **Completion Percentage**: 0-100%
- **Duration**: Real-time execution time
- **Detailed Statistics**: Tables processed, rows transferred, etc.
- **Error Details**: Comprehensive error reporting if issues occur

## 🔧 Technical Details

### Data Type Conversion

| Oracle Type | PostgreSQL Type | Notes |
|-------------|-----------------|-------|
| `VARCHAR2` | `VARCHAR` | Length preserved |
| `NUMBER` | `NUMERIC` | Precision/scale mapped |
| `DATE` | `TIMESTAMP` | Oracle DATE includes time |
| `CLOB` | `TEXT` | Large text objects |
| `BLOB` | `BYTEA` | Binary data |
| `ANYDATA` | `JSONB` | Structured data conversion |

### Generated Code Structure

**PostgreSQL Functions:**
```sql
-- Oracle: SCHEMA.PACKAGE.FUNCTION_NAME
-- PostgreSQL: SCHEMA.PACKAGE_function_name
CREATE OR REPLACE FUNCTION schema.package_function_name(param1 TEXT)
RETURNS TABLE(result TEXT) AS $$
BEGIN
    -- Converted business logic
END;
$$ LANGUAGE plpgsql;
```

**REST Controllers:**
```java
@ApplicationScoped
@Path("/schema/package")
public class PackageController {
    @Inject DataSource dataSource;
    
    @GET @Path("/functionName")
    public Response functionName(@QueryParam("param") String param) {
        // Calls PostgreSQL function directly
        return callPostgreSQLFunction("schema.package_function_name", param);
    }
}
```

## 🤝 Contributing

This project follows enterprise development standards:

- **Code Quality**: Comprehensive error handling and logging
- **Testing**: Unit and integration test coverage
- **Documentation**: Extensive OpenAPI documentation
- **Monitoring**: Built-in progress tracking and job management
- **Configuration**: Flexible property-based configuration

## 📄 License

Proprietary - Internal Use Only

---

For detailed API documentation, visit the interactive Swagger UI at `http://localhost:8080/q/swagger-ui` when the application is running.