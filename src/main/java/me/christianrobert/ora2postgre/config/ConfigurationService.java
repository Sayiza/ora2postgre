package me.christianrobert.ora2postgre.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.christianrobert.ora2postgre.global.Config;
import java.util.Map;
import java.util.HashMap;

@ApplicationScoped
public class ConfigurationService {

    @Inject
    Config config;

    private final Map<String, Object> runtimeSettings = new HashMap<>();

    public RuntimeConfiguration getCurrentConfiguration() {
        RuntimeConfiguration runtimeConfig = new RuntimeConfiguration();
        
        // Process flags
        runtimeConfig.setDoAllSchemas(getValue("do.all-schemas", config.isDoAllSchemas()));
        runtimeConfig.setDoOnlyTestSchema(getValue("do.only-test-schema", String.join(",", config.getDoOnlyTestSchema())));
        runtimeConfig.setDoTable(getValue("do.table", config.isDoTable()));
        runtimeConfig.setDoSynonyms(getValue("do.synonyms", config.isDoSynonyms()));
        runtimeConfig.setDoData(getValue("do.data", config.isDoData()));
        runtimeConfig.setDoObjectTypeSpec(getValue("do.object-type-spec", config.isDoObjectTypeSpec()));
        runtimeConfig.setDoObjectTypeBody(getValue("do.object-type-body", config.isDoObjectTypeBody()));
        runtimeConfig.setDoPackageSpec(getValue("do.package-spec", config.isDoPackageSpec()));
        runtimeConfig.setDoPackageBody(getValue("do.package-body", config.isDoPackageBody()));
        runtimeConfig.setDoViewSignature(getValue("do.view-signature", config.isDoViewSignature()));
        runtimeConfig.setDoViewDdl(getValue("do.view-ddl", config.isDoViewDdl()));
        runtimeConfig.setDoTriggers(getValue("do.triggers", config.isDoTriggers()));
        runtimeConfig.setDoWriteRestControllers(getValue("do.write-rest-controllers", config.isDoWriteRestControllers()));
        runtimeConfig.setDoWritePostgreFiles(getValue("do.write-postgre-files", config.isDoWritePostgreFiles()));
        runtimeConfig.setDoExecutePostgreFiles(getValue("do.execute-postgre-files", config.isDoExecutePostgreFiles()));
        runtimeConfig.setDoRestControllerFunctions(getValue("do.rest-controller-functions", config.isDoRestControllerFunctions()));
        runtimeConfig.setDoRestControllerProcedures(getValue("do.rest-controller-procedures", config.isDoRestControllerProcedures()));
        
        // Connection settings
        runtimeConfig.setOracleUrl(getValue("oracle.url", config.getOracleUrl()));
        runtimeConfig.setOracleUser(getValue("oracle.user", config.getOracleUser()));
        runtimeConfig.setOraclePassword(getValue("oracle.password", config.getOraclePassword()));
        runtimeConfig.setPostgreUrl(getValue("postgre.url", config.getPostgreUrl()));
        runtimeConfig.setPostgreUsername(getValue("postgre.username", config.getPostgreUsername()));
        runtimeConfig.setPostgrePassword(getValue("postgre.password", config.getPostgrePassword()));
        
        // Path settings
        runtimeConfig.setJavaGeneratedPackageName(getValue("java.generated-package-name", config.getGeneratedJavaPackageName()));
        runtimeConfig.setPathTargetProjectRoot(getValue("path.target-project-root", config.getPathToTargetProjectRoot()));
        runtimeConfig.setPathTargetProjectJava(getValue("path.target-project-java", "/src/main/java"));
        runtimeConfig.setPathTargetProjectResources(getValue("path.target-project-resources", "/src/main/resources"));
        runtimeConfig.setPathTargetProjectPostgre(getValue("path.target-project-postgre", "/postgre/autoddl"));
        
        return runtimeConfig;
    }

