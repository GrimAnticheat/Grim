package ac.grim.grimac.manager.config.update;

import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UltimateUpdater {

    private static final int LATEST_CONFIG_VERSION = 10;

    public void updateConfig() throws IOException {
        // --- Setup (Use your plugin's methods) ---
        File dataFolder = new File("/home/user/Desktop/Server/Network/fabric-1.21.7/config/GrimAC"); // In a real plugin, use getDataFolder()
        dataFolder.mkdirs();

        File configFile = new File(dataFolder, "config.yml");
        File backupFile = new File(dataFolder, "config.yml.bak");
        // In plugin, you'd save the default config from resources if it doesn't exist.
        // For updates, we assume a new default config is available.
        File newDefaultConfigFile = new File("/home/user/Desktop/Server/Network/fabric-1.21.7/config/GrimAC/new-config.yml"); // Path to your default template

        // Step 0: Check Version, etc. (your existing logic)
        // ...

        // Step 1: Backup
        if (configFile.exists()) {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Step 2: Copy new default config over
        try (InputStream defaultConfigStream = new FileInputStream(newDefaultConfigFile)) { // In plugin: getResource("config.yml")
            Files.copy(defaultConfigStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        if (!backupFile.exists()) {
            System.out.println("Fresh install, no update required.");
            return;
        }

        // --- THE HYBRID LOGIC ---

        // 1. ANALYSIS: Use SnakeYAML to find all required changes.
        Yaml yaml = new Yaml();
        Map<String, Object> oldConfigData = yaml.load(new FileInputStream(backupFile));
        Map<String, Object> newConfigData = yaml.load(new FileInputStream(configFile));

        Map<String, Object> changesToApply = new HashMap<>();
        findChanges("", oldConfigData, newConfigData, changesToApply);

        // Your custom migration hook can still work here by modifying the changes map
        // Example:
        // migrateReach(oldConfigData, changesToApply);

        // 2. SURGERY: Apply changes to the file.
        if (!changesToApply.isEmpty()) {
            ConfigPatcher patcher = new ConfigPatcher(configFile);
            for (Map.Entry<String, Object> entry : changesToApply.entrySet()) {
                patcher.setValue(entry.getKey(), entry.getValue());
            }
            patcher.save();
        }

        System.out.println("Update complete. Formatting and all values have been preserved.");
    }

    private void findChanges(String path, Map<String, Object> oldData, Map<String, Object> newData, Map<String, Object> changesToMake) {
        for (String key : oldData.keySet()) {
            if (!newData.containsKey(key)) continue;

            String currentPath = path.isEmpty() ? key : path + "." + key;
            Object oldValue = oldData.get(key);
            Object newValue = newData.get(key);

            if (oldValue instanceof Map && newValue instanceof Map) {
                findChanges(currentPath, (Map<String, Object>) oldValue, (Map<String, Object>) newValue, changesToMake);
            } else if (!Objects.equals(oldValue, newValue)) {
                // User's value is different, we need to preserve it.
                changesToMake.put(currentPath, oldValue);
            }
        }
        // We do not want to change the config-version back to the old value
        changesToMake.remove("config-version");
    }

    // A dummy main method to make this runnable for testing purposes.
    // In your plugin, you would call `new ConfigUpdater().updateConfig();`
    public static void main(String[] args) throws IOException {
        // You would need to create dummy files here to test this standalone.
        // For example, create a "path/to/your/plugin/data-folder" directory,
        // put a dummy config.yml in it, and have a default config.yml in your resources.
        new UltimateUpdater().updateConfig();
        System.out.println("To test, create the necessary file structure and run the updateConfig() method.");
    }
}
