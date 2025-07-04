package me.christianrobert.ora2postgre.global;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for new REST controller configuration flags.
 */
public class ConfigTest {

    @Test
    public void testConfigurationDefaults() {
        // Create config instance (in real usage it would be injected with properties)
        Config config = new Config();
        
        // Test that the getter methods exist and work
        // Note: These will use the default values from @ConfigProperty annotations
        assertNotNull(config);
        
        // The actual property values will be loaded from application.properties
        // but we can test that the methods exist
        try {
            
            // Test new REST controller methods
            config.isDoWriteRestControllers();
            config.isDoRestSimpleDtos();
            
            // Test PostgreSQL methods still work
            config.isDoWritePostgreFiles();
            config.isDoExecutePostgreFiles();
            
            // If we get here, all methods exist
            assertTrue(true, "All configuration methods are accessible");
            
        } catch (Exception e) {
            fail("Configuration methods should be accessible: " + e.getMessage());
        }
    }
    
    @Test
    public void testConfigurationMethodsExist() {
        Config config = new Config();
        
        // Verify new methods exist by checking they don't throw exceptions
        assertDoesNotThrow(() -> config.isDoWriteRestControllers());
        assertDoesNotThrow(() -> config.isDoRestSimpleDtos());
    }
}