    public void updateConfiguration(RuntimeConfiguration updates) {
        if (updates.getDoAllSchemas() != null) {
            runtimeSettings.put("do.all-schemas", updates.getDoAllSchemas());
        }
        if (updates.getDoOnlyTestSchema() != null) {
            runtimeSettings.put("do.only-test-schema", updates.getDoOnlyTestSchema());
        }
        if (updates.getDoTable() != null) {
            runtimeSettings.put("do.table", updates.getDoTable());
        }
        if (updates.getDoSynonyms() != null) {
            runtimeSettings.put("do.synonyms", updates.getDoSynonyms());
        }
        if (updates.getDoData() != null) {
            runtimeSettings.put("do.data", updates.getDoData());
        }
        if (updates.getDoObjectTypeSpec() != null) {
            runtimeSettings.put("do.object-type-spec", updates.getDoObjectTypeSpec());
        }
        if (updates.getDoObjectTypeBody() != null) {
            runtimeSettings.put("do.object-type-body", updates.getDoObjectTypeBody());
        }
        if (updates.getDoPackageSpec() != null) {
            runtimeSettings.put("do.package-spec", updates.getDoPackageSpec());
        }
        if (updates.getDoPackageBody() != null) {
            runtimeSettings.put("do.package-body", updates.getDoPackageBody());
        }
        if (updates.getDoViewSignature() != null) {
            runtimeSettings.put("do.view-signature", updates.getDoViewSignature());
        }
        if (updates.getDoViewDdl() != null) {
            runtimeSettings.put("do.view-ddl", updates.getDoViewDdl());
        }
        if (updates.getDoTriggers() != null) {
            runtimeSettings.put("do.triggers", updates.getDoTriggers());
        }
        if (updates.getDoWriteRestControllers() != null) {
            runtimeSettings.put("do.write-rest-controllers", updates.getDoWriteRestControllers());
        }
        if (updates.getDoWritePostgreFiles() != null) {
            runtimeSettings.put("do.write-postgre-files", updates.getDoWritePostgreFiles());
        }
        if (updates.getDoExecutePostgreFiles() != null) {
            runtimeSettings.put("do.execute-postgre-files", updates.getDoExecutePostgreFiles());
        }
        if (updates.getDoRestControllerFunctions() != null) {
            runtimeSettings.put("do.rest-controller-functions", updates.getDoRestControllerFunctions());
        }
        if (updates.getDoRestControllerProcedures() != null) {
            runtimeSettings.put("do.rest-controller-procedures", updates.getDoRestControllerProcedures());
        }
        
        // Connection settings
        if (updates.getOracleUrl() != null) {
            runtimeSettings.put("oracle.url", updates.getOracleUrl());
        }
        if (updates.getOracleUser() != null) {
            runtimeSettings.put("oracle.user", updates.getOracleUser());
        }
        if (updates.getOraclePassword() != null) {
            runtimeSettings.put("oracle.password", updates.getOraclePassword());
        }
        if (updates.getPostgreUrl() != null) {
            runtimeSettings.put("postgre.url", updates.getPostgreUrl());
        }
        if (updates.getPostgreUsername() != null) {
            runtimeSettings.put("postgre.username", updates.getPostgreUsername());
        }
        if (updates.getPostgrePassword() != null) {
            runtimeSettings.put("postgre.password", updates.getPostgrePassword());
        }
        
        // Path settings
        if (updates.getJavaGeneratedPackageName() != null) {
            runtimeSettings.put("java.generated-package-name", updates.getJavaGeneratedPackageName());
        }
        if (updates.getPathTargetProjectRoot() != null) {
            runtimeSettings.put("path.target-project-root", updates.getPathTargetProjectRoot());
        }
        if (updates.getPathTargetProjectJava() != null) {
            runtimeSettings.put("path.target-project-java", updates.getPathTargetProjectJava());
        }
        if (updates.getPathTargetProjectResources() != null) {
            runtimeSettings.put("path.target-project-resources", updates.getPathTargetProjectResources());
        }
        if (updates.getPathTargetProjectPostgre() != null) {
            runtimeSettings.put("path.target-project-postgre", updates.getPathTargetProjectPostgre());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(String key, T defaultValue) {
        Object value = runtimeSettings.get(key);
        return value != null ? (T) value : defaultValue;
    }

    public boolean isDoAllSchemas() {
        return getValue("do.all-schemas", config.isDoAllSchemas());
    }

    public String getDoOnlyTestSchema() {
        return getValue("do.only-test-schema", String.join(",", config.getDoOnlyTestSchema()));
    }

    public boolean isDoTable() {
        return getValue("do.table", config.isDoTable());
    }

    public boolean isDoSynonyms() {
        return getValue("do.synonyms", config.isDoSynonyms());
    }

    public boolean isDoData() {
        return getValue("do.data", config.isDoData());
    }

    public boolean isDoObjectTypeSpec() {
        return getValue("do.object-type-spec", config.isDoObjectTypeSpec());
    }

    public boolean isDoObjectTypeBody() {
        return getValue("do.object-type-body", config.isDoObjectTypeBody());
    }

    public boolean isDoPackageSpec() {
        return getValue("do.package-spec", config.isDoPackageSpec());
    }

    public boolean isDoPackageBody() {
        return getValue("do.package-body", config.isDoPackageBody());
    }

    public boolean isDoViewSignature() {
        return getValue("do.view-signature", config.isDoViewSignature());
    }

    public boolean isDoViewDdl() {
        return getValue("do.view-ddl", config.isDoViewDdl());
    }

    public boolean isDoTriggers() {
        return getValue("do.triggers", config.isDoTriggers());
    }

    public boolean isDoWriteRestControllers() {
        return getValue("do.write-rest-controllers", config.isDoWriteRestControllers());
    }

    public boolean isDoWritePostgreFiles() {
        return getValue("do.write-postgre-files", config.isDoWritePostgreFiles());
    }

    public boolean isDoWriteTriggerFiles() {
        return getValue("do.write-trigger-files", config.isDoWriteTriggerFiles());
    }

    public boolean isDoExecutePostgreFiles() {
        return getValue("do.execute-postgre-files", config.isDoExecutePostgreFiles());
    }

    public boolean isDoRestControllerFunctions() {
        return getValue("do.rest-controller-functions", config.isDoRestControllerFunctions());
    }

    public boolean isDoRestControllerProcedures() {
        return getValue("do.rest-controller-procedures", config.isDoRestControllerProcedures());
    }

    public String getOracleUrl() {
        return getValue("oracle.url", config.getOracleUrl());
    }

    public String getOracleUser() {
        return getValue("oracle.user", config.getOracleUser());
    }

    public String getOraclePassword() {
        return getValue("oracle.password", config.getOraclePassword());
    }

    public String getPostgreUrl() {
        return getValue("postgre.url", config.getPostgreUrl());
    }

    public String getPostgreUsername() {
        return getValue("postgre.username", config.getPostgreUsername());
    }

    public String getPostgrePassword() {
        return getValue("postgre.password", config.getPostgrePassword());
    }

    public String getJavaGeneratedPackageName() {
        return getValue("java.generated-package-name", config.getGeneratedJavaPackageName());
    }

    public String getPathTargetProjectRoot() {
        return getValue("path.target-project-root", config.getPathToTargetProjectRoot());
    }

    public String getPathTargetProjectJava() {
        return getValue("path.target-project-java", "/src/main/java");
    }

    public String getPathTargetProjectResources() {
        return getValue("path.target-project-resources", "/src/main/resources");
    }

    public String getPathTargetProjectPostgre() {
        return getValue("path.target-project-postgre", "/postgre/autoddl");
    }
}