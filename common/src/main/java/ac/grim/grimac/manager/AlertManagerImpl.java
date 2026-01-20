package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.config.ConfigReloadable;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Efficient implementation of AlertManager, handling state changes and notifications.
 * Caches toggle messages for performance.
 */
public final class AlertManagerImpl implements AlertManager, ConfigReloadable, StartableInitable {
    private static @NotNull PlatformServer platformServer;

    private enum AlertType {
        NORMAL, VERBOSE, BRAND;

        public String enableMessage;
        public String disableMessage;
        public final Set<PlatformPlayer> players = new CopyOnWriteArraySet<>();
        public boolean console;

        @Contract(pure = true)
        public boolean hasListeners() {
            return !players.isEmpty() || console;
        }

        @Contract(pure = true)
        public String getToggleMessage(boolean enabled) {
            return enabled ? enableMessage : disableMessage;
        }

        /**
         * @param component the message to send to listeners
         * @param excluding the listeners to exclude, null means console
         * @return listeners this message was sent to, null means console
         */
        public Set<@Nullable PlatformPlayer> send(Component component, @Nullable Set<@Nullable PlatformPlayer> excluding) {
            HashSet<PlatformPlayer> listeners = new HashSet<>(players);
            if (excluding != null) {
                listeners.removeAll(excluding);
            }

            for (PlatformPlayer platformPlayer : listeners) {
                platformPlayer.sendMessage(component);
            }

            if (console && (excluding == null || !excluding.contains(null))) {
                platformServer.getConsoleSender().sendMessage(component);
                listeners.add(null);
            }

            return listeners;
        }
    }

    @Override
    public void start() {
        platformServer = GrimAPI.INSTANCE.getPlatformServer();
        reload(GrimAPI.INSTANCE.getConfigManager().getConfig());
    }

    @Override
    public void reload(ConfigManager config) {
        setConsoleAlertsEnabled(config.getBooleanElse("alerts.print-to-console", true), true);
        setConsoleVerboseEnabled(config.getBooleanElse("verbose.print-to-console", false), true);

        AlertType.NORMAL.enableMessage = config.getStringElse("alerts-enabled", "%prefix% &fAlerts enabled");
        AlertType.NORMAL.disableMessage = config.getStringElse("alerts-disabled", "%prefix% &fAlerts disabled");
        AlertType.VERBOSE.enableMessage = config.getStringElse("verbose-enabled", "%prefix% &fVerbose enabled");
        AlertType.VERBOSE.disableMessage = config.getStringElse("verbose-disabled", "%prefix% &fVerbose disabled");
        AlertType.BRAND.enableMessage = config.getStringElse("brands-enabled", "%prefix% &fBrands enabled");
        AlertType.BRAND.disableMessage = config.getStringElse("brands-disabled", "%prefix% &fBrands disabled");
    }

    /**
     * Gets the non-null PlatformPlayer from a GrimUser.
     * @throws IllegalArgumentException if the user is not a GrimPlayer.
     * @throws NullPointerException if the GrimPlayer's platformPlayer is null.
     */
    private @NotNull PlatformPlayer requirePlatformPlayerFromUser(@NotNull GrimUser user) {
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

    /** Gets the cached message, applies placeholders, and sends it to a PlatformPlayer. */
    private static void sendToggleMessage(@NotNull PlatformPlayer player, boolean enabled, @NotNull AlertType type) {
        String rawMessage = type.getToggleMessage(enabled);
        if (rawMessage.isEmpty()) return;

        String messageWithPlaceholders = MessageUtil.replacePlaceholders(player, rawMessage);
        player.sendMessage(MessageUtil.miniMessage(messageWithPlaceholders));
    }

    @Override
    public boolean hasAlertsEnabled(@NotNull GrimUser player) {
        return hasAlertsEnabled(requirePlatformPlayerFromUser(player));
    }

    @Override
    public void setAlertsEnabled(@NotNull GrimUser player, boolean enabled, boolean silent) {
        setAlertsEnabled(requirePlatformPlayerFromUser(player), enabled, silent);
    }

    @Override
    public boolean hasVerboseEnabled(@NotNull GrimUser player) {
        return hasVerboseEnabled(requirePlatformPlayerFromUser(player));
    }

    @Override
    public void setVerboseEnabled(@NotNull GrimUser player, boolean enabled, boolean silent) {
        setVerboseEnabled(requirePlatformPlayerFromUser(player), enabled, silent);
    }

    @Override
    public boolean hasBrandsEnabled(@NotNull GrimUser player) {
        GrimPlayer grimPlayer = (GrimPlayer) player;
        // Some proxies break packet order in sending brand and send the data too early for performance
        // which causes us to iterate over all players with this method
        // before platformPlayer is intialized; while generally packet order is important to maintain
        // for compatibility's sake lets just default to not sending alerts to these players
        if (grimPlayer.platformPlayer == null) return false;

        return hasBrandsEnabled(grimPlayer.platformPlayer);
    }

    @Override
    public void setBrandsEnabled(@NotNull GrimUser player, boolean enabled, boolean silent) {
        setPlayerStateAndNotify(requirePlatformPlayerFromUser(player), enabled, silent, AlertType.BRAND);
    }

    @Override
    public boolean hasAlertsEnabled(Player player) {
        if (player == null) return false;
        return hasAlertsEnabled(GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromNativePlayerType(player));
    }

    @Override
    public void toggleAlerts(Player player) {
        if (player == null) return;
        toggleAlerts(GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromNativePlayerType(player));
    }

    @Override
    public boolean hasVerboseEnabled(Player player) {
        if (player == null) return false;
        return hasVerboseEnabled(GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromNativePlayerType(player));
    }

    @Override
    public void toggleVerbose(Player player) {
        if (player == null) return;
        toggleVerbose(GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromNativePlayerType(player));
    }

    public void handlePlayerQuit(@Nullable PlatformPlayer platformPlayer) {
        if (platformPlayer == null) return;

        AlertType.NORMAL.players.remove(platformPlayer);
        AlertType.VERBOSE.players.remove(platformPlayer);
        AlertType.BRAND.players.remove(platformPlayer);
    }

    public boolean toggleConsoleAlerts() {
        return toggleConsoleAlerts(false);
    }

    public boolean toggleConsoleAlerts(boolean silent) {
        return setConsoleAlertsEnabled(!hasConsoleAlertsEnabled(), silent);
    }

    @Contract("_ -> param1")
    public boolean setConsoleAlertsEnabled(boolean enabled) {
        return setConsoleAlertsEnabled(enabled, false);
    }

    @Contract("_, _ -> param1")
    public boolean setConsoleAlertsEnabled(boolean enabled, boolean silent) {
        setConsoleStateAndNotify(AlertType.NORMAL, enabled, silent);
        if (!enabled) setConsoleVerboseEnabled(false, silent);
        return enabled;
    }

    @Contract(pure = true)
    public boolean hasConsoleAlertsEnabled() {
        return AlertType.NORMAL.console;
    }

    public boolean toggleConsoleVerbose() {
        return toggleConsoleVerbose(false);
    }

    public boolean toggleConsoleVerbose(boolean silent) {
        return setConsoleVerboseEnabled(!hasConsoleVerboseEnabled(), silent);
    }

    @Contract("_ -> param1")
    public boolean setConsoleVerboseEnabled(boolean enabled) {
        return setConsoleVerboseEnabled(enabled, false);
    }

    @Contract("_, _ -> param1")
    public boolean setConsoleVerboseEnabled(boolean enabled, boolean silent) {
        if (enabled) setConsoleAlertsEnabled(true, silent);
        return setConsoleStateAndNotify(AlertType.VERBOSE, enabled, silent);
    }

    @Contract(pure = true)
    public boolean hasConsoleVerboseEnabled() {
        return AlertType.VERBOSE.console;
    }

    public boolean toggleConsoleBrands() {
        return toggleConsoleBrands(false);
    }

    public boolean toggleConsoleBrands(boolean silent) {
        return setConsoleBrandsEnabled(!hasConsoleBrandsEnabled(), silent);
    }

    @Contract("_ -> param1")
    public boolean setConsoleBrandsEnabled(boolean enabled) {
        return setConsoleStateAndNotify(AlertType.BRAND, enabled, false);
    }

    @Contract("_, _ -> param1")
    public boolean setConsoleBrandsEnabled(boolean enabled, boolean silent) {
        return setConsoleStateAndNotify(AlertType.BRAND, enabled, silent);
    }

    @Contract(pure = true)
    public boolean hasConsoleBrandsEnabled() {
        return AlertType.BRAND.console;
    }

    @Contract("_, _, _ -> param2")
    private boolean setConsoleStateAndNotify(@NotNull AlertType type, boolean enabled, boolean silent) {
        if (type.console != enabled && !silent) {
            String rawMessage = type.getToggleMessage(enabled);
            if (!rawMessage.isEmpty()) {
                platformServer.getConsoleSender().sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders((PlatformPlayer) null, rawMessage)));
            }
        }

        type.console = enabled;
        return enabled;
    }

    // All internal code, will replace later
    private void setPlayerStateAndNotify(@NotNull PlatformPlayer platformPlayer, boolean enabled, boolean silent, @NotNull AlertType type) {
        Objects.requireNonNull(platformPlayer, "platformPlayer cannot be null");
        boolean changed = enabled ? type.players.add(platformPlayer) : type.players.remove(platformPlayer);

        if (changed && !silent) {
            sendToggleMessage(platformPlayer, enabled, type);
        }
    }

    public boolean toggleBrands(@NotNull PlatformPlayer player) {
        return toggleBrands(player, false);
    }

    public boolean toggleBrands(@NotNull PlatformPlayer player, boolean silent) {
        return setBrandsEnabled(player, !hasBrandsEnabled(player), silent);
    }

    @Contract("_, _ -> param2")
    public boolean setBrandsEnabled(@NotNull PlatformPlayer player, boolean enabled) {
        return setBrandsEnabled(player, enabled, false);
    }

    @Contract("_, _, _ -> param2")
    public boolean setBrandsEnabled(@NotNull PlatformPlayer player, boolean enabled, boolean silent) {
        setPlayerStateAndNotify(player, enabled, silent, AlertType.BRAND);
        return enabled;
    }

    @Contract(pure = true)
    public boolean hasBrandsEnabled(@NotNull PlatformPlayer player) {
        return AlertType.BRAND.players.contains(player);
    }

    public boolean toggleVerbose(@NotNull PlatformPlayer player) {
        return toggleVerbose(player, false);
    }

    public boolean toggleVerbose(@NotNull PlatformPlayer player, boolean silent) {
        return setVerboseEnabled(player, !hasVerboseEnabled(player), silent);
    }

    @Contract("_, _ -> param2")
    public boolean setVerboseEnabled(@NotNull PlatformPlayer player, boolean enabled) {
        return setVerboseEnabled(player, enabled, false);
    }

    @Contract("_, _, _ -> param2")
    public boolean setVerboseEnabled(@NotNull PlatformPlayer player, boolean enabled, boolean silent) {
        if (enabled) setAlertsEnabled(player, true, silent);
        setPlayerStateAndNotify(player, enabled, silent, AlertType.VERBOSE);
        return enabled;
    }

    @Contract(pure = true)
    public boolean hasVerboseEnabled(@NotNull PlatformPlayer player) {
        return AlertType.VERBOSE.players.contains(player);
    }

    public boolean toggleAlerts(@NotNull PlatformPlayer player) {
        return toggleAlerts(player, false);
    }

    public boolean toggleAlerts(@NotNull PlatformPlayer player, boolean silent) {
        return setAlertsEnabled(player, !hasAlertsEnabled(player), silent);
    }

    @Contract("_, _ -> param2")
    public boolean setAlertsEnabled(@NotNull PlatformPlayer player, boolean enabled) {
        return setAlertsEnabled(player, enabled, false);
    }

    @Contract("_, _, _ -> param2")
    public boolean setAlertsEnabled(@NotNull PlatformPlayer player, boolean enabled, boolean silent) {
        setPlayerStateAndNotify(player, enabled, silent, AlertType.NORMAL);
        if (!enabled) setVerboseEnabled(player, false, silent);
        return enabled;
    }

    @Contract(pure = true)
    public boolean hasAlertsEnabled(@NotNull PlatformPlayer player) {
        return AlertType.NORMAL.players.contains(player);
    }

    /**
     * @param component the message to send to listeners
     * @param excluding the listeners to exclude, null means console
     * @return listeners this message was sent to, null means console
     */
    public Set<PlatformPlayer> sendBrand(Component component, @Nullable Set<@Nullable PlatformPlayer> excluding) {
        return AlertType.BRAND.send(component, excluding);
    }

    /**
     * @param component the message to send to listeners
     * @param excluding the listeners to exclude, null means console
     * @return listeners this message was sent to, null means console
     */
    public Set<PlatformPlayer> sendVerbose(Component component, @Nullable Set<@Nullable PlatformPlayer> excluding) {
        return AlertType.VERBOSE.send(component, excluding);
    }

    /**
     * @param component the message to send to listeners
     * @param excluding the listeners to exclude, null means console
     * @return listeners this message was sent to, null means console
     */
    public Set<PlatformPlayer> sendAlert(Component component, @Nullable Set<@Nullable PlatformPlayer> excluding) {
        return AlertType.NORMAL.send(component, excluding);
    }

    @Contract(pure = true)
    public boolean hasVerboseListeners() {
        return AlertType.VERBOSE.hasListeners();
    }

    @Contract(pure = true)
    public boolean hasAlertListeners() {
        return AlertType.NORMAL.hasListeners();
    }
}
