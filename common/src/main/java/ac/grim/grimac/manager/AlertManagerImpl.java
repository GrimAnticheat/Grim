package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.config.ConfigReloadable;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;


import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Efficient implementation of AlertManager, handling state changes and notifications.
 * Caches toggle messages for performance.
 */
public final class AlertManagerImpl implements AlertManager, ConfigReloadable, StartableInitable {

    private final Set<PlatformPlayer> enabledAlerts = new CopyOnWriteArraySet<>();
    private final Set<PlatformPlayer> enabledVerbose = new CopyOnWriteArraySet<>();
    private final Set<PlatformPlayer> enabledBrands = new CopyOnWriteArraySet<>();

    private boolean consoleAlertsEnabled;
    private boolean consoleVerboseEnabled;
    private boolean consoleBrandsEnabled;

    private String alertsEnabledMsg = "";
    private String alertsDisabledMsg = "";
    private String verboseEnabledMsg = "";
    private String verboseDisabledMsg = "";
    private String brandsEnabledMsg = "";
    private String brandsDisabledMsg = "";

    private @NonNull PlatformServer platformServer;

    @Override
    public void start() {
        this.platformServer = GrimAPI.INSTANCE.getPlatformServer();
        reload(GrimAPI.INSTANCE.getConfigManager().getConfig());
    }

    @Override
    public void reload(ConfigManager config) {
        this.consoleAlertsEnabled = config.getBooleanElse("alerts.print-to-console", true);
        this.consoleVerboseEnabled = config.getBooleanElse("verbose.print-to-console", false);
        this.consoleBrandsEnabled = false;

        alertsEnabledMsg = config.getStringElse("alerts-enabled", "%prefix% &fAlerts enabled");
        alertsDisabledMsg = config.getStringElse("alerts-disabled", "%prefix% &fAlerts disabled");
        verboseEnabledMsg = config.getStringElse("verbose-enabled", "%prefix% &fVerbose enabled");
        verboseDisabledMsg = config.getStringElse("verbose-disabled", "%prefix% &fVerbose disabled");
        brandsEnabledMsg = config.getStringElse("brands-enabled", "%prefix% &fBrands enabled");
        brandsDisabledMsg = config.getStringElse("brands-disabled", "%prefix% &fBrands disabled");
    }

    /**
     * Gets the non-null PlatformPlayer from a GrimUser.
     * Throws IllegalArgumentException if the user is not a GrimPlayer.
     * Throws NullPointerException if the GrimPlayer's platformPlayer is null.
     */
    @NonNull
    private PlatformPlayer requirePlatformPlayerFromUser(@NonNull GrimUser user) {
        Objects.requireNonNull(user, "user cannot be null"); // Should be guaranteed by interface contract, but good practice

        if (!(user instanceof GrimPlayer grimPlayer)) {
            // Throw a specific exception if the type is wrong
            throw new IllegalArgumentException("AlertManager action called with non-GrimPlayer user: " + user.getName());
        }

        PlatformPlayer platformPlayer = grimPlayer.platformPlayer;

        // Throw NullPointerException with the specific message if platformPlayer is null
        Objects.requireNonNull(platformPlayer, "AlertManager action for user " + user.getName() + " with null platformPlayer (potentially during early join)");

        return platformPlayer;
    }

    /** Central logic for setting player state and conditionally sending messages. */
    private void setPlayerStateAndNotify(@NonNull GrimUser player, boolean enabled, boolean silent,
                                         @NonNull Set<PlatformPlayer> targetSet, @NonNull String type) {
        // requirePlatformPlayerFromUser handles null checks for player and platformPlayer
        // It will throw an exception if platformPlayer is null, stopping execution here.
        PlatformPlayer platformPlayer = requirePlatformPlayerFromUser(player);

        boolean changed;
        if (enabled) {
            changed = targetSet.add(platformPlayer);
        } else {
            changed = targetSet.remove(platformPlayer);
        }

        if (changed && !silent) {
            sendToggleMessage(platformPlayer, enabled, type);
        }
    }

