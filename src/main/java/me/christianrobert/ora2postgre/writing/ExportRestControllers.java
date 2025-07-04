package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.StringAux;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.tools.RestControllerGenerator;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports REST controllers that call PostgreSQL functions directly.
 * This replaces the complex Java code generation with simple database-calling endpoints.
 */
public class ExportRestControllers {
    
    /**
     * Sets up the target project with pom.xml, application.properties, and utility classes.
     * This method handles the project infrastructure needed for REST controllers.
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
     * Generates REST controllers for Oracle packages.
     * Each package becomes a REST controller that calls the corresponding PostgreSQL functions.
     */
    public static void generateControllers(String path, String javaPackageName, 
                                         List<OraclePackage> specs, List<OraclePackage> bodies, 
                                         Everything data) {
        for (OraclePackage pkg : mergeSpecAndBody(specs, bodies)) {
            // Skip packages with no functions or procedures
            if (pkg.getFunctions().isEmpty() && pkg.getProcedures().isEmpty()) {
                continue;
            }
            
            String fullPathAsString = path +
                    File.separator +
                    javaPackageName.replace('.', File.separatorChar) +
                    File.separator +
                    pkg.getSchema().toLowerCase() +
                    File.separator +
                    "controllers";
            
            String className = StringAux.capitalizeFirst(pkg.getName()) + "Controller.java";
            String content = RestControllerGenerator.generateController(pkg, javaPackageName, data);
            
            FileWriter.write(Paths.get(fullPathAsString), className, content);
        }
    }
    
    /**
     * Merges package specs and bodies, preferring body implementations when available.
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
     * Generates the pom.xml content for the target project.
     */
    private static String generatePom() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.example</groupId>
                <artifactId>quarkus-minimal</artifactId>
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
                    <!-- RESTEasy Reactive Jackson for JSON support -->
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
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
     * Generates the application.properties content for the target project.
     */
    private static String generateApplicationProperties(String postgreUrl, String postgreUserName, String postgrePassword) {
        return new StringBuilder()
                .append("quarkus.http.port=8080\n")
                .append("quarkus.datasource.db-kind=postgresql\n")
                .append("quarkus.datasource.jdbc.url=")
                .append(postgreUrl)
                .append("\n")
                .append("quarkus.datasource.username=")
                .append(postgreUserName)
                .append("\n")
                .append("quarkus.datasource.password=")
                .append(postgrePassword)
                .append("\n")
                .toString();
    }
}