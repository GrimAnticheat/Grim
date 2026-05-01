package ac.grim.grimac.manager.config;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.common.BasicReloadable;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.manager.config.update.ConfigUpdater;
import ac.grim.grimac.manager.config.update.GrimConfigSpecs;
import ac.grim.grimac.utils.anticheat.LogUtil;
import github.scarsz.configuralize.DynamicConfig;
import github.scarsz.configuralize.Language;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigManagerFileImpl implements ConfigManager, BasicReloadable {

    private final DynamicConfig config;
    private boolean initialized = false;

    public ConfigManagerFileImpl() {
        config = new DynamicConfig();
    }

    private File getConfigFile(String path) {
        return new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), path);
    }

    /** Backend ids whose per-backend yml gets loaded + auto-updated alongside the user-facing files. */
    static final List<String> BACKEND_IDS = List.of("sqlite", "mysql", "postgres", "mongo", "redis");

    private void runConfigUpdates() {
        Logger logger = Logger.getLogger("grim-config");
        ConfigUpdater updater = new ConfigUpdater(GrimAPI.class, logger);
        // Use the multi-file API so cross-file migrations (e.g. config.yml
        // v9 → v10 lifting history.database.* into database.yml + the
        // matching databases/<id>.yml) can target sibling files via
        // ctx.otherFile(...). punishments.yml is intentionally absent —
        // open-ended user-defined data, no schema versioning.
        Map<File, ConfigUpdater.Spec> batch = new LinkedHashMap<>();
        batch.put(getConfigFile("config.yml"), GrimConfigSpecs.mainConfig());
        batch.put(getConfigFile("discord.yml"), GrimConfigSpecs.discord());
        batch.put(getConfigFile("messages.yml"), GrimConfigSpecs.messages());
        batch.put(getConfigFile("database.yml"), GrimConfigSpecs.database());
        for (String id : BACKEND_IDS) {
            batch.put(getConfigFile("databases/" + id + ".yml"), GrimConfigSpecs.backend(id));
        }
        // Pass the same language Configuralize resolved (after its own
        // all-or-nothing availability check above) so a zh operator gets
        // ZH-comment bundled defaults stamped on disk to match.
        String langCode = config.getLanguage().getCode().toLowerCase();
        try {
            updater.updateAll(batch, langCode);
        } catch (Exception e) {
            LogUtil.warn("ConfigUpdater batch failed — loading on-disk files as-is: " + e);
        }
    }

    @Override
    public void reload() {
        GrimAPI.INSTANCE.getGrimPlugin().getDataFolder().mkdirs();
        if (!initialized) {
            initialized = true;
            config.addSource(GrimAPI.class, "config", getConfigFile("config.yml"));
            config.addSource(GrimAPI.class, "messages", getConfigFile("messages.yml"));
            config.addSource(GrimAPI.class, "discord", getConfigFile("discord.yml"));
            config.addSource(GrimAPI.class, "punishments", getConfigFile("punishments.yml"));
            // database.yml + per-backend files load through here too; their
            // keys are namespaced under `database:` / `<id>:` wrappers so
            // they don't collide with config.yml / discord.yml / each other
            // when Configuralize merges everything into one keyspace.
            config.addSource(GrimAPI.class, "database", getConfigFile("database.yml"));
            for (String id : BACKEND_IDS) {
                config.addSource(GrimAPI.class, "databases/" + id, getConfigFile("databases/" + id + ".yml"));
            }
        }

        String languageCode = System.getProperty("user.language").toUpperCase();

        try {
            config.setLanguage(Language.valueOf(languageCode));
        } catch (IllegalArgumentException ignored) { // not a valid language code
        }

        // Logic for system language
        if (!config.isLanguageAvailable(config.getLanguage())) {
            String lang = languageCode.toUpperCase();
            LogUtil.info("Unknown user language " + lang + ".");
            LogUtil.info("If you fluently speak " + lang + " as well as English, see the GitHub repo to translate it!");
            config.setLanguage(Language.EN);
        }

        try {
            // Save bundled defaults BEFORE the updater runs so any cross-file
            // migration (e.g. config.yml v9 → v10 lifting history.database.*
            // into database.yml + databases/<id>.yml) can target files that
            // exist on disk. saveAllDefaults(false) is non-overwriting — the
            // operator's existing files keep their current content.
            config.saveAllDefaults(false);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save default config files", e);
        }

        // Cross-version migrations run AFTER Configuralize creates any
        // missing files. Idempotent on already-current configs. Versioned
        // backups (<name>.v<oldVersion>.bak) preserve operator-rollback
        // evidence across stacked migration steps.
        runConfigUpdates();

        try {
            config.loadAll();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    private void upgrade() {
        File config = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "config.yml");
        if (config.exists()) {
            try {
                String configString = new String(Files.readAllBytes(config.toPath()));

                int configVersion = configString.indexOf("config-version: ");

                if (configVersion != -1) {
                    String configStringVersion = configString.substring(configVersion + "config-version: ".length());
                    configStringVersion = configStringVersion.substring(0, !configStringVersion.contains("\n") ? configStringVersion.length() : configStringVersion.indexOf("\n"));
                    configStringVersion = configStringVersion.replaceAll("\\D", "");

                    configVersion = Integer.parseInt(configStringVersion);
                    // TODO: Do we have to hardcode this?
                    configString = configString.replaceAll("config-version: " + configStringVersion, "config-version: 9");
                    Files.write(config.toPath(), configString.getBytes());

                    upgradeModernConfig(config, configString, configVersion);
                } else {
                    removeLegacyTwoPointOne(config);
                }

            } catch (IOException e) {
                LogUtil.error("Failed to upgrade config file", e);
            }
        }
    }

    private void upgradeModernConfig(File config, String configString, int configVersion) throws IOException {
        if (configVersion < 1) {
            addMaxPing(config, configString);
        }
        if (configVersion < 2) {
            addMissingPunishments();
        }
        if (configVersion < 3) {
            addBaritoneCheck();
        }
        if (configVersion < 4) {
            newOffsetNewDiscordConf(config, configString);
        }
        if (configVersion < 5) {
            fixBadPacketsAndAdjustPingConfig(config, configString);
        }
        if (configVersion < 6) {
            addSuperDebug(config, configString);
        }
        if (configVersion < 7) {
            removeAlertsOnJoin(config, configString);
        }
        if (configVersion < 8) {
            addPacketSpamThreshold(config, configString);
        }
        if (configVersion < 9) {
            newOffsetHandlingAntiKB(config, configString);
        }
    }

    private void removeLegacyTwoPointOne(File config) throws IOException {
        // If config doesn't have config-version, it's a legacy config
        Files.move(config.toPath(), new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "config-2.1.old.yml").toPath());
    }

    private void addMaxPing(File config, String configString) throws IOException {
        configString += "\n\n\n" +
                "# How long should players have until we keep them for timing out? Default = 2 minutes\n" +
                "max-ping: 120";

        Files.write(config.toPath(), configString.getBytes());
    }

    // TODO: Write conversion for this... I'm having issues with windows new lines
    private void addMissingPunishments() {
        File config = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "punishments.yml");
        String configString;
        if (config.exists()) {
            try {
                configString = new String(Files.readAllBytes(config.toPath()));

                // If it works, it isn't stupid.  Only replace it if it exactly matches the default config.
                int commentIndex = configString.indexOf("  # As of 2.2.2 these are just placeholders, there are no Killaura/Aim/Autoclicker checks other than those that");
                if (commentIndex != -1) {

                    configString = configString.substring(0, commentIndex);
                    configString += "  Combat:\n" +
                            "    remove-violations-after: 300\n" +
                            "    checks:\n" +
                            "      - \"Killaura\"\n" +
                            "      - \"Aim\"\n" +
                            "    commands:\n" +
                            "      - \"20:40 [alert]\"\n" +
                            "  # As of 2.2.10, there are no AutoClicker checks and this is a placeholder. 2.3 will include AutoClicker checks.\n" +
                            "  Autoclicker:\n" +
                            "    remove-violations-after: 300\n" +
                            "    checks:\n" +
                            "      - \"Autoclicker\"\n" +
                            "    commands:\n" +
                            "      - \"20:40 [alert]\"\n";
                }

                Files.write(config.toPath(), configString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void fixBadPacketsAndAdjustPingConfig(File config, String configString) {
        try {
            configString = configString.replaceAll("max-ping: \\d+", "max-transaction-time: 60");
            Files.write(config.toPath(), configString.getBytes());
        } catch (IOException ignored) {
        }

        File punishConfig = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "punishments.yml");
        String punishConfigString;
        if (punishConfig.exists()) {
            try {
                punishConfigString = new String(Files.readAllBytes(punishConfig.toPath()));
                punishConfigString = punishConfigString.replace("commands:", "commands:");
                Files.write(punishConfig.toPath(), punishConfigString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void addBaritoneCheck() {
        File config = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "punishments.yml");
        String configString;
        if (config.exists()) {
            try {
                configString = new String(Files.readAllBytes(config.toPath()));
                configString = configString.replace("      - \"EntityControl\"\n", "      - \"EntityControl\"\n      - \"Baritone\"\n      - \"FastBreak\"\n");
                Files.write(config.toPath(), configString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void newOffsetNewDiscordConf(File config, String configString) throws IOException {
        configString = configString.replace("threshold: 0.0001", "threshold: 0.001"); // 1e-5 -> 1e-4 default flag level
        configString = configString.replace("threshold: 0.00001", "threshold: 0.001"); // 1e-6 -> 1e-4 antikb flag
        Files.write(config.toPath(), configString.getBytes());

        File discordFile = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "discord.yml");

        if (discordFile.exists()) {
            try {
                String discordString = new String(Files.readAllBytes(discordFile.toPath()));
                discordString += "\nembed-color: \"#00FFFF\"\n" +
                        "violation-content:\n" +
                        "  - \"**Player**: %player%\"\n" +
                        "  - \"**Check**: %check%\"\n" +
                        "  - \"**Violations**: %violations%\"\n" +
                        "  - \"**Client Version**: %version%\"\n" +
                        "  - \"**Brand**: %brand%\"\n" +
                        "  - \"**Ping**: %ping%\"\n" +
                        "  - \"**TPS**: %tps%\"\n";
                Files.write(discordFile.toPath(), discordString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void addSuperDebug(File config, String configString) throws IOException {
        // The default config didn't have this change
        configString = configString.replace("threshold: 0.0001", "threshold: 0.001"); // 1e-5 -> 1e-4 default flag level
        if (!configString.contains("experimental-checks")) {
            configString += "\n\n# Enables experimental checks\n" +
                    "experimental-checks: false\n\n";
        }
        configString += "\nverbose:\n" +
                "  print-to-console: false\n";
        Files.write(config.toPath(), configString.getBytes());

        File messageFile = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "messages.yml");
        if (messageFile.exists()) {
            try {
                String messagesString = new String(Files.readAllBytes(messageFile.toPath()));
                messagesString += "\n\nupload-log: \"%prefix% &fUploaded debug to: %url%\"\n" +
                        "upload-log-start: \"%prefix% &fUploading log... please wait\"\n" +
                        "upload-log-not-found: \"%prefix% &cUnable to find that log\"\n" +
                        "upload-log-upload-failure: \"%prefix% &cSomething went wrong while uploading this log, see console for more info\"\n";
                Files.write(messageFile.toPath(), messagesString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void removeAlertsOnJoin(File config, String configString) throws IOException {
        configString = configString.replaceAll("  # Should players with grim\\.alerts permission automatically enable alerts on join\\?\r?\n  enable-on-join: (?:true|false)\r?\n", ""); // en
        configString = configString.replaceAll("  # 管理员进入时是否自动开启警告？\r?\n  enable-on-join: (?:true|false)\r?\n", ""); // zh
        Files.write(config.toPath(), configString.getBytes());
    }

    private void addPacketSpamThreshold(File config, String configString) throws IOException {
        configString += "\n# Grim sometimes cancels illegal packets such as with timer, after X packets in a second cancelled, when should\n" +
                "# we simply kick the player? This is required as some packet limiters don't count packets cancelled by grim.\n" +
                "packet-spam-threshold: 150\n";
        Files.write(config.toPath(), configString.getBytes());
    }

    private void newOffsetHandlingAntiKB(File config, String configString) throws IOException {
        configString = configString.replaceAll("  # How much of an offset is \"cheating\"\r?\n  # By default this is 1e-5, which is safe and sane\r?\n  # Measured in blocks from the correct movement\r?\n  threshold: 0.001\r?\n  setbackvl: 3",
                "  # How much should we multiply total advantage by when the player is legit\n" +
                        "  setback-decay-multiplier: 0.999\n" +
                        "  # How large of an offset from the player's velocity should we create a violation for?\n" +
                        "  # Measured in blocks from the possible velocity\n" +
                        "  threshold: 0.001\n" +
                        "  # How large of a violation in a tick before the player gets immediately setback?\n" +
                        "  # -1 to disable\n" +
                        "  immediate-setback-threshold: 0.1\n" +
                        "  # How large of an advantage over all ticks before we start to setback?\n" +
                        "  # -1 to disable\n" +
                        "  max-advantage: 1\n" +
                        "  # This is to stop the player from gathering too many violations and never being able to clear them all\n" +
                        "  max-ceiling: 4"
        );
        Files.write(config.toPath(), configString.getBytes());
    }

    @Override
    public String getStringElse(String key, String otherwise) {
        return config.getStringElse(key, otherwise);
    }

    @Override
    public @Nullable String getString(String key) {
        return config.getString(key);
    }

    @Override
    public List<String> getStringList(String key) {
        return config.getStringList(key);
    }

    @Override
    public List<String> getStringListElse(String key, List<String> otherwise) {
        return config.getStringListElse(key, otherwise);
    }

    @Override
    public int getIntElse(String key, int other) {
        return config.getIntElse(key, other);
    }

    @Override
    public long getLongElse(String key, long otherwise) {
        return config.getLongElse(key, otherwise);
    }

    @Override
    public double getDoubleElse(String key, double otherwise) {
        return config.getDoubleElse(key, otherwise);
    }

    @Override
    public boolean getBooleanElse(String key, boolean otherwise) {
        return config.getBooleanElse(key, otherwise);
    }

    @Override
    public <T> T get(String key) {
        return config.get(key);
    }

    @Override
    public <T> @Nullable T getElse(String key, T otherwise) {
        return config.getElse(key, otherwise);
    }

    @Override
    public <K, V> Map<K, V> getMap(String key) {
        return config.getMap(key);
    }

    @Override
    public @Nullable <K, V> Map<K, V> getMapElse(String s, Map<K, V> map) {
        return config.getMapElse(s, map);
    }

    @Override
    public @Nullable <T> List<T> getList(String path) {
        return config.getList(path);
    }

    @Override
    public @Nullable <T> List<T> getListElse(String path, List<T> otherwise) {
        return config.getListElse(path, otherwise);
    }

    @Override
    public boolean hasLoaded() {
        return initialized;
    }

}
