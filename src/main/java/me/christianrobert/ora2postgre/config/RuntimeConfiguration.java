package me.christianrobert.ora2postgre.config;

public class RuntimeConfiguration {

  // Process flags
  private Boolean doAllSchemas;
  private String doOnlyTestSchema;
  private Boolean doTable;
  private Boolean doSynonyms;
  private Boolean doData;
  private Boolean doObjectTypeSpec;
  private Boolean doObjectTypeBody;
  private Boolean doPackageSpec;
  private Boolean doPackageBody;
  private Boolean doViewSignature;
  private Boolean doViewDdl;
  private Boolean doTriggers;
  private Boolean doConstraints;
  private Boolean doIndexes;
  private Boolean doWriteRestControllers;
  private Boolean doWritePostgreFiles;
  private Boolean doExecutePostgreFiles;
  private Boolean doRestControllerFunctions;
  private Boolean doRestControllerProcedures;

  // Connection settings
  private String oracleUrl;
  private String oracleUser;
  private String oraclePassword;
  private String postgreUrl;
  private String postgreUsername;
  private String postgrePassword;

  // Path settings
  private String javaGeneratedPackageName;
  private String pathTargetProjectRoot;
  private String pathTargetProjectJava;
  private String pathTargetProjectResources;
  private String pathTargetProjectPostgre;

  public Boolean getDoAllSchemas() {
    return doAllSchemas;
  }

  public void setDoAllSchemas(Boolean doAllSchemas) {
    this.doAllSchemas = doAllSchemas;
  }

  public String getDoOnlyTestSchema() {
    return doOnlyTestSchema;
  }

  public void setDoOnlyTestSchema(String doOnlyTestSchema) {
    this.doOnlyTestSchema = doOnlyTestSchema;
  }

  public Boolean getDoTable() {
    return doTable;
  }

  public void setDoTable(Boolean doTable) {
    this.doTable = doTable;
  }

  public Boolean getDoSynonyms() {
    return doSynonyms;
  }

  public void setDoSynonyms(Boolean doSynonyms) {
    this.doSynonyms = doSynonyms;
  }

  public Boolean getDoData() {
    return doData;
  }

  public void setDoData(Boolean doData) {
    this.doData = doData;
  }

  public Boolean getDoObjectTypeSpec() {
    return doObjectTypeSpec;
  }

  public void setDoObjectTypeSpec(Boolean doObjectTypeSpec) {
    this.doObjectTypeSpec = doObjectTypeSpec;
  }

  public Boolean getDoObjectTypeBody() {
    return doObjectTypeBody;
  }

  public void setDoObjectTypeBody(Boolean doObjectTypeBody) {
    this.doObjectTypeBody = doObjectTypeBody;
  }

  public Boolean getDoPackageSpec() {
    return doPackageSpec;
  }

  public void setDoPackageSpec(Boolean doPackageSpec) {
    this.doPackageSpec = doPackageSpec;
  }

  public Boolean getDoPackageBody() {
    return doPackageBody;
  }

  public void setDoPackageBody(Boolean doPackageBody) {
    this.doPackageBody = doPackageBody;
  }

  public Boolean getDoViewSignature() {
    return doViewSignature;
  }

  public void setDoViewSignature(Boolean doViewSignature) {
    this.doViewSignature = doViewSignature;
  }

  public Boolean getDoViewDdl() {
    return doViewDdl;
  }

  public void setDoViewDdl(Boolean doViewDdl) {
    this.doViewDdl = doViewDdl;
  }

  public Boolean getDoTriggers() {
    return doTriggers;
  }

  public void setDoTriggers(Boolean doTriggers) {
    this.doTriggers = doTriggers;
  }

  public Boolean getDoConstraints() {
    return doConstraints;
  }

  public void setDoConstraints(Boolean doConstraints) {
    this.doConstraints = doConstraints;
  }

  public Boolean getDoIndexes() {
    return doIndexes;
  }

  public void setDoIndexes(Boolean doIndexes) {
    this.doIndexes = doIndexes;
  }

  public Boolean getDoWriteRestControllers() {
    return doWriteRestControllers;
  }

  public void setDoWriteRestControllers(Boolean doWriteRestControllers) {
    this.doWriteRestControllers = doWriteRestControllers;
  }

  public Boolean getDoWritePostgreFiles() {
    return doWritePostgreFiles;
  }

  public void setDoWritePostgreFiles(Boolean doWritePostgreFiles) {
    this.doWritePostgreFiles = doWritePostgreFiles;
  }

  public Boolean getDoExecutePostgreFiles() {
    return doExecutePostgreFiles;
  }

  public void setDoExecutePostgreFiles(Boolean doExecutePostgreFiles) {
    this.doExecutePostgreFiles = doExecutePostgreFiles;
  }

  public Boolean getDoRestControllerFunctions() {
    return doRestControllerFunctions;
  }

  public void setDoRestControllerFunctions(Boolean doRestControllerFunctions) {
    this.doRestControllerFunctions = doRestControllerFunctions;
  }

  public Boolean getDoRestControllerProcedures() {
    return doRestControllerProcedures;
  }

  public void setDoRestControllerProcedures(Boolean doRestControllerProcedures) {
    this.doRestControllerProcedures = doRestControllerProcedures;
  }

  public String getOracleUrl() {
    return oracleUrl;
  }

  public void setOracleUrl(String oracleUrl) {
    this.oracleUrl = oracleUrl;
  }

  public String getOracleUser() {
    return oracleUser;
  }

  public void setOracleUser(String oracleUser) {
    this.oracleUser = oracleUser;
  }

  public String getOraclePassword() {
    return oraclePassword;
  }

  public void setOraclePassword(String oraclePassword) {
    this.oraclePassword = oraclePassword;
  }

  public String getPostgreUrl() {
    return postgreUrl;
  }

  public void setPostgreUrl(String postgreUrl) {
    this.postgreUrl = postgreUrl;
  }

  public String getPostgreUsername() {
    return postgreUsername;
  }

  public void setPostgreUsername(String postgreUsername) {
    this.postgreUsername = postgreUsername;
  }

  public String getPostgrePassword() {
    return postgrePassword;
  }

  public void setPostgrePassword(String postgrePassword) {
    this.postgrePassword = postgrePassword;
  }

  public String getJavaGeneratedPackageName() {
    return javaGeneratedPackageName;
  }

  public void setJavaGeneratedPackageName(String javaGeneratedPackageName) {
    this.javaGeneratedPackageName = javaGeneratedPackageName;
  }

  public String getPathTargetProjectRoot() {
    return pathTargetProjectRoot;
  }

  public void setPathTargetProjectRoot(String pathTargetProjectRoot) {
    this.pathTargetProjectRoot = pathTargetProjectRoot;
  }

  public String getPathTargetProjectJava() {
    return pathTargetProjectJava;
  }

  public void setPathTargetProjectJava(String pathTargetProjectJava) {
    this.pathTargetProjectJava = pathTargetProjectJava;
  }

  public String getPathTargetProjectResources() {
    return pathTargetProjectResources;
  }

  public void setPathTargetProjectResources(String pathTargetProjectResources) {
    this.pathTargetProjectResources = pathTargetProjectResources;
  }

  public String getPathTargetProjectPostgre() {
    return pathTargetProjectPostgre;
  }

  public void setPathTargetProjectPostgre(String pathTargetProjectPostgre) {
    this.pathTargetProjectPostgre = pathTargetProjectPostgre;
  }
}