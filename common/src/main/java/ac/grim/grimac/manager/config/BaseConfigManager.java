package ac.grim.grimac.manager.config;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/*
 * This is to hold whatever config manager was set via the reload method in the API
 * and any global variables that are the same between players.
 */
public class BaseConfigManager {

    private final List<Pattern> ignoredClientPatterns = new ArrayList<>();
    @Getter
    private ConfigManager config = null;
    @Getter
    private boolean printAlertsToConsole = false;
    @Getter
    private String prefix = "&bGrim &8»";
    @Getter
    private String disconnectTimeout;
    @Getter
    private String disconnectClosed;
    @Getter
    private String disconnectPacketError;
    @Getter
    private String disconnectBlacklistedForge;
    @Getter
    private boolean blockBlacklistedForgeClients;

    @Getter
    private boolean disablePongCancelling;

    // initialize the config
    public void load(ConfigManager config) {
        this.config = config;

        int configuredMaxTransactionTime = config.getIntElse("max-transaction-time", 60);
        if (configuredMaxTransactionTime > 180 || configuredMaxTransactionTime < 1) {
            LogUtil.warn("Detected invalid max-transaction-time! This setting is clamped between 1 and 180 to prevent issues. Attempting to disable or set this too high can result in memory usage issues.");
        }

        ignoredClientPatterns.clear();
        for (String string : config.getStringList("client-brand.ignored-clients")) {
            try {
                ignoredClientPatterns.add(Pattern.compile(string));
            } catch (PatternSyntaxException e) {
                throw new RuntimeException("Failed to compile client pattern", e);
            }
        }

        printAlertsToConsole = config.getBooleanElse("alerts.print-to-console", true);
        prefix = config.getStringElse("prefix", "&bGrim &8»");

        disconnectTimeout = config.getStringElse("disconnect.timeout", "<lang:disconnect.timeout>");
        disconnectClosed = config.getStringElse("disconnect.closed", "<lang:disconnect.timeout>");
        disconnectPacketError = config.getStringElse("disconnect.error", "<red>An error occurred whilst processing packets. Please contact the administrators.");
        blockBlacklistedForgeClients = config.getBooleanElse("client-brand.disconnect-blacklisted-forge-versions", true);
        disconnectBlacklistedForge = config.getStringElse("disconnect.blacklisted-forge",
                "<red>Your forge version is blacklisted due to inbuilt reach hacks.<newline><gold>Versions affected: 1.18.2-1.19.3<newline><newline><red>Please see https://github.com/MinecraftForge/MinecraftForge/issues/9309.");

        disablePongCancelling = config.getBooleanElse("disable-pong-cancelling", false);
    }

    // ran on start, can be used to handle things that can't be done while loading
    public void start() {
    }

    public boolean isIgnoredClient(String brand) {
        for (Pattern pattern : ignoredClientPatterns) {
            if (pattern.matcher(brand).find()) return true;
        }
        return false;
    }
}
