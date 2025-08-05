package me.christianrobert.ora2postgre.global;

import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.oracledb.SynonymMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.TableReference;
import me.christianrobert.ora2postgre.plsql.ast.Trigger;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class Everything {
  // raw data from the database
  private List<String> userNames = new ArrayList<>();
  private List<TableMetadata> tableSql = new ArrayList<>();
  
  
  private List<ViewMetadata> viewDefinition = new ArrayList<>();
  private List<SynonymMetadata> synonyms = new ArrayList<>();
  private List<IndexMetadata> indexes = new ArrayList<>();
  private List<PlsqlCode> objectTypeSpecPlsql = new ArrayList<>();
  private List<PlsqlCode> objectTypeBodyPlsql = new ArrayList<>();
  private List<PlsqlCode> packageSpecPlsql = new ArrayList<>();
  private List<PlsqlCode> packageBodyPlsql = new ArrayList<>();
  private List<PlsqlCode> standaloneFunctionPlsql = new ArrayList<>();
  private List<PlsqlCode> standaloneProcedurePlsql = new ArrayList<>();
  private List<PlsqlCode> triggerPlsql = new ArrayList<>();

  // parsed data
  // TODO table default Expression!!!
  private List<ViewSpecAndQuery> viewSpecAndQueries = new ArrayList<>();
  private List<ObjectType> objectTypeSpecAst = new ArrayList<>();
  private List<ObjectType> objectTypeBodyAst = new ArrayList<>();
  private List<OraclePackage> packageSpecAst = new ArrayList<>();
  private List<OraclePackage> packageBodyAst = new ArrayList<>();
  private List<Function> standaloneFunctionAst = new ArrayList<>();
  private List<Procedure> standaloneProcedureAst = new ArrayList<>();
  private List<Trigger> triggerAst = new ArrayList<>();

  private long totalRowCount = 0;

  public List<String> getUserNames() {
    return userNames;
  }

  public List<TableMetadata> getTableSql() {
    return tableSql;
  }

  public List<ViewMetadata> getViewDefinition() {
    return viewDefinition;
  }

  public List<SynonymMetadata> getSynonyms() { return synonyms; }

  public List<IndexMetadata> getIndexes() { return indexes; }

  public List<PlsqlCode> getObjectTypeSpecPlsql() {
    return objectTypeSpecPlsql;
  }

  public List<PlsqlCode> getObjectTypeBodyPlsql() {
    return objectTypeBodyPlsql;
  }

  public List<PlsqlCode> getPackageSpecPlsql() {
    return packageSpecPlsql;
  }

  public List<PlsqlCode> getPackageBodyPlsql() {
    return packageBodyPlsql;
  }

  public List<PlsqlCode> getStandaloneFunctionPlsql() {
    return standaloneFunctionPlsql;
  }

  public List<PlsqlCode> getStandaloneProcedurePlsql() {
    return standaloneProcedurePlsql;
  }

  public List<PlsqlCode> getTriggerPlsql() {
    return triggerPlsql;
  }

  public List<ViewSpecAndQuery> getViewSpecAndQueries() {
    return viewSpecAndQueries;
  }

  public List<ObjectType> getObjectTypeSpecAst() {
    return objectTypeSpecAst;
  }

  public List<ObjectType> getObjectTypeBodyAst() {
    return objectTypeBodyAst;
  }

  public List<OraclePackage> getPackageSpecAst() {
    return packageSpecAst;
  }

  public List<OraclePackage> getPackageBodyAst() {
    return packageBodyAst;
  }

  public List<Function> getStandaloneFunctionAst() {
    return standaloneFunctionAst;
  }

  public List<Procedure> getStandaloneProcedureAst() {
    return standaloneProcedureAst;
  }

  public List<Trigger> getTriggerAst() {
    return triggerAst;
  }

  public long getTotalRowCount() {
    return totalRowCount;
  }

  public void setTotalRowCount(long totalRowCount) {
    this.totalRowCount = totalRowCount;
  }

  // Statistics methods for standalone functions and procedures
  public int getStandaloneFunctionCount() {
    return standaloneFunctionAst.size();
  }

  public int getStandaloneProcedureCount() {
    return standaloneProcedureAst.size();
  }

  public int getStandaloneFunctionPlsqlCount() {
    return standaloneFunctionPlsql.size();
  }

  public int getStandaloneProcedurePlsqlCount() {
    return standaloneProcedurePlsql.size();
  }

  public void findDefaultExpression(String schemaWhereWeAreNow, String myTableName, String columnName) {
    //TODO
  }

  
  /**
   * Gets all functions from all parsed packages and standalone functions.
   * This enables comprehensive search for function-local collection types.
   * @return List of all parsed functions
   */
  public List<Function> getAllFunctions() {
    List<Function> allFunctions = new ArrayList<>();
    
    // Add functions from package specs
    for (OraclePackage pkg : packageSpecAst) {
      if (pkg.getFunctions() != null) {
        allFunctions.addAll(pkg.getFunctions());
      }
    }
    
    // Add functions from package bodies
    for (OraclePackage pkg : packageBodyAst) {
      if (pkg.getFunctions() != null) {
        allFunctions.addAll(pkg.getFunctions());
      }
    }
    
    // Add standalone functions
    for (Function func : standaloneFunctionAst) {
      allFunctions.add(func);
    }
    
    return allFunctions;
  }
}