    /** Retrieves the appropriate cached message string based on type and state. */
    @NonNull
    private String getCachedToggleMessage(boolean enabled, @NonNull String type) {
        String lowerType = type.toLowerCase();
        if (enabled) {
            return getString(type, lowerType, alertsEnabledMsg, verboseEnabledMsg, brandsEnabledMsg);
        } else {
            return getString(type, lowerType, alertsDisabledMsg, verboseDisabledMsg, brandsDisabledMsg);
        }
    }

    @NonNull
    private String getString(@NonNull String type, String lowerType, String alertsDisabledMsg, String verboseDisabledMsg, String brandsDisabledMsg) {
        return switch (lowerType) {
            case "alerts" -> alertsDisabledMsg;
            case "verbose" -> verboseDisabledMsg;
            case "brands" -> brandsDisabledMsg;
            default -> {
                GrimAPI.INSTANCE.getGrimPlugin().getLogger().warning("Invalid type passed to getCachedToggleMessage: " + type);
                yield "";
            }
        };
    }

    /** Gets the cached message, applies placeholders, and sends it to a PlatformPlayer. */
    private void sendToggleMessage(@NonNull PlatformPlayer player, boolean enabled, @NonNull String type) {
        String rawMessage = getCachedToggleMessage(enabled, type);
        if (rawMessage.isEmpty()) return;

        String messageWithPlaceholders = MessageUtil.replacePlaceholders(player, rawMessage);
        player.sendMessage(MessageUtil.miniMessage(messageWithPlaceholders));
    }

    /** Gets the cached message, applies generic placeholders, and sends it to the Console Sender. */
    private void sendToggleMessage(@NonNull Sender consoleSender, boolean enabled, @NonNull String type) {
        String rawMessage = getCachedToggleMessage(enabled, type);
        if (rawMessage.isEmpty()) return;

        String messageWithPlaceholders = MessageUtil.replacePlaceholders((PlatformPlayer)null, rawMessage);
        consoleSender.sendMessage(MessageUtil.miniMessage(messageWithPlaceholders));
    }



    @Override
    public boolean hasAlertsEnabled(@NonNull GrimUser player) {
        PlatformPlayer p = requirePlatformPlayerFromUser(player);
        return enabledAlerts.contains(p);
    }

    @Override
    public void setAlertsEnabled(@NonNull GrimUser player, boolean enabled, boolean silent) {
        // Let exceptions from requirePlatformPlayerFromUser propagate if called during set
        setPlayerStateAndNotify(player, enabled, silent, enabledAlerts, "Alerts");
    }

    @Override
    public boolean hasVerboseEnabled(@NonNull GrimUser player) {
        PlatformPlayer p = requirePlatformPlayerFromUser(player);
        return enabledVerbose.contains(p);
    }

    @Override
    public void setVerboseEnabled(@NonNull GrimUser player, boolean enabled, boolean silent) {
        setPlayerStateAndNotify(player, enabled, silent, enabledVerbose, "Verbose");
    }

    @Override
    public boolean hasBrandsEnabled(@NonNull GrimUser player) {
        GrimPlayer grimPlayer = (GrimPlayer) player;
        // Some proxies break packet order in sending brand and send the data too early for performance
        // which causes us to iterate over all players with this method
        // before platformPlayer is intialized; while generally packet order is important to maintain
        // for compatibles sake lets just default to not sending alerts to these players
        if (grimPlayer.platformPlayer == null) return false;

        PlatformPlayer p = requirePlatformPlayerFromUser(player);
        return enabledBrands.contains(p);
    }

    @Override
    public void setBrandsEnabled(@NonNull GrimUser player, boolean enabled, boolean silent) {
        setPlayerStateAndNotify(player, enabled, silent, enabledBrands, "Brands");
    }


    public void handlePlayerQuit(@NonNull GrimUser user) {
        Objects.requireNonNull(user, "user cannot be null");
        // We need to get PlatformPlayer *without* throwing an error on quit
        if (user instanceof GrimPlayer grimPlayer && grimPlayer.platformPlayer != null) {
            handlePlayerQuit(grimPlayer.platformPlayer);
        }
    }

