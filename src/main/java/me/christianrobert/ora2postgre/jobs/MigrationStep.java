package me.christianrobert.ora2postgre.jobs;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the migration workflow steps with their duration weights and sub-steps.
 * Duration weights are used to calculate overall progress percentage across the full migration.
 */
public enum MigrationStep {

  EXTRACT("Extract Database Metadata", 0.15, Arrays.asList(
          "Extract user schemas",
          "Extract table metadata",
          "Extract view definitions",
          "Extract synonyms",
          "Extract object type specs",
          "Extract object type bodies",
          "Extract package specs",
          "Extract package bodies",
          "Calculate total row counts"
  )),

  PARSE("Parse PL/SQL to AST", 0.10, Arrays.asList(
          "Parse view DDL into AST",
          "Parse object type specs into AST",
          "Parse object type bodies into AST",
          "Parse package specs into AST",
          "Parse package bodies into AST"
  )),

  EXPORT("Generate PostgreSQL Files", 0.15, Arrays.asList(
          "Setup target project infrastructure",
          "Generate REST controllers",
          "Export PostgreSQL project structure",
          "Export schema DDL",
          "Export table DDL",
          "Export view structures",
          "Export object type specs",
          "Export package specs",
          "Export view DDL",
          "Export object type bodies",
          "Export package bodies"
  )),

  EXECUTE_PRE("Execute Pre-Transfer SQL", 0.05, Arrays.asList(
          "Execute type specs",
          "Execute schema and table DDL"
  )),

  TRANSFERDATA("Transfer Data", 0.50, Arrays.asList(
          "Analyze tables with object types",
          "Select transfer strategies",
          "Execute data transfer",
          "Handle fallback strategies",
          "Generate transfer results"
  )),

  EXECUTE_POST("Execute Post-Transfer SQL", 0.05, Arrays.asList(
          "Execute constraints and final objects"
  ));

  private final String displayName;
  private final double durationWeight; // Relative weight for overall progress calculation
  private final List<String> subSteps;

  MigrationStep(String displayName, double durationWeight, List<String> subSteps) {
    this.displayName = displayName;
    this.durationWeight = durationWeight;
    this.subSteps = subSteps;
  }

  public String getDisplayName() {
    return displayName;
  }

  public double getDurationWeight() {
    return durationWeight;
  }

  public List<String> getSubSteps() {
    return subSteps;
  }

  public int getSubStepCount() {
    return subSteps.size();
  }

  /**
   * Get the step number (1-based) for display purposes
   */
  public int getStepNumber() {
    return ordinal() + 1;
  }

  /**
   * Get the total number of migration steps
   */
  public static int getTotalSteps() {
    return values().length;
  }

  /**
   * Calculate the starting percentage for this step based on previous steps' weights
   */
  public double getStartingPercentage() {
    double total = 0.0;
    for (MigrationStep step : values()) {
      if (step.ordinal() < this.ordinal()) {
        total += step.durationWeight;
      } else {
        break;
      }
    }
    return total;
  }

  /**
   * Calculate the ending percentage for this step
   */
  public double getEndingPercentage() {
    return getStartingPercentage() + durationWeight;
  }

  /**
   * Calculate overall progress percentage given step and sub-step progress
   * @param subStepProgress Progress within current step (0.0 to 1.0)
   * @return Overall progress percentage (0.0 to 1.0)
   */
  public double calculateOverallProgress(double subStepProgress) {
    return getStartingPercentage() + (durationWeight * Math.max(0.0, Math.min(1.0, subStepProgress)));
  }

  /**
   * Get step from step number (1-based)
   */
  public static MigrationStep fromStepNumber(int stepNumber) {
    if (stepNumber < 1 || stepNumber > values().length) {
      throw new IllegalArgumentException("Invalid step number: " + stepNumber);
    }
    return values()[stepNumber - 1];
  }
}