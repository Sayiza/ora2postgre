package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.StringAux;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.ModPlsqlSimulatorGenerator;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports mod-plsql simulator controllers that execute PostgreSQL procedures
 * and return generated HTML content via HTP calls.
 * 
 * This replaces the REST controller export functionality with true mod_plsql
 * simulator generation that replicates Oracle's mod_plsql web behavior.
 */
public class ExportModPlsqlSimulator {

  /**
   * Sets up the target project with pom.xml, application.properties, and utility classes
   * configured for mod-plsql simulator functionality.
   */
  public static void setupTargetProject(String rootPath, String pathJava, String pathResources,
                                        String javaPackageName, String postgreUrl,
                                        String postgreUserName, String postgrePassword) {
    // Create pom.xml in root directory
    FileWriter.write(Paths.get(rootPath), "pom.xml", generatePom());

    // Create application.properties in resources directory
    FileWriter.write(Paths.get(pathResources), "application.properties",
            generateApplicationProperties(postgreUrl, postgreUserName, postgrePassword));
  }

  /**
   * Generates mod-plsql simulator controllers for Oracle packages.
   * Each package with procedures becomes a mod-plsql simulator controller.
   */
  public static void generateSimulators(String path, String javaPackageName,
                                        List<OraclePackage> specs, List<OraclePackage> bodies,
                                        Everything data) {
    // Generate the ModPlsqlExecutor utility class first
    generateModPlsqlExecutor(path, javaPackageName);
    
    for (OraclePackage pkg : mergeSpecAndBody(specs, bodies)) {
      // Skip packages with no procedures (mod-plsql only supports procedures)
      if (pkg.getProcedures().isEmpty()) {
        continue;
      }

      String fullPathAsString = path +
              File.separator +
              javaPackageName.replace('.', File.separatorChar) +
              File.separator +
              pkg.getSchema().toLowerCase() +
              File.separator +
              "modplsql";

      String className = StringAux.capitalizeFirst(pkg.getName()) + "ModPlsqlController.java";
      String content = ModPlsqlSimulatorGenerator.generateSimulator(pkg, javaPackageName, data);

      FileWriter.write(Paths.get(fullPathAsString), className, content);
    }
  }

  /**
   * Merges package specs and bodies, preferring body implementations when available.
   * This is the same logic as the REST controller export, ensuring consistency.
   */
  private static List<OraclePackage> mergeSpecAndBody(List<OraclePackage> specs, List<OraclePackage> bodies) {
    List<OraclePackage> merged = new ArrayList<>();

    for (OraclePackage spec : specs) {
      boolean found = false;
      for (OraclePackage body : bodies) {
        if (spec.getName().equals(body.getName()) && spec.getSchema().equals(body.getSchema())) {
          found = true;
          // Use body implementation when available
          merged.add(body);
          break;
        }
      }
      if (!found) {
        // Use spec if no body found
        merged.add(spec);
      }
    }

    // Add any bodies that don't have corresponding specs
    for (OraclePackage body : bodies) {
      boolean hasSpec = specs.stream()
              .anyMatch(spec -> spec.getName().equals(body.getName()) &&
                      spec.getSchema().equals(body.getSchema()));
      if (!hasSpec) {
        merged.add(body);
      }
    }

    return merged;
  }

