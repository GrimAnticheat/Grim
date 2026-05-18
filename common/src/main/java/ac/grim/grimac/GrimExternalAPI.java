package ac.grim.grimac;

import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.events.GrimReloadEvent;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.grim.grimac.manager.config.ConfigManagerFileImpl;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.common.ConfigReloadObserver;
import ac.grim.grimac.utils.common.PropertiesUtil;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// This is used for grim's external API. It has its own class just for organization.
public class GrimExternalAPI implements GrimAbstractAPI, ConfigReloadObserver, StartableInitable {

    // Holder class — GrimExternalAPI is constructed inside GrimAPI's ctor,
    // so a plain static-final would see a null GrimAPI.INSTANCE. Holder
    // init runs on first fire, after GrimAPI is fully built.
    private static final class Channels {
        static final GrimReloadEvent.Channel RELOAD = GrimAPI.INSTANCE.getEventBus().get(GrimReloadEvent.class);
    }

    private final GrimAPI api;
    @Getter
    private final Map<String, Function<GrimUser, String>> variableReplacements = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, String> staticReplacements = new ConcurrentHashMap<>();
    private final Map<String, Function<Object, Object>> functions = new ConcurrentHashMap<>();
    private final ConfigManagerFileImpl configManagerFile = new ConfigManagerFileImpl();
    private final String grimVersion;
    private ConfigManager configManager = null;
    private boolean started = false;

    public GrimExternalAPI(GrimAPI api) {
        this.api = api;
        this.grimVersion = resolveGrimVersion(api);
    }

    @Override
    public @NotNull EventBus getEventBus() {
        return api.getEventBus();
    }

    @Override
    public @Nullable GrimUser getGrimUser(Player player) {
        return getGrimUser(player.getUniqueId());
    }

    @Override
    public @Nullable GrimUser getGrimUser(UUID uuid) {
        return api.getPlayerDataManager().getPlayer(uuid);
    }

    @Override
    public void registerVariable(String string, Function<GrimUser, String> replacement) {
        if (replacement == null) {
            variableReplacements.remove(string);
        } else {
            variableReplacements.put(string, replacement);
        }
    }

    @Override
    public void registerVariable(String variable, String replacement) {
        if (replacement == null) {
            staticReplacements.remove(variable);
        } else {
            staticReplacements.put(variable, replacement);
        }
    }

    @Override
    public String getGrimVersion() {
        return grimVersion;
    }

    private static String resolveGrimVersion(GrimAPI api) {
        try {
            Properties properties = PropertiesUtil.readProperties(GrimExternalAPI.class, "grimac.properties");
            String buildVersion = properties.getProperty("build.version");
            if (buildVersion != null && !buildVersion.isBlank() && !buildVersion.startsWith("${")) {
                return buildVersion;
            }
        } catch (RuntimeException ignored) {
        }

        try {
            return api.getGrimPlugin().getDescription().getVersion();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    @Override
    public void registerFunction(String key, Function<Object, Object> function) {
        if (function == null) {
            functions.remove(key);
        } else {
            functions.put(key, function);
        }
    }

    @Override
    public Function<Object, Object> getFunction(String key) {
        return functions.get(key);
    }

    @Override
    public AlertManager getAlertManager() {
        return GrimAPI.INSTANCE.getAlertManager();
    }

    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public boolean hasStarted() {
        return started;
    }

    @Override
    public int getCurrentTick() {
        return GrimAPI.INSTANCE.getTickManager().currentTick;
    }

    @Override
    public @NotNull GrimPlugin getGrimPlugin(@NotNull Object o) {
        return this.api.getExtensionManager().getPlugin(o);
    }

    @Override
    public @NotNull BackendRegistry getBackendRegistry() {
        return api.getBackendRegistry();
    }

    // on load, load the config & register the service
    public void load() {
        reload(configManagerFile);
        api.getLoader().registerAPIService();
    }

    // handles any config loading that's needed to be done after load
    @Override
    public void start() {
        started = true;
        try {
            GrimAPI.INSTANCE.getConfigManager().start();
        } catch (Exception e) {
            LogUtil.error("Failed to start config manager.", e);
        }
    }

    @Override
    public void reload(ConfigManager config) {
        if (config.isLoadedAsync() && started) {
            GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(),
                    () -> successfulReload(config));
        } else {
            successfulReload(config);
        }
    }

    @Override
    public CompletableFuture<Boolean> reloadAsync(ConfigManager config) {
        if (config.isLoadedAsync() && started) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(),
                    () -> future.complete(successfulReload(config)));
            return future;
        }
        return CompletableFuture.completedFuture(successfulReload(config));
    }

    private boolean successfulReload(ConfigManager config) {
        try {
            config.reload();
            GrimAPI.INSTANCE.getConfigManager().load(config);
            if (started) GrimAPI.INSTANCE.getConfigManager().start();
            onReload(config);
            if (started)
                GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(),
                        () -> Channels.RELOAD.fire(true));
            return true;
        } catch (Exception e) {
            LogUtil.error("Failed to reload config", e);
        }
        if (started)
            GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(),
                    () -> Channels.RELOAD.fire(false));
        return false;
    }

    @Override
    public void onReload(ConfigManager newConfig) {
        if (newConfig == null) {
            LogUtil.warn("ConfigManager not set. Using default config file manager.");
            configManager = configManagerFile;
        } else {
            configManager = newConfig;
        }
        // Update variables
        updateVariables();
        // Restart
        GrimAPI.INSTANCE.getAlertManager().reload(configManager);
        GrimAPI.INSTANCE.getDiscordManager().reload();
        GrimAPI.INSTANCE.getSpectateManager().reload();
        // First-load guard: load() calls reload() before start() runs, so this fires once with started=false before the datastore exists. Subsequent /grim reload calls see started=true and proceed (including disabled→enabled flips — DataStoreLifecycle.reload() re-evaluates builder.enabled() each time).
        if (!started) return;
        // Hot-reload picks up backend swaps + routing + connection-pool edits without a server restart. Drains in-flight writes for shutdown-drain-timeout-ms then drops; brief mid-reload unavailability is the tradeoff.
        if (GrimAPI.INSTANCE.getDataStoreLifecycle() != null) {
            GrimAPI.INSTANCE.getDataStoreLifecycle().reload();
        }
        // Reload checks for all players
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.runSafely(() -> player.reload(configManager));
        }
    }

    private void updateVariables() {
        variableReplacements.putIfAbsent("%player%", GrimUser::getName);
        variableReplacements.putIfAbsent("%uuid%", user -> user.getUniqueId().toString());
        variableReplacements.putIfAbsent("%ping%", user -> user.getTransactionPing() + "");
        variableReplacements.putIfAbsent("%brand%", GrimUser::getBrand);
        variableReplacements.putIfAbsent("%h_sensitivity%", user -> ((int) Math.round(user.getHorizontalSensitivity() * 200)) + "");
        variableReplacements.putIfAbsent("%v_sensitivity%", user -> ((int) Math.round(user.getVerticalSensitivity() * 200)) + "");
        variableReplacements.putIfAbsent("%fast_math%", user -> !user.isVanillaMath() + "");
        variableReplacements.putIfAbsent("%tps%", user -> String.format("%.2f", GrimAPI.INSTANCE.getPlatformServer().getTPS()));
        variableReplacements.putIfAbsent("%version%", GrimUser::getVersionName);
        // static variables
        staticReplacements.put("%prefix%", MessageUtil.translateAlternateColorCodes('&', GrimAPI.INSTANCE.getConfigManager().getPrefix()));
        staticReplacements.putIfAbsent("%grim_version%", getGrimVersion());
    }
}
