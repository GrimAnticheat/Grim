package ac.grim.grimac.platform.fabric.utils.metrics;

import ac.grim.grimac.utils.anticheat.LogUtil;
import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class BStatsConfig {
    private static final String HEADER = """
            # bStats (https://bStats.org) collects some basic information for plugin authors, like how
            # many people use their plugin and their total player count. It's recommended to keep bStats
            # enabled, but if you're not comfortable with this, you can turn this setting off. There is no
            # performance penalty associated with having metrics enabled, and data sent to bStats is fully
            # anonymous.
            """;

    public static Config loadConfig() {
        File bStatsFolder = new File(FabricLoader.getInstance().getConfigDir().toString(), "bStats");
        File configFile = new File(bStatsFolder, "config.yml");

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        Map<String, Object> data;
        Config config = new Config();

        try {
            if (!configFile.exists()) {
                bStatsFolder.mkdirs();

                // Create default config
                data = new LinkedHashMap<>();
                data.put("enabled", true);
                data.put("serverUuid", UUID.randomUUID().toString());
                data.put("logFailedRequests", false);
                data.put("logSentData", false);
                data.put("logResponseStatusText", false);

                // Write config with header
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                    writer.write(HEADER);
                    yaml.dump(data, writer);
                }
            } else {
                // Load existing config
                data = yaml.load(new FileInputStream(configFile));
                if (data == null) {
                    data = new LinkedHashMap<>();
                }
            }

            // Map the data to Config object
            config.enabled = getBoolean(data, "enabled", true);
            config.serverUuid = getString(data, "serverUuid", UUID.randomUUID().toString());
            config.logFailedRequests = getBoolean(data, "logFailedRequests", false);
            config.logSentData = getBoolean(data, "logSentData", false);
            config.logResponseStatusText = getBoolean(data, "logResponseStatusText", false);

        } catch (IOException e) {
            LogUtil.error("Failed to load bStats config. Using default values.", e);
            // Fallback to default values
            config.enabled = true;
            config.serverUuid = UUID.randomUUID().toString();
            config.logFailedRequests = false;
            config.logSentData = false;
            config.logResponseStatusText = false;
        }

        return config;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    // Your existing Config class
    public static class Config {
        public boolean enabled = true;
        public String serverUuid;
        public boolean logFailedRequests = false;
        public boolean logSentData = false;
        public boolean logResponseStatusText = false;
    }
}
