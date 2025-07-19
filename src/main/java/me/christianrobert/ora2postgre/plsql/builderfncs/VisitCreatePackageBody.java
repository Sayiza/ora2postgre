package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.RecordType;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import java.util.ArrayList;
import java.util.List;

public class VisitCreatePackageBody {
  public static PlSqlAst visit(
          PlSqlParser.Create_package_bodyContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    String packageName = "UNKNOWN";
    if (ctx.package_name() != null && !ctx.package_name().isEmpty()) {
      for (PlSqlParser.Package_nameContext p : ctx.package_name()) {
        packageName = p.getText();
      }
    }
    
    // Store current package context for call resolution
    astBuilder.setCurrentPackageName(packageName);
    List<Procedure> procedures = new ArrayList<>();
    List<Function> funcs = new ArrayList<>();
    List<Variable> variables = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    if (ctx.package_obj_body() != null) {
      for (var member : ctx.package_obj_body()) {
        PlSqlAst memberAst = astBuilder.visit(member);
        if (memberAst instanceof Variable) {
          variables.add((Variable) memberAst);
        } else if (memberAst instanceof RecordType) {
          recordTypes.add((RecordType) memberAst);
        } else if (memberAst instanceof VarrayType) {
          varrayTypes.add((VarrayType) memberAst);
        } else if (memberAst instanceof NestedTableType) {
          nestedTableTypes.add((NestedTableType) memberAst);
        } else if (memberAst instanceof Function) {
          funcs.add((Function) memberAst);
        } else if (memberAst instanceof Procedure) {
          procedures.add((Procedure) memberAst);
        }
      }
    }
    OraclePackage o = new OraclePackage(packageName, astBuilder.schema, variables, null, null, null, recordTypes, varrayTypes, nestedTableTypes, funcs, procedures, null);
    o.getFunctions().forEach(e -> e.setParentPackage(o));
    o.getProcedures().forEach(e -> e.setParentPackage(o));
    return o;
  }
}