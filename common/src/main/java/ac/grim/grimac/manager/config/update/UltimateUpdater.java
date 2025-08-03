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

        if (configFile.exists()) {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        try (InputStream defaultConfigStream = new FileInputStream(newDefaultConfigFile)) {
            Files.copy(defaultConfigStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        if (!backupFile.exists()) return;

        Yaml yaml = new Yaml();
        Map<String, Object> oldConfigData = yaml.load(new FileInputStream(backupFile));
        Map<String, Object> newConfigData = yaml.load(new FileInputStream(configFile));
        Map<String, Object> changesToApply = new HashMap<>();
        findChanges("", oldConfigData, newConfigData, changesToApply);

        // Run specific migration logic based on the user's old version.
        runMigrations(oldVersion, oldConfigData, changesToApply);

        if (!changesToApply.isEmpty()) {
            // 1. Create the patcher, which builds the initial location map.
            ConfigPatcher patcher = new ConfigPatcher(configFile);

            // 2. Create a list of pending changes using the initial locations.
            List<PendingChange> pendingChanges = new ArrayList<>();
            for (Map.Entry<String, Object> entry : changesToApply.entrySet()) {
                ConfigPatcher.NodePosition pos = patcher.getNodePosition(entry.getKey());
                if (pos != null) {
                    pendingChanges.add(new PendingChange(pos, entry.getValue()));
                }
            }

            // 3. Sort the changes in REVERSE line order.
            Collections.sort(pendingChanges);

            // 4. Apply the changes from the bottom of the file to the top.
            for (PendingChange change : pendingChanges) {
                patcher.applyChange(change);
            }

            // 5. Save the final result.
            patcher.save();
        }

        System.out.println("Update complete. Formatting and all values have been preserved.");
    }

    // Helper class to hold a change and its original location
    public static class PendingChange implements Comparable<PendingChange> {
        final ConfigPatcher.NodePosition position;
        final Object value;

        PendingChange(ConfigPatcher.NodePosition position, Object value) {
            this.position = position;
            this.value = value;
        }

        // This sorts the list in descending order of line number.
        @Override
        public int compareTo(PendingChange other) {
            return Integer.compare(other.position.lineIndex, this.position.lineIndex);
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

    public static void main(String[] args) throws IOException {
        new UltimateUpdater().updateConfig();
    }
}
