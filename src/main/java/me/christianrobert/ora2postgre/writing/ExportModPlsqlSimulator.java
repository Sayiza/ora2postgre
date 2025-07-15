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
}