  /**
   * Generates the pom.xml content for the mod-plsql simulator target project.
   * Updated with dependencies optimized for mod-plsql functionality.
   */
  private static String generatePom() {
    return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.example</groupId>
                <artifactId>mod-plsql-simulator</artifactId>
                <version>1.0-SNAPSHOT</version>

                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    <quarkus.platform.version>3.15.1</quarkus.platform.version>
                </properties>

                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.quarkus.platform</groupId>
                            <artifactId>quarkus-bom</artifactId>
                            <version>${quarkus.platform.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>

                <dependencies>
                    <!-- Core Quarkus dependency for RESTEasy Reactive -->
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-resteasy-reactive</artifactId>
                    </dependency>
                    <!-- PostgreSQL JDBC driver -->
                    <dependency>
                       <groupId>io.quarkus</groupId>
                       <artifactId>quarkus-jdbc-postgresql</artifactId>
                    </dependency>
                    <!-- Agroal datasource for connection pooling -->
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-agroal</artifactId>
                    </dependency>
                    <!-- SmallRye Health for monitoring -->
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-smallrye-health</artifactId>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-maven-plugin</artifactId>
                            <version>${quarkus.platform.version}</version>
                            <executions>
                                <execution>
                                    <goals>
                                        <goal>build</goal>
                                        <goal>generate-code</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.13.0</version>
                            <configuration>
                                <source>${maven.compiler.source}</source>
                                <target>${maven.compiler.target}</target>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
  }

  /**
   * Generates the application.properties content for the mod-plsql simulator project.
   * Optimized for HTML content serving and database connectivity.
   */
  private static String generateApplicationProperties(String postgreUrl, String postgreUserName, String postgrePassword) {
    return new StringBuilder()
            .append("# Mod-PLSQL Simulator Configuration\n")
            .append("quarkus.http.port=8080\n")
            .append("quarkus.http.host=0.0.0.0\n\n")
            
            .append("# PostgreSQL Database Configuration\n")
            .append("quarkus.datasource.db-kind=postgresql\n")
            .append("quarkus.datasource.jdbc.url=")
            .append(postgreUrl)
            .append("\n")
            .append("quarkus.datasource.username=")
            .append(postgreUserName)
            .append("\n")
            .append("quarkus.datasource.password=")
            .append(postgrePassword)
            .append("\n\n")
            
            .append("# Connection Pool Settings for Mod-PLSQL\n")
            .append("quarkus.datasource.jdbc.min-size=5\n")
            .append("quarkus.datasource.jdbc.max-size=20\n")
            .append("quarkus.datasource.jdbc.acquisition-timeout=30s\n\n")
            
            .append("# HTTP Configuration for HTML Content\n")
            .append("quarkus.http.body.handle-file-uploads=false\n")
            .append("quarkus.http.limits.max-body-size=10M\n")
            .append("quarkus.http.read-timeout=60s\n\n")
            
            .append("# Logging Configuration\n")
            .append("quarkus.log.level=INFO\n")
            .append("quarkus.log.category.\"").append("me.christianrobert.ora2postgre\".level=DEBUG\n")
            .toString();
  }

  /**
   * Generates a README.md file with usage instructions for the mod-plsql simulator.
   */
  public static void generateReadme(String rootPath, String javaPackageName) {
    String readmeContent = """
            # Mod-PLSQL Simulator
            
            This project is a generated mod-plsql simulator that replicates Oracle mod_plsql functionality
            using PostgreSQL procedures and Quarkus.
            
            ## Usage
            
            ### Starting the Application
            ```bash
            mvn quarkus:dev
            ```
            
            ### Accessing Procedures
            The mod-plsql simulator exposes procedures using the URL pattern:
            ```
            http://localhost:8080/modplsql/{schema}/{package}/{procedure}?param1=value1&param2=value2
            ```
            
            ### Example
            If you have a procedure `MY_SCHEMA.WEB_PACKAGE.SHOW_REPORT`, access it via:
            ```
            http://localhost:8080/modplsql/my_schema/web_package/show_report?user_id=123&report_type=summary
            ```
            
            ## Features
            
            - **True mod_plsql Compatibility**: Replicates Oracle mod_plsql behavior exactly
            - **HTP Buffer Support**: Procedures use HTP calls to generate HTML content
            - **Dynamic Parameters**: All query parameters are passed to procedures
            - **Error Handling**: Database errors return formatted HTML error pages
            - **No-Cache Headers**: Ensures fresh content on each request
            
            ## Generated Controllers
            
            Controllers are generated for each Oracle package that contains procedures.
            Only procedures are supported (functions are not callable via mod_plsql).
            
            ## Database Requirements
            
            - PostgreSQL database with migrated schema
            - HTP helper functions installed (SYS.HTP_init, SYS.HTP_p, SYS.HTP_page, etc.)
            - Migrated procedures that use HTP calls for HTML generation
            """;

    FileWriter.write(Paths.get(rootPath), "README.md", readmeContent);
  }

  /**
   * Generates the ModPlsqlExecutor utility class in the target project.
   * This class provides the core functionality for executing PostgreSQL procedures with HTP support.
   */
  private static void generateModPlsqlExecutor(String path, String javaPackageName) {
    String utilsPath = path +
            File.separator +
            javaPackageName.replace('.', File.separatorChar) +
            File.separator +
            "utils";

    String content = generateModPlsqlExecutorContent(javaPackageName);
    FileWriter.write(Paths.get(utilsPath), "ModPlsqlExecutor.java", content);
  }

  /**
   * Generates the content for the ModPlsqlExecutor utility class.
   * This is a self-contained utility class that handles HTP buffer management and procedure execution.
   */
  private static String generateModPlsqlExecutorContent(String javaPackageName) {
    StringBuilder sb = new StringBuilder();
    
    sb.append("package ").append(javaPackageName).append(".utils;\n\n");
    
    sb.append("import java.sql.CallableStatement;\n");
    sb.append("import java.sql.Connection;\n");
    sb.append("import java.sql.PreparedStatement;\n");
    sb.append("import java.sql.ResultSet;\n");
    sb.append("import java.sql.Savepoint;\n");
    sb.append("import java.sql.SQLException;\n");
    sb.append("import java.util.Map;\n");
    sb.append("import java.util.StringJoiner;\n\n");
    
    sb.append("/**\n");
    sb.append(" * Utility class for executing PostgreSQL procedures with HTP buffer management.\n");
    sb.append(" * \n");
    sb.append(" * This class provides the core functionality for the mod-plsql simulator by:\n");
    sb.append(" * 1. Initializing the HTP buffer before procedure execution\n");
    sb.append(" * 2. Initializing package variables if needed (session-specific cache)\n");
    sb.append(" * 3. Executing the target procedure with parameters\n");
    sb.append(" * 4. Retrieving the generated HTML content from the HTP buffer\n");
    sb.append(" */\n");
    sb.append("public class ModPlsqlExecutor {\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Session isolation: Each connection gets fresh package variable state.\n");
    sb.append("   * Since we use forced connection closure per request, we always initialize\n");
    sb.append("   * package variables for each fresh connection without caching.\n");
    sb.append("   */\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Initializes the HTP buffer by calling SYS.HTP_init().\n");
    sb.append("   * This creates a fresh temporary table for HTML content generation.\n");
    sb.append("   */\n");
    sb.append("  public static void initializeHtpBuffer(Connection conn) throws SQLException {\n");
    sb.append("    try (CallableStatement stmt = conn.prepareCall(\"CALL SYS.HTP_init()\")) {\n");
    sb.append("      stmt.execute();\n");
    sb.append("    }\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Executes a PostgreSQL procedure with HTP support and returns the generated HTML.\n");
    sb.append("   * \n");
    sb.append("   * @param conn Database connection\n");
    sb.append("   * @param procedureName Fully qualified PostgreSQL procedure name (e.g., \"SCHEMA.PACKAGE_procedure\")\n");
    sb.append("   * @param parameters Map of parameter names to values\n");
    sb.append("   * @return Generated HTML content from HTP buffer\n");
    sb.append("   * @throws SQLException If database operation fails\n");
    sb.append("   */\n");
    sb.append("  public static String executeProcedureWithHtp(Connection conn, String procedureName, \n");
    sb.append("                                               Map<String, String> parameters) throws SQLException {\n");
    sb.append("    // Initialize package variables if this is a package procedure\n");
    sb.append("    initializePackageVariables(conn, procedureName);\n");
    sb.append("    \n");
    sb.append("    // Execute the procedure with parameters\n");
    sb.append("    executeProcedure(conn, procedureName, parameters);\n");
    sb.append("    \n");
    sb.append("    // Retrieve and return the generated HTML\n");
    sb.append("    return getHtmlFromBuffer(conn);\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Executes a PostgreSQL procedure with the provided parameters.\n");
    sb.append("   * Parameters are passed as strings and the database handles type conversion.\n");
    sb.append("   */\n");
    sb.append("  private static void executeProcedure(Connection conn, String procedureName, \n");
    sb.append("                                       Map<String, String> parameters) throws SQLException {\n");
    sb.append("    if (parameters.isEmpty()) {\n");
    sb.append("      // Simple case: no parameters\n");
    sb.append("      try (CallableStatement stmt = conn.prepareCall(\"CALL \" + procedureName + \"()\")) {\n");
    sb.append("        stmt.execute();\n");
    sb.append("      }\n");
    sb.append("    } else {\n");
    sb.append("      // Build parameterized call\n");
    sb.append("      StringJoiner placeholders = new StringJoiner(\", \");\n");
    sb.append("      for (int i = 0; i < parameters.size(); i++) {\n");
    sb.append("        placeholders.add(\"?\");\n");
    sb.append("      }\n");
    sb.append("      \n");
    sb.append("      String sql = \"CALL \" + procedureName + \"(\" + placeholders.toString() + \")\";\n");
    sb.append("      \n");
    sb.append("      try (CallableStatement stmt = conn.prepareCall(sql)) {\n");
    sb.append("        // Set parameters in order (note: parameter order matters!)\n");
    sb.append("        int paramIndex = 1;\n");
    sb.append("        for (String value : parameters.values()) {\n");
    sb.append("          if (value == null || value.trim().isEmpty()) {\n");
    sb.append("            stmt.setNull(paramIndex, java.sql.Types.VARCHAR);\n");
    sb.append("          } else {\n");
    sb.append("            stmt.setString(paramIndex, value);\n");
    sb.append("          }\n");
    sb.append("          paramIndex++;\n");
    sb.append("        }\n");
    sb.append("        \n");
    sb.append("        stmt.execute();\n");
    sb.append("      }\n");
    sb.append("    }\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Retrieves the complete HTML content from the HTP buffer.\n");
    sb.append("   * Calls SYS.HTP_page() to get the concatenated HTML output.\n");
    sb.append("   */\n");
    sb.append("  private static String getHtmlFromBuffer(Connection conn) throws SQLException {\n");
    sb.append("    try (PreparedStatement stmt = conn.prepareStatement(\"SELECT SYS.HTP_page()\")) {\n");
    sb.append("      try (ResultSet rs = stmt.executeQuery()) {\n");
    sb.append("        if (rs.next()) {\n");
    sb.append("          String html = rs.getString(1);\n");
    sb.append("          return html != null ? html : \"\";\n");
    sb.append("        }\n");
    sb.append("        return \"\";\n");
    sb.append("      }\n");
    sb.append("    }\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Utility method to flush the HTP buffer (clear contents).\n");
    sb.append("   * Useful for testing or error recovery scenarios.\n");
    sb.append("   */\n");
    sb.append("  public static void flushHtpBuffer(Connection conn) throws SQLException {\n");
    sb.append("    try (CallableStatement stmt = conn.prepareCall(\"CALL SYS.HTP_flush()\")) {\n");
    sb.append("      stmt.execute();\n");
    sb.append("    }\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Public method to force package variable initialization for a specific procedure.\n");
    sb.append("   * This is useful for testing session isolation and ensuring package variables\n");
    sb.append("   * are properly initialized for each fresh connection.\n");
    sb.append("   * \n");
    sb.append("   * @param conn Database connection\n");
    sb.append("   * @param procedureName Fully qualified PostgreSQL procedure name\n");
    sb.append("   * @throws SQLException If database operation fails\n");
    sb.append("   */\n");
    sb.append("  public static void forcePackageVariableInitialization(Connection conn, String procedureName) throws SQLException {\n");
    sb.append("    initializePackageVariables(conn, procedureName);\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Initializes package variables for a given procedure by calling the package initialization procedure.\n");
    sb.append("   * This method extracts the package name from the procedure name and calls the corresponding\n");
    sb.append("   * initialization procedure (e.g., \"SCHEMA.PACKAGE_init_variables\").\n");
    sb.append("   * \n");
    sb.append("   * IMPORTANT: No caching is used to ensure fresh package variable state for each request.\n");
    sb.append("   * This provides proper session isolation when connections are closed per request.\n");
    sb.append("   * \n");
    sb.append("   * @param conn Database connection (should be a fresh connection per request)\n");
    sb.append("   * @param procedureName Fully qualified PostgreSQL procedure name (e.g., \"SCHEMA.PACKAGE_procedure\")\n");
    sb.append("   * @throws SQLException If database operation fails\n");
    sb.append("   */\n");
    sb.append("  private static void initializePackageVariables(Connection conn, String procedureName) throws SQLException {\n");
    sb.append("    String packageInitProcedure = getPackageInitializationProcedure(procedureName);\n");
    sb.append("    if (packageInitProcedure != null) {\n");
    sb.append("      // Use savepoint to isolate package variable initialization attempt\n");
    sb.append("      // This prevents transaction abort if initialization function doesn't exist\n");
    sb.append("      Savepoint savepoint = conn.setSavepoint(\"package_init\");\n");
    sb.append("      try (CallableStatement stmt = conn.prepareCall(\"SELECT \" + packageInitProcedure + \"()\")) {\n");
    sb.append("        stmt.execute();\n");
    sb.append("        // Success - package variables are now initialized with default values\n");
    sb.append("        conn.releaseSavepoint(savepoint);\n");
    sb.append("      } catch (SQLException e) {\n");
    sb.append("        // Rollback to savepoint instead of aborting entire transaction\n");
    sb.append("        conn.rollback(savepoint);\n");
    sb.append("        // This allows procedures without package variables to work normally\n");
    sb.append("        // The main transaction remains valid for subsequent procedure execution\n");
    sb.append("        // Log the error for debugging if needed\n");
    sb.append("        // System.err.println(\"Package initialization failed for \" + packageInitProcedure + \": \" + e.getMessage());\n");
    sb.append("      }\n");
    sb.append("    }\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Extracts the package initialization procedure name from a procedure name.\n");
    sb.append("   * Converts \"SCHEMA.PACKAGE_procedure\" to \"SCHEMA.PACKAGE_init_variables\".\n");
    sb.append("   * Returns null if the procedure doesn't appear to be a package procedure.\n");
    sb.append("   * \n");
    sb.append("   * IMPORTANT: This method uses a heuristic approach. For packages with underscores\n");
    sb.append("   * in their names, it tries to identify common procedure naming patterns to avoid\n");
    sb.append("   * truncating the package name incorrectly.\n");
    sb.append("   * \n");
    sb.append("   * @param procedureName Fully qualified PostgreSQL procedure name\n");
    sb.append("   * @return Package initialization procedure name or null\n");
    sb.append("   */\n");
    sb.append("  private static String getPackageInitializationProcedure(String procedureName) {\n");
    sb.append("    if (procedureName == null || !procedureName.contains(\".\")) {\n");
    sb.append("      return null;\n");
    sb.append("    }\n");
    sb.append("    \n");
    sb.append("    String[] parts = procedureName.split(\"\\\\.\");\n");
    sb.append("    if (parts.length != 2) {\n");
    sb.append("      return null;\n");
    sb.append("    }\n");
    sb.append("    \n");
    sb.append("    String schema = parts[0];\n");
    sb.append("    String nameWithProcedure = parts[1];\n");
    sb.append("    \n");
    sb.append("    // Check if this looks like a package procedure (contains underscore)\n");
    sb.append("    if (!nameWithProcedure.contains(\"_\")) {\n");
    sb.append("      return null;\n");
    sb.append("    }\n");
    sb.append("    \n");
    sb.append("    // Enhanced heuristic to handle packages with underscores\n");
    sb.append("    String packageName = extractPackageName(nameWithProcedure);\n");
    sb.append("    if (packageName == null) {\n");
    sb.append("      return null;\n");
    sb.append("    }\n");
    sb.append("    \n");
    sb.append("    return schema + \".\" + packageName + \"_init_variables\";\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Enhanced package name extraction with common procedure pattern recognition.\n");
    sb.append("   * This method attempts to identify where the package name ends and procedure name begins\n");
    sb.append("   * by recognizing common procedure naming patterns.\n");
    sb.append("   * \n");
    sb.append("   * @param nameWithProcedure The part after schema (PACKAGE_procedure_name)\n");
    sb.append("   * @return Extracted package name or null if pattern not recognized\n");
    sb.append("   */\n");
    sb.append("  private static String extractPackageName(String nameWithProcedure) {\n");
    sb.append("    // Common procedure name patterns to recognize\n");
    sb.append("    String[] commonProcedureWords = {\n");
    sb.append("      \"get\", \"set\", \"create\", \"update\", \"delete\", \"insert\", \"select\", \"process\",\n");
    sb.append("      \"execute\", \"run\", \"perform\", \"handle\", \"manage\", \"generate\", \"validate\",\n");
    sb.append("      \"check\", \"verify\", \"send\", \"receive\", \"load\", \"save\", \"export\", \"import\",\n");
    sb.append("      \"show\", \"display\", \"render\", \"format\", \"parse\", \"convert\", \"transform\",\n");
    sb.append("      \"login\", \"logout\", \"register\", \"authenticate\", \"authorize\", \"calculate\",\n");
    sb.append("      \"compute\", \"analyze\", \"report\", \"summarize\", \"list\", \"find\", \"search\"\n");
    sb.append("    };\n");
    sb.append("    \n");
    sb.append("    // Try to find package boundary by looking for common procedure word patterns\n");
    sb.append("    for (String procWord : commonProcedureWords) {\n");
    sb.append("      // Look for _word pattern (e.g., _get_user, _process_order)\n");
    sb.append("      String pattern1 = \"_\" + procWord + \"_\";\n");
    sb.append("      String pattern2 = \"_\" + procWord;\n");
    sb.append("      \n");
    sb.append("      int index = nameWithProcedure.toLowerCase().indexOf(pattern1.toLowerCase());\n");
    sb.append("      if (index > 0) {\n");
    sb.append("        return nameWithProcedure.substring(0, index);\n");
    sb.append("      }\n");
    sb.append("      \n");
    sb.append("      // Check if procedure ends with this word\n");
    sb.append("      if (nameWithProcedure.toLowerCase().endsWith(pattern2.toLowerCase()) && \n");
    sb.append("          nameWithProcedure.length() > pattern2.length()) {\n");
    sb.append("        int endIndex = nameWithProcedure.length() - pattern2.length();\n");
    sb.append("        if (endIndex > 0) {\n");
    sb.append("          return nameWithProcedure.substring(0, endIndex);\n");
    sb.append("        }\n");
    sb.append("      }\n");
    sb.append("    }\n");
    sb.append("    \n");
    sb.append("    // Additional check: if name has only one underscore and doesn't match patterns,\n");
    sb.append("    // it's likely a standalone procedure with underscore in name (e.g., STANDALONE_PROC)\n");
    sb.append("    long underscoreCount = nameWithProcedure.chars().filter(ch -> ch == '_').count();\n");
    sb.append("    if (underscoreCount == 1) {\n");
    sb.append("      // Check if it's likely a standalone procedure by looking at the part after underscore\n");
    sb.append("      String afterUnderscore = nameWithProcedure.substring(nameWithProcedure.indexOf('_') + 1);\n");
    sb.append("      // If the part after underscore is a simple word (like PROC, PROCEDURE, FUNC), \n");
    sb.append("      // it's probably a standalone procedure\n");
    sb.append("      if (afterUnderscore.toLowerCase().matches(\"(proc|procedure|func|function|test|demo|util|helper)\")) {\n");
    sb.append("        return null;\n");
    sb.append("      }\n");
    sb.append("    }\n");
    sb.append("    \n");
    sb.append("    // Fallback: use the old logic as last resort\n");
    sb.append("    // This handles simple cases but may still fail for complex package names\n");
    sb.append("    int lastUnderscoreIndex = nameWithProcedure.lastIndexOf(\"_\");\n");
    sb.append("    if (lastUnderscoreIndex > 0) {\n");
    sb.append("      return nameWithProcedure.substring(0, lastUnderscoreIndex);\n");
    sb.append("    }\n");
    sb.append("    \n");
    sb.append("    return null;\n");
    sb.append("  }\n\n");
    
    sb.append("  /**\n");
    sb.append("   * Gets the current size of the HTP buffer.\n");
    sb.append("   * Useful for debugging or monitoring HTML generation.\n");
    sb.append("   */\n");
    sb.append("  public static int getHtpBufferSize(Connection conn) throws SQLException {\n");
    sb.append("    try (PreparedStatement stmt = conn.prepareStatement(\"SELECT SYS.HTP_buffer_size()\")) {\n");
    sb.append("      try (ResultSet rs = stmt.executeQuery()) {\n");
    sb.append("        if (rs.next()) {\n");
    sb.append("          return rs.getInt(1);\n");
    sb.append("        }\n");
    sb.append("        return 0;\n");
    sb.append("      }\n");
    sb.append("    }\n");
    sb.append("  }\n\n");
    
    sb.append("}\n");
    
    return sb.toString();
  }
}