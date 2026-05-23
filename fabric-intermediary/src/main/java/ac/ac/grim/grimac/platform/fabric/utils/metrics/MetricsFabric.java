package ac.grim.grimac.platform.fabric.utils.metrics;

/*
 * This Metrics class was auto-generated and can be copied into your project if you are
 * not using a build tool like Gradle or Maven for dependency management.
 *
 * IMPORTANT: You are not allowed to modify this class, except changing the package.
 *
 * Disallowed modifications include but are not limited to:
 *  - Remove the option for users to opt-out
 *  - Change the frequency for data submission
 *  - Obfuscate the code (every obfuscator should allow you to make an exception for specific files)
 *  - Reformat the code (if you use a linter, add an exception)
 *
 * Violations will result in a ban of your plugin and account from bStats.
 */

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.fabricmc.loader.api.FabricLoader;

import java.util.logging.Level;

public class MetricsFabric implements Metrics {

    private final MetricsBase metricsBase;

    /**
     * Creates a new Metrics instance.
     *
     * @param serviceId The id of the service. It can be found at <a
     *                  href="https://bstats.org/what-is-my-plugin-id">What is my plugin id?</a>
     */
    public MetricsFabric(GrimPlugin plugin, int serviceId) {
        // Get the config file
        BStatsConfig.Config config = BStatsConfig.loadConfig();

        // Load the data
        boolean enabled = config.enabled;
        String serverUUID = config.serverUuid;
        boolean logErrors = config.logFailedRequests;
        boolean logSentData = config.logSentData;
        boolean logResponseStatusText = config.logResponseStatusText;

        metricsBase =
                new // See https://github.com/Bastian/bstats-metrics/pull/126
                        // See https://github.com/Bastian/bstats-metrics/pull/126
                        // See https://github.com/Bastian/bstats-metrics/pull/126
                        // See https://github.com/Bastian/bstats-metrics/pull/126
                        // See https://github.com/Bastian/bstats-metrics/pull/126
                        // See https://github.com/Bastian/bstats-metrics/pull/126
                        // See https://github.com/Bastian/bstats-metrics/pull/126
                        MetricsBase(
                        "fabric",
                        serverUUID,
                        serviceId,
                        enabled,
                        this::appendPlatformData,
                        this::appendServiceData,
                        submitDataTask -> GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(plugin, submitDataTask),
                        () -> true,
                        (message, error) -> plugin.getLogger().log(Level.WARNING, message, error),
                        (message) -> plugin.getLogger().log(Level.INFO, message),
                        logErrors,
                        logSentData,
                        logResponseStatusText,
                        false);
    }

    /**
     * Shuts down the underlying scheduler service.
     */
    public void shutdown() {
        metricsBase.shutdown();
    }

    /**
     * Adds a custom chart.
     *
     * @param chart The chart to add.
     */
    public void addCustomChart(CustomChart chart) {
        metricsBase.addCustomChart(chart);
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        builder.appendField("playerAmount", getPlayerAmount());
        builder.appendField("onlineMode", GrimACFabricLoaderPlugin.FABRIC_SERVER.usesAuthentication() ? 0 : 1);
        builder.appendField("bukkitVersion", GrimAPI.INSTANCE.getPlatformServer().getPlatformImplementationString());
        builder.appendField("bukkitName", "Fabric");
        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("pluginVersion", FabricLoader.getInstance().getModContainer("grimac").get().getMetadata().getVersion().getFriendlyString());
    }

    private int getPlayerAmount() {
        if (GrimACFabricLoaderPlugin.FABRIC_SERVER.isRunning()) {
            return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerCount();
        } else {
            return 0;
        }
    }
}
