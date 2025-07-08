package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class HtpStatement extends Statement {

  private String what;

  public HtpStatement(String what) {
    this.what = what;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Htp{" + what + "}";
  }

  // toJava() method removed - HTP calls stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(data.getIntendation())
            .append("CALL SYS.HTP_p(")
            .append(what)
            .append(");\n");
    return b.toString();
  }

}