    public void handlePlayerQuit(@NonNull PlatformPlayer platformPlayer) {
        // Null check for platformPlayer should be done by the caller if necessary
        enabledAlerts.remove(platformPlayer);
        enabledVerbose.remove(platformPlayer);
        enabledBrands.remove(platformPlayer);
    }

    public boolean toggleConsoleAlerts() {
        boolean newState = !this.consoleAlertsEnabled;
        this.consoleAlertsEnabled = newState;
        sendToggleMessage(platformServer.getConsoleSender(), newState, "Alerts");
        return newState;
    }

    public boolean toggleConsoleVerbose() {
        boolean newState = !this.consoleVerboseEnabled;
        this.consoleVerboseEnabled = newState;
        sendToggleMessage(platformServer.getConsoleSender(), newState, "Verbose");
        return newState;
    }

    public boolean toggleConsoleBrands() {
        boolean newState = !this.consoleBrandsEnabled;
        this.consoleBrandsEnabled = newState;
        sendToggleMessage(platformServer.getConsoleSender(), newState, "Brands");
        return newState;
    }

    // All internal code, will replace later
    private void setPlayerStateAndNotify(@NonNull PlatformPlayer platformPlayer, boolean enabled, boolean silent,
                                         @NonNull Set<PlatformPlayer> targetSet, @NonNull String type) {
        Objects.requireNonNull(platformPlayer, "platformPlayer cannot be null");
        boolean changed;
        if (enabled) {
            changed = targetSet.add(platformPlayer);
        } else {
            changed = targetSet.remove(platformPlayer);
        }

        if (changed && !silent) {
            sendToggleMessage(platformPlayer, enabled, type);
        }
    }

    private boolean togglePlayerStateAndNotify(@NonNull PlatformPlayer platformPlayer, boolean silent,
                                               @NonNull Set<PlatformPlayer> targetSet, @NonNull String type) {
        Objects.requireNonNull(platformPlayer, "platformPlayer cannot be null");
        boolean currentState = targetSet.contains(platformPlayer);
        boolean newState = !currentState; // The desired state after toggle

        // Use the set method to handle actual state change and notification
        setPlayerStateAndNotify(platformPlayer, newState, silent, targetSet, type);

        return newState; // Return the state *after* the toggle attempt
    }

    public boolean toggleBrands(@NonNull PlatformPlayer platformPlayer, boolean silent) {
        return togglePlayerStateAndNotify(platformPlayer, silent, enabledBrands, "Brands");
    }

    public boolean toggleVerbose(@NonNull PlatformPlayer platformPlayer, boolean silent) {
        return togglePlayerStateAndNotify(platformPlayer, silent, enabledVerbose, "Verbose");
    }

    public boolean toggleAlerts(@NonNull PlatformPlayer platformPlayer, boolean silent) {
        return togglePlayerStateAndNotify(platformPlayer, silent, enabledAlerts, "Alerts");
    }

    public void sendBrand(Component component) {
        for (PlatformPlayer platformPlayer : enabledBrands) {
            platformPlayer.sendMessage(component);
        }

        if (consoleBrandsEnabled) {
            platformServer.getConsoleSender().sendMessage(component);
        }
    }

    public void sendVerbose(Component component) {
        for (PlatformPlayer platformPlayer : enabledVerbose) {
            platformPlayer.sendMessage(component);
        }

        if (consoleVerboseEnabled) {
            platformServer.getConsoleSender().sendMessage(component);
        }
    }

    public void sendAlert(Component component) {
        for (PlatformPlayer platformPlayer : enabledAlerts) {
            platformPlayer.sendMessage(component);
        }

        if (consoleAlertsEnabled) {
            platformServer.getConsoleSender().sendMessage(component);
        }
    }

    public boolean hasVerboseListeners() {
        return !enabledVerbose.isEmpty() || consoleVerboseEnabled;
    }

    public boolean hasAlertListeners() {
        return !enabledAlerts.isEmpty() || consoleAlertsEnabled;
    }
}
