package me.christianrobert.ora2postgre.global;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class Config {

  @ConfigProperty(name = "oracle.url")
  String oracleUrl;
  
  @ConfigProperty(name = "oracle.user")
  String oracleUser;
  
  @ConfigProperty(name = "oracle.password")
  String oraclePassword;
  
  @ConfigProperty(name = "postgre.url")
  String postgreUrl;
  
  @ConfigProperty(name = "postgre.username")
  String postgreUsername;
  
  @ConfigProperty(name = "postgre.password")
  String postgrePassword;
  
  @ConfigProperty(name = "path.target-project-root")
  String pathTargetProjectRoot;
  
  @ConfigProperty(name = "path.target-project-java")
  String pathTargetProjectJava;
  
  @ConfigProperty(name = "path.target-project-resources")
  String pathTargetProjectResources;
  
  @ConfigProperty(name = "path.target-project-postgre")
  String pathTargetProjectPostgre;
  
  @ConfigProperty(name = "java.generated-package-name")
  String generatedJavaPackageName;
  
  @ConfigProperty(name = "do.all-schemas", defaultValue = "true")
  boolean doAllSchemas;
  
  @ConfigProperty(name = "do.only-test-schema", defaultValue = "")
  String doOnlyTestSchema;

  public String getGeneratedJavaPackageName() {
    return generatedJavaPackageName;
  }

  public String getOracleUrl() {
    return oracleUrl;
  }

  public String getOracleUser() {
    return oracleUser;
  }

  public String getOraclePassword() {
    return oraclePassword;
  }

  public String getPostgreUrl() {
    return postgreUrl;
  }

  public String getPostgreUsername() {
    return postgreUsername;
  }

  public String getPostgrePassword() {
    return postgrePassword;
  }

  public String getPathToTargetProjectRoot() {
    return pathTargetProjectRoot;
  }

  public String getPathTargetProjectPostgre() {
    return pathTargetProjectPostgre;
  }

  public String getPathTargetProjectJava() {
    return pathTargetProjectJava;
  }

  public String getPathTargetProjectResources() {
    return pathTargetProjectResources;
  }

  public List<String> getDoOnlyTestSchema() {
    return Arrays.stream(doOnlyTestSchema.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
  }

  @ConfigProperty(name = "do.add-test-data", defaultValue = "false")
  boolean doAddTestData;
  
  @ConfigProperty(name = "do.table", defaultValue = "true")
  boolean doTable;
  
  @ConfigProperty(name = "do.synonyms", defaultValue = "true")
  boolean doSynonyms;
  
  @ConfigProperty(name = "do.data", defaultValue = "true")
  boolean doData;
  
  @ConfigProperty(name = "do.object-type-spec", defaultValue = "true")
  boolean doObjectTypeSpec;
  
  @ConfigProperty(name = "do.object-type-body", defaultValue = "true")
  boolean doObjectTypeBody;
  
  @ConfigProperty(name = "do.package-spec", defaultValue = "true")
  boolean doPackageSpec;
  
  @ConfigProperty(name = "do.package-body", defaultValue = "true")
  boolean doPackageBody;
  
  @ConfigProperty(name = "do.view-signature", defaultValue = "true")
  boolean doViewSignature;
  
  @ConfigProperty(name = "do.view-ddl", defaultValue = "true")
  boolean doViewDdl;
  
  @ConfigProperty(name = "do.triggers", defaultValue = "true")
  boolean doTriggers;
  
  @ConfigProperty(name = "do.extract", defaultValue = "true")
  boolean doExtract;
  
  @ConfigProperty(name = "do.parse", defaultValue = "true")
  boolean doParse;
  
  // DEPRECATED - Complex Java code generation removed in favor of PostgreSQL-first approach
  @ConfigProperty(name = "do.write-java-files", defaultValue = "false")
  boolean doWriteJavaFiles;
  
  @ConfigProperty(name = "do.write-rest-controllers", defaultValue = "true")
  boolean doWriteRestControllers;
  
  @ConfigProperty(name = "do.rest-controller-functions", defaultValue = "true") 
  boolean doRestControllerFunctions;
  
  @ConfigProperty(name = "do.rest-controller-procedures", defaultValue = "true")
  boolean doRestControllerProcedures;
  
  @ConfigProperty(name = "do.rest-simple-dtos", defaultValue = "false")
  boolean doRestSimpleDtos;
  
  @ConfigProperty(name = "do.write-postgre-files", defaultValue = "true")
  boolean doWritePostgreFiles;

  @ConfigProperty(name = "do.execute-postgre-files", defaultValue = "true")
  boolean doExecutePostgreFiles;

  public boolean isDoAddTestData() {
    return doAddTestData;
  }

  public boolean isDoTable() {
    return doTable;
  }

  public boolean isDoSynonyms() {
    return doSynonyms;
  }

  public boolean isDoData() {
    return doData;
  }

  public boolean isDoObjectTypeSpec() {
    return doObjectTypeSpec;
  }

  public boolean isDoObjectTypeBody() {
    return doObjectTypeBody;
  }

  public boolean isDoPackageSpec() {
    return doPackageSpec;
  }

  public boolean isDoPackageBody() {
    return doPackageBody;
  }

  public boolean isDoViewSignature() {
    return doViewSignature;
  }

  public boolean isDoViewDdl() {
    return doViewDdl;
  }

  public boolean isDoTriggers() {
    return doTriggers;
  }

  public boolean isDoWriteRestControllers() {
    return doWriteRestControllers;
  }

  public boolean isDoRestControllerFunctions() {
    return doRestControllerFunctions;
  }

  public boolean isDoRestControllerProcedures() {
    return doRestControllerProcedures;
  }

  public boolean isDoRestSimpleDtos() {
    return doRestSimpleDtos;
  }

  public boolean isDoWritePostgreFiles() {
    return doWritePostgreFiles;
  }

  public boolean isDoExecutePostgreFiles() {
    return doExecutePostgreFiles;
  }

  public boolean isDoAllSchemas() {
    return doAllSchemas;
  }
}
