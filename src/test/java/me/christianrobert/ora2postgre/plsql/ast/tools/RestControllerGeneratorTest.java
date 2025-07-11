package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.RestControllerGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestControllerGeneratorTest {

  @Test
  public void testGenerateSimpleController() {
    // Create a mock Everything context
    Everything data = new Everything();

    // Create a simple function with parameters
    DataTypeSpec varcharType = new DataTypeSpec("VARCHAR2", null, null, null);
    DataTypeSpec numberType = new DataTypeSpec("NUMBER", null, null, null);

    Parameter param1 = new Parameter("userId", varcharType, null, true, false);
    Parameter param2 = new Parameter("maxResults", numberType, null, true, false);
    List<Parameter> parameters = Arrays.asList(param1, param2);

    Function testFunction = new Function("getUserData", parameters,new ArrayList<>(), "varchar2", new ArrayList<>());

    // Create a simple procedure
    Procedure testProcedure = new Procedure("updateUser", parameters,new ArrayList<>(), new ArrayList<>());

    // Create a package
    OraclePackage testPackage = new OraclePackage(
            "USER_PKG",
            "TEST_SCHEMA",
            new ArrayList<>(), // variables
            new ArrayList<>(), // subtypes  
            new ArrayList<>(), // cursors
            new ArrayList<>(), // types
            Arrays.asList(testFunction), // functions
            Arrays.asList(testProcedure), // procedures
            new ArrayList<>()  // body statements
    );

    // Set parent package for function and procedure
    testFunction.setParentPackage(testPackage);
    testProcedure.setParentPackage(testPackage);

    // Generate controller
    String controller = RestControllerGenerator.generateController(testPackage, "com.example", data);

    // Basic assertions
    assert controller.contains("@ApplicationScoped");
    assert controller.contains("@Path(\"/test_schema/user_pkg\")");
    assert controller.contains("getUserData");
    assert controller.contains("updateUser");
    assert controller.contains("@QueryParam(\"userId\")");
    assert controller.contains("@QueryParam(\"maxResults\")");
  }
}