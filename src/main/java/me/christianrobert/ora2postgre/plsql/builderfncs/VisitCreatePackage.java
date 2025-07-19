package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.PackageType;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.RecordType;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import java.util.ArrayList;
import java.util.List;

public class VisitCreatePackage {
  public static PlSqlAst visit(
          PlSqlParser.Create_packageContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    String packageName = "UNKNOWN";
    if (ctx.package_name() != null && !ctx.package_name().isEmpty()) {
      for (PlSqlParser.Package_nameContext p : ctx.package_name()) {
        packageName = p.getText();
      }
    }

    List<Variable> variables = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    List<PackageType> packageTypes = new ArrayList<>();

    if (ctx.package_obj_spec() != null) {
      for (var member : ctx.package_obj_spec()) {
        PlSqlAst memberAst = astBuilder.visit(member);
        if (memberAst instanceof Variable) {
          variables.add((Variable) memberAst);
        } else if (memberAst instanceof RecordType) {
          recordTypes.add((RecordType) memberAst);
        } else if (memberAst instanceof VarrayType) {
          varrayTypes.add((VarrayType) memberAst);
        } else if (memberAst instanceof NestedTableType) {
          nestedTableTypes.add((NestedTableType) memberAst);
        } else if (memberAst instanceof PackageType) {
          packageTypes.add((PackageType) memberAst);
        }
      }
    }
    // TODO $if, subtype, cursor, etc.
    return new OraclePackage(packageName, astBuilder.schema, variables, null, null, packageTypes, recordTypes, varrayTypes, nestedTableTypes, null, null, null);
  }
}