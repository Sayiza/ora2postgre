package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation for transforming Oracle packages to PostgreSQL.
 * This strategy handles most common Oracle package patterns and converts them
 * to PostgreSQL-compatible CREATE FUNCTION/PROCEDURE statements.
 */
public class StandardPackageStrategy implements PackageTransformationStrategy {

  private static final Logger log = LoggerFactory.getLogger(StandardPackageStrategy.class);

  @Override
  public boolean supports(OraclePackage oraclePackage) {
    // Standard strategy supports all packages as a fallback
    return true;
  }

  @Override
  public String transform(OraclePackage oraclePackage, Everything context, boolean specOnly) {
    log.debug("Transforming package {}.{} using StandardPackageStrategy (specOnly={})",
            oraclePackage.getSchema(), oraclePackage.getName(), specOnly);

    StringBuilder b = new StringBuilder();
    
    // Transform package variables - currently TODO in original code
    for (Variable variable : oraclePackage.getVariables()) {
      // TODO: Package variables might need to become PostgreSQL global variables or constants
      // This is currently a TODO in the original OraclePackage.toPostgre() method
    }
    
    // Transform package subtypes - currently TODO in original code
    for (SubType subtype : oraclePackage.getSubtypes()) {
      // TODO: Oracle subtypes might need to become PostgreSQL domains or composite types
      // This is currently a TODO in the original OraclePackage.toPostgre() method
    }
    
    // Transform package cursors - currently TODO in original code  
    for (Cursor cursor : oraclePackage.getCursors()) {
      // TODO: Package cursors might need special handling in PostgreSQL
      // This is currently a TODO in the original OraclePackage.toPostgre() method
    }
    
    // Transform package types - currently TODO in original code
    for (PackageType type : oraclePackage.getTypes()) {
      // TODO: Package types might need to become PostgreSQL composite types
      // This is currently a TODO in the original OraclePackage.toPostgre() method
    }
    
    // Transform functions
    for (Function function : oraclePackage.getFunctions()) {
      b.append(function.toPostgre(context, specOnly));
      b.append("\n\n");
    }
    
    // Transform procedures
    for (Procedure procedure : oraclePackage.getProcedures()) {
      b.append(procedure.toPostgre(context, specOnly));
      b.append("\n\n");
    }
    
    // TODO: Body statements need to be passed to functions and procedures
    // This is currently a TODO in the original OraclePackage.toPostgre() method
    
    log.debug("Successfully transformed package {}.{} with {} functions and {} procedures",
            oraclePackage.getSchema(), oraclePackage.getName(), 
            oraclePackage.getFunctions().size(), oraclePackage.getProcedures().size());

    return b.toString();
  }

  @Override
  public String getStrategyName() {
    return "Standard Package";
  }

  @Override
  public int getPriority() {
    // Standard strategy has lowest priority as it's the fallback
    return 0;
  }

  @Override
  public String getConversionNotes(OraclePackage oraclePackage) {
    StringBuilder notes = new StringBuilder("Converted using standard package transformation");
    
    // Add notes about incomplete transformations
    if (!oraclePackage.getVariables().isEmpty()) {
      notes.append("; Package variables not yet implemented");
    }
    if (!oraclePackage.getSubtypes().isEmpty()) {
      notes.append("; Package subtypes not yet implemented");  
    }
    if (!oraclePackage.getCursors().isEmpty()) {
      notes.append("; Package cursors not yet implemented");
    }
    if (!oraclePackage.getTypes().isEmpty()) {
      notes.append("; Package types not yet implemented");
    }
    if (!oraclePackage.getBodyStatements().isEmpty()) {
      notes.append("; Body statements not yet passed to functions/procedures");
    }
    
    return notes.toString();
  }
}