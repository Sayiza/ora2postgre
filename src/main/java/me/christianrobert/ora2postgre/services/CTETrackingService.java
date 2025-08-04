package me.christianrobert.ora2postgre.services;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.HashSet;

/**
 * Service for tracking Common Table Expression (CTE) scopes during SQL transformation.
 * Manages the current CTE scope to prevent CTE names from being resolved as regular
 * table names during query processing.
 */
@ApplicationScoped
public class CTETrackingService {

  private Set<String> activeCTENames = new HashSet<>();
  
  // Static instance for testing when CDI is not available
  private static CTETrackingService testInstance = null;
  
  /**
   * Sets a test instance to be used when CDI injection is not available.
   * This is primarily for testing scenarios.
   */
  public static void setTestInstance(CTETrackingService instance) {
    testInstance = instance;
  }
  
  /**
   * Gets the test instance if available, otherwise returns null.
   * This is used as a fallback when CDI injection fails.
   */
  public static CTETrackingService getTestInstance() {
    return testInstance;
  }

  /**
   * Adds a CTE name to the current scope.
   * @param cteName The name of the CTE to add to the active scope
   */
  public void addActiveCTE(String cteName) {
    if (cteName != null && !cteName.trim().isEmpty()) {
      activeCTENames.add(cteName.toUpperCase());
    }
  }

  /**
   * Removes a CTE name from the current scope.
   * @param cteName The name of the CTE to remove from the active scope
   */
  public void removeActiveCTE(String cteName) {
    if (cteName != null && !cteName.trim().isEmpty()) {
      activeCTENames.remove(cteName.toUpperCase());
    }
  }

  /**
   * Checks if a table name is actually a CTE name in the current scope.
   * @param tableName The table name to check
   * @return true if the name is an active CTE, false otherwise
   */
  public boolean isActiveCTE(String tableName) {
    if (tableName == null || tableName.trim().isEmpty()) {
      return false;
    }
    return activeCTENames.contains(tableName.toUpperCase());
  }

  /**
   * Clears all active CTE names from the current scope.
   * This should be called when exiting a query context that had CTEs.
   */
  public void clearActiveCTEs() {
    activeCTENames.clear();
  }

  /**
   * Gets a copy of the current active CTE names for debugging purposes.
   * @return A copy of the set of active CTE names
   */
  public Set<String> getActiveCTENames() {
    return new HashSet<>(activeCTENames);
  }
}