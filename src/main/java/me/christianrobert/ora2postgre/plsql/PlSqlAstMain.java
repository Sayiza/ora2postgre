package me.christianrobert.ora2postgre.plsql;

import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.antlr.PlSqlLexer;
import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlSqlAstMain {

  private static final Logger log = LoggerFactory.getLogger(PlSqlAstMain.class);

  public static PlSqlAst processPlsqlCode(PlsqlCode plSqlCode) {
    ParseTree tree = parsePlSql(plSqlCode.code);
    PlSqlAstBuilder astBuilder = new PlSqlAstBuilder(plSqlCode.schema);
    PlSqlAst visited = astBuilder.visit(tree);
    if (visited == null) {
      throw new RuntimeException("Failed to parse plsql code: " + plSqlCode.code);
    }
    log.debug("AST: {}", visited.toString().substring(0, Math.min(200, visited.toString().length())));

    return visited;
  }

  private static ParseTree parsePlSql(String code) {
    CharStream input = CharStreams.fromString(code);
    PlSqlLexer lexer = new PlSqlLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    PlSqlParser parser = new PlSqlParser(tokens);
    return parser.sql_script();
  }

  /**
   * Builds a standalone function AST from PL/SQL code.
   * Sets the standalone flag and schema for the function.
   */
  public static Function buildStandaloneFunctionAst(PlsqlCode plsqlCode) throws Exception {
    PlSqlAst ast = processPlsqlCode(plsqlCode);
    
    if (ast instanceof Function) {
      Function function = (Function) ast;
      function.setStandalone(true);
      function.setSchema(plsqlCode.schema);
      return function;
    } else {
      throw new Exception("Expected Function AST but got: " + 
                         (ast != null ? ast.getClass().getSimpleName() : "null"));
    }
  }

  /**
   * Builds a standalone procedure AST from PL/SQL code.
   * Sets the standalone flag and schema for the procedure.
   */
  public static Procedure buildStandaloneProcedureAst(PlsqlCode plsqlCode) throws Exception {
    PlSqlAst ast = processPlsqlCode(plsqlCode);
    
    if (ast instanceof Procedure) {
      Procedure procedure = (Procedure) ast;
      procedure.setStandalone(true);
      procedure.setSchema(plsqlCode.schema);
      return procedure;
    } else {
      throw new Exception("Expected Procedure AST but got: " + 
                         (ast != null ? ast.getClass().getSimpleName() : "null"));
    }
  }
}
