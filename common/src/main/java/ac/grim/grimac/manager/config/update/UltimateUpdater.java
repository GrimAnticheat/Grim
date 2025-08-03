package ac.grim.grimac.manager.config.update;

import ac.grim.grimac.manager.config.update.ConfigPatcher;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UltimateUpdater {

    private static final int LATEST_CONFIG_VERSION = 10;

    public void updateConfig() throws IOException {
        // --- Setup and Analysis (same as before) ---
        File dataFolder = new File("/home/user/Desktop/Server/Network/fabric-1.21.7/config/GrimAC/");
        dataFolder.mkdirs();
        File configFile = new File(dataFolder, "config.yml");
        File backupFile = new File(dataFolder, "config.yml.bak");
        File newDefaultConfigFile = new File("/home/user/Desktop/Server/Network/fabric-1.21.7/config/GrimAC/new-config.yml");

        // Logic for fresh install...
        if (!configFile.exists()) {
            return;
        }

        Yaml yaml = new Yaml();
        Map<String, Object> oldConfigData;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            oldConfigData = yaml.load(fis);
        }

        int oldVersion = (int) oldConfigData.getOrDefault("config-version", 0);
        if (oldVersion >= LATEST_CONFIG_VERSION) {
            System.out.println("Config is up to date.");
            return;
        }

        System.out.println("Old config version (" + oldVersion + ") detected. Starting update...");

        // 1. Backup and copy new default
        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try (InputStream defaultConfigStream = new FileInputStream(newDefaultConfigFile)) {
            Files.copy(defaultConfigStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // 2. Analyze changes
        Map<String, Object> newConfigData = yaml.load(new FileInputStream(configFile));
        Map<String, Object> changesToApply = new HashMap<>();
        findChanges("", oldConfigData, newConfigData, changesToApply);

        // 3. *** THE HOOK IS HERE ***
        // Run specific migration logic based on the user's old version.
        runMigrations(oldVersion, oldConfigData, changesToApply);

        // 4. Surgically apply all changes
        if (!changesToApply.isEmpty()) {
            ConfigPatcher patcher = new ConfigPatcher(configFile);
            List<PendingChange> pendingChanges = new ArrayList<>();
            for (Map.Entry<String, Object> entry : changesToApply.entrySet()) {
                ConfigPatcher.NodePosition pos = patcher.getNodePosition(entry.getKey());
                if (pos != null) {
                    pendingChanges.add(new PendingChange(pos, entry.getValue()));
                }
            }
            Collections.sort(pendingChanges);
            for (PendingChange change : pendingChanges) {
                patcher.applyChange(change);
            }
            patcher.save();
        }

        System.out.println("Update complete. Formatting and all values have been preserved.");
    }

    // Helper class to hold a change and its original location
    public record PendingChange(ConfigPatcher.NodePosition position, Object value) implements Comparable<PendingChange> {
        // This sorts the list in descending order of line number.
        @Override
        public int compareTo(PendingChange other) {
            return Integer.compare(other.position.lineIndex(), this.position.lineIndex());
        }
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
                changesToMake.put(currentPath, oldValue);
            }
        }
    }

    /**
     * This is your custom migration hook. You can add any logic you need here
     * to handle moving or renaming keys between versions.
     *
     * @param oldVersion The config version the user was on.
     * @param oldData The full map of the user's old configuration.
     * @param changesToApply The map of changes that will be applied. You can modify this.
     */
    private void runMigrations(int oldVersion, Map<String, Object> oldData, Map<String, Object> changesToApply) {
        // This is the correct pattern for incremental updates.

        if (oldVersion < 8) {
            System.out.println("Migrating config from v7 or older...");
        }

        if (oldVersion < 9) {
            // Logic for migrating from v8 to v9...
            System.out.println("Migrating config from v8...");
        }

        if (oldVersion < 10) {
            System.out.println("Migrating config from v9...");
            // Example: 'Reach.enable-post-packet' was moved to the root level.
            Map<String, Object> reachSection = (Map<String, Object>) oldData.get("Reach");
            if (reachSection != null && reachSection.containsKey("enable-post-packet")) {
                Object oldValue = reachSection.get("enable-post-packet");
                // Add this change to our to-do list.
                changesToApply.put("enable-post-packet", oldValue);
            }

        }

        // Always set the config-version to the new version
        changesToApply.put("config-version", LATEST_CONFIG_VERSION);
    }

    public static void main(String[] args) throws IOException {
        new UltimateUpdater().updateConfig();
    }
}
