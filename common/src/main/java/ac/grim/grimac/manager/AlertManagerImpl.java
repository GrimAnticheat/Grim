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
    private boolean consoleAlertsEnabled;
    private boolean consoleVerboseEnabled;
    private boolean consoleBrandsEnabled;

    private @NonNull PlatformServer platformServer;

    private enum AlertType {
        NORMAL, VERBOSE, BRAND;
        public String enableMessage;
        public String disableMessage;
        public final Set<PlatformPlayer> players = new CopyOnWriteArraySet<>();
    }

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

        AlertType.NORMAL.enableMessage = config.getStringElse("alerts-enabled", "%prefix% &fAlerts enabled");
        AlertType.NORMAL.disableMessage = config.getStringElse("alerts-disabled", "%prefix% &fAlerts disabled");
        AlertType.VERBOSE.enableMessage = config.getStringElse("verbose-enabled", "%prefix% &fVerbose enabled");
        AlertType.VERBOSE.disableMessage = config.getStringElse("verbose-disabled", "%prefix% &fVerbose disabled");
        AlertType.BRAND.enableMessage = config.getStringElse("brands-enabled", "%prefix% &fBrands enabled");
        AlertType.BRAND.disableMessage = config.getStringElse("brands-disabled", "%prefix% &fBrands disabled");
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
    private void setPlayerStateAndNotify(@NonNull GrimUser player, boolean enabled, boolean silent, @NonNull AlertType type) {
        // requirePlatformPlayerFromUser handles null checks for player and platformPlayer
        // It will throw an exception if platformPlayer is null, stopping execution here.
        PlatformPlayer platformPlayer = requirePlatformPlayerFromUser(player);

        boolean changed = enabled ? type.players.add(platformPlayer) : type.players.remove(platformPlayer);

        if (changed && !silent) {
            sendToggleMessage(platformPlayer, enabled, type);
        }
    }

    /** Retrieves the appropriate cached message string based on type and state. */
    @NonNull
    private String getCachedToggleMessage(boolean enabled, @NonNull AlertType type) {
        return enabled ? type.enableMessage : type.disableMessage;
    }

    /** Gets the cached message, applies placeholders, and sends it to a PlatformPlayer. */
    private void sendToggleMessage(@NonNull PlatformPlayer player, boolean enabled, @NonNull AlertType type) {
        String rawMessage = getCachedToggleMessage(enabled, type);
        if (rawMessage.isEmpty()) return;

        String messageWithPlaceholders = MessageUtil.replacePlaceholders(player, rawMessage);
        player.sendMessage(MessageUtil.miniMessage(messageWithPlaceholders));
    }

    /** Gets the cached message, applies generic placeholders, and sends it to the Console Sender. */
    private void sendToggleMessage(@NonNull Sender consoleSender, boolean enabled, @NonNull AlertType type) {
        String rawMessage = getCachedToggleMessage(enabled, type);
        if (rawMessage.isEmpty()) return;

        String messageWithPlaceholders = MessageUtil.replacePlaceholders((PlatformPlayer) null, rawMessage);
        consoleSender.sendMessage(MessageUtil.miniMessage(messageWithPlaceholders));
    }

    @Override
    public boolean hasAlertsEnabled(@NonNull GrimUser player) {
        return AlertType.NORMAL.players.contains(requirePlatformPlayerFromUser(player));
    }

    @Override
    public void setAlertsEnabled(@NonNull GrimUser player, boolean enabled, boolean silent) {
        // Let exceptions from requirePlatformPlayerFromUser propagate if called during set
        setPlayerStateAndNotify(player, enabled, silent, AlertType.NORMAL);
        if (!enabled) setVerboseEnabled(player, false, silent);
    }

    @Override
    public boolean hasVerboseEnabled(@NonNull GrimUser player) {
        return AlertType.VERBOSE.players.contains(requirePlatformPlayerFromUser(player));
    }

    @Override
    public void setVerboseEnabled(@NonNull GrimUser player, boolean enabled, boolean silent) {
        if (enabled) setAlertsEnabled(player, true, silent);
        setPlayerStateAndNotify(player, enabled, silent, AlertType.VERBOSE);
    }

    @Override
    public boolean hasBrandsEnabled(@NonNull GrimUser player) {
        GrimPlayer grimPlayer = (GrimPlayer) player;
        // Some proxies break packet order in sending brand and send the data too early for performance
        // which causes us to iterate over all players with this method
        // before platformPlayer is intialized; while generally packet order is important to maintain
        // for compatibles sake lets just default to not sending alerts to these players
        if (grimPlayer.platformPlayer == null) return false;

        return AlertType.BRAND.players.contains(grimPlayer.platformPlayer);
    }

    @Override
    public void setBrandsEnabled(@NonNull GrimUser player, boolean enabled, boolean silent) {
        setPlayerStateAndNotify(player, enabled, silent, AlertType.BRAND);
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
        AlertType.NORMAL.players.remove(platformPlayer);
        AlertType.VERBOSE.players.remove(platformPlayer);
        AlertType.BRAND.players.remove(platformPlayer);
    }

    public boolean toggleConsoleAlerts() {
        boolean newState = !this.consoleAlertsEnabled;
        this.consoleAlertsEnabled = newState;
        sendToggleMessage(platformServer.getConsoleSender(), newState, AlertType.NORMAL);
        return newState;
    }

    public boolean toggleConsoleVerbose() {
        boolean newState = !this.consoleVerboseEnabled;
        this.consoleVerboseEnabled = newState;
        sendToggleMessage(platformServer.getConsoleSender(), newState, AlertType.VERBOSE);
        return newState;
    }

    public boolean toggleConsoleBrands() {
        boolean newState = !this.consoleBrandsEnabled;
        this.consoleBrandsEnabled = newState;
        sendToggleMessage(platformServer.getConsoleSender(), newState, AlertType.BRAND);
        return newState;
    }

    // All internal code, will replace later
    private void setPlayerStateAndNotify(@NonNull PlatformPlayer platformPlayer, boolean enabled, boolean silent, @NonNull AlertType type) {
        Objects.requireNonNull(platformPlayer, "platformPlayer cannot be null");
        boolean changed = enabled ? type.players.add(platformPlayer) : type.players.remove(platformPlayer);

        if (changed && !silent) {
            sendToggleMessage(platformPlayer, enabled, type);
        }
    }

    private boolean togglePlayerStateAndNotify(@NonNull PlatformPlayer platformPlayer, boolean silent, @NonNull AlertType type) {
        Objects.requireNonNull(platformPlayer, "platformPlayer cannot be null");
        boolean newState = !type.players.contains(platformPlayer); // The desired state after toggle

        // Use the set method to handle actual state change and notification
        setPlayerStateAndNotify(platformPlayer, newState, silent, type);

        return newState; // Return the state *after* the toggle attempt
    }

    public boolean toggleBrands(@NonNull PlatformPlayer platformPlayer, boolean silent) {
        return togglePlayerStateAndNotify(platformPlayer, silent, AlertType.BRAND);
    }

    public boolean toggleVerbose(@NonNull PlatformPlayer platformPlayer, boolean silent) {
        return togglePlayerStateAndNotify(platformPlayer, silent, AlertType.VERBOSE);
    }

    public boolean toggleAlerts(@NonNull PlatformPlayer platformPlayer, boolean silent) {
        return togglePlayerStateAndNotify(platformPlayer, silent, AlertType.NORMAL);
    }

    public void sendBrand(Component component) {
        for (PlatformPlayer platformPlayer : AlertType.BRAND.players) {
            platformPlayer.sendMessage(component);
        }

        if (consoleBrandsEnabled) {
            platformServer.getConsoleSender().sendMessage(component);
        }
    }

    public void sendVerbose(Component component) {
        for (PlatformPlayer platformPlayer : AlertType.VERBOSE.players) {
            platformPlayer.sendMessage(component);
        }

        if (consoleVerboseEnabled) {
            platformServer.getConsoleSender().sendMessage(component);
        }
    }

    public void sendAlert(Component component) {
        for (PlatformPlayer platformPlayer : AlertType.NORMAL.players) {
            platformPlayer.sendMessage(component);
        }

        if (consoleAlertsEnabled) {
            platformServer.getConsoleSender().sendMessage(component);
        }
    }

    public boolean hasVerboseListeners() {
        return !AlertType.VERBOSE.players.isEmpty() || consoleVerboseEnabled;
    }

    public boolean hasAlertListeners() {
        return !AlertType.NORMAL.players.isEmpty() || consoleAlertsEnabled;
    }
}
