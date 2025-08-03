package ac.grim.grimac.manager.config.update;// File: ConfigUpdater.java
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.function.BiConsumer;

public class ConfigUpdater {

    private static final int LATEST_CONFIG_VERSION = 10;

    public void updateConfig() throws IOException {
        // Assume this code runs in your onEnable() or a command context
        File dataFolder = new File("/home/user/Desktop/Server/Network/fabric-1.21.7/config/GrimAC"); // In a real plugin, use getDataFolder()
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, "config.yml");
        File backupFile = new File(dataFolder, "config.yml.bak");

        // Step 0: Check if an update is needed.
        if (configFile.exists()) {
            Yaml snakeYaml = new Yaml();
            Map<String, Object> currentConfigData = snakeYaml.load(new FileInputStream(configFile));
            int currentVersion = (int) currentConfigData.getOrDefault("config-version", 0);

            if (currentVersion >= LATEST_CONFIG_VERSION) {
                System.out.println("Config is up to date. No update needed.");
                return;
            }
            System.out.println("Old config version detected (" + currentVersion + "). Starting update process...");
        }

        // Step 1: Backup old config if it exists
        if (configFile.exists()) {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backed up 'config.yml' to 'config.yml.bak'");
        }

        // Step 2: Copy over new config from JAR resources
        // In a real plugin, you get the resource from the JAR.
        try (InputStream defaultConfigStream = getClass().getResourceAsStream("/config.yml")) {
            InputStream stream;
            if (defaultConfigStream == null) {
                stream = Files.newInputStream(Paths.get("/home/user/Desktop/Server/Network/fabric-1.21.7/config/GrimAC/new-config.yml"));
//                throw new IOException("Default 'config.yml' not found in JAR resources.");
            } else {
                stream = defaultConfigStream;
            }
            Files.copy(stream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied new default config to 'config.yml'");
        }

        // If there's no backup, we're done (it's a fresh install).
        if (!backupFile.exists()) {
            System.out.println("Fresh install, no old values to migrate.");
            return;
        }

        // Step 3: Read backup and modify the new config
        Yaml snakeYaml = new Yaml();
        Map<String, Object> oldConfigData = snakeYaml.load(new FileInputStream(backupFile));
        CommentedYamlConfiguration newConfig = new CommentedYamlConfiguration(configFile);

        // 3a: Default recursive value preservation
        System.out.println("Preserving user values from backup...");
        preserveValues("", oldConfigData, newConfig);

        // 3b: Custom migration hook
        System.out.println("Applying custom migration rules...");
        BiConsumer<Map<String, Object>, CommentedYamlConfiguration> migrator = (oldData, config) -> {
            System.out.println("Executing migrator hook...");
            // Example: 'Reach.enable-post-packet' was removed and became 'enable-post-packet'
            if (oldData.containsKey("Reach")) {
                Object reachSection = oldData.get("Reach");
                if (reachSection instanceof Map) {
                    Map<String, Object> reachMap = (Map<String, Object>) reachSection;
                    if (reachMap.containsKey("enable-post-packet")) {
                        Object oldValue = reachMap.get("enable-post-packet");
                        // Use the node-based API to set the value at the new path
                        System.out.println("Migrating 'Reach.enable-post-packet' -> 'enable-post-packet'");
                        config.setValue("enable-post-packet", oldValue);
                    }
                }
            }
        };
        migrator.accept(oldConfigData, newConfig);

        // Finally, save all changes to the file.
        newConfig.save();
        System.out.println("Update complete. All changes have been saved to 'config.yml'.");
    }

    /**
     * Recursively traverses the old config data and applies its values to the new config
     * at the same path, but only if the path already exists in the new config.
     *
     * @param currentPath   The dot-notation path of the current section.
     * @param oldDataSection The map representing the current section from the old config.
     * @param newConfig     The CommentedYamlConfiguration to be modified.
     */
    private void preserveValues(String currentPath, Map<String, Object> oldDataSection, CommentedYamlConfiguration newConfig) {
        for (String key : oldDataSection.keySet()) {
            String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;
            Object oldValue = oldDataSection.get(key);

            if (oldValue instanceof Map) {
                // It's a nested section, recurse.
                preserveValues(fullPath, (Map<String, Object>) oldValue, newConfig);
            } else {
                // It's a leaf value (or a list). Set it in the new config.
                // The setValue method already checks if the path exists.
                newConfig.setValue(fullPath, oldValue);
            }
        }
    }

    // A dummy main method to make this runnable for testing purposes.
    // In your plugin, you would call `new ConfigUpdater().updateConfig();`
    public static void main(String[] args) throws IOException {
        // You would need to create dummy files here to test this standalone.
        // For example, create a "path/to/your/plugin/data-folder" directory,
        // put a dummy config.yml in it, and have a default config.yml in your resources.
         new ConfigUpdater().updateConfig();
        System.out.println("To test, create the necessary file structure and run the updateConfig() method.");
    }
}
