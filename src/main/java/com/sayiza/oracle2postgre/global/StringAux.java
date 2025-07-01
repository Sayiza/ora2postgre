package com.sayiza.oracle2postgre.global;

public class StringAux {
  public static String capitalizeFirst(String input) {
    if (input == null || input.isEmpty()) return input;
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }

  public static String lowerCaseFirst(String input) {
    if (input == null || input.isEmpty()) return input;
    return input.substring(0, 1).toLowerCase() + input.substring(1);
  }
}
