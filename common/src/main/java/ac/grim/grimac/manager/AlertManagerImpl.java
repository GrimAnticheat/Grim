package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
public class AlertManagerImpl implements AlertManager {
    private final Set<PlatformPlayer> enabledAlerts = new CopyOnWriteArraySet<>();
    private final Set<PlatformPlayer> enabledVerbose = new CopyOnWriteArraySet<>();
    private final Set<PlatformPlayer> enabledBrands = new CopyOnWriteArraySet<>();

    @Override
    public boolean hasAlertsEnabled(@NonNull GrimUser player) {
        Preconditions.checkArgument(player != null, "Player cannot be null");

        GrimPlayer grimPlayer = (GrimPlayer) player;
        Preconditions.checkArgument(grimPlayer.platformPlayer != null, "GrimPlayer has null platformPlayer");

        return hasAlertsEnabled(grimPlayer.platformPlayer);
    }

    public boolean hasAlertsEnabled(@NonNull PlatformPlayer platformPlayer) {
        Preconditions.checkArgument(platformPlayer != null, "PlatformPlayer cannot be null");
        return enabledAlerts.contains(platformPlayer);
    }

    @Override
    public boolean toggleAlerts(@NonNull GrimUser player) {
        Preconditions.checkArgument(player != null, "Player cannot be null");

        GrimPlayer grimPlayer = (GrimPlayer) player;
        Preconditions.checkArgument(grimPlayer.platformPlayer != null, "GrimPlayer has null platformPlayer");

        return toggleAlerts(grimPlayer.platformPlayer);
    }

    public boolean toggleAlerts(@NonNull PlatformPlayer platformPlayer) {
        Preconditions.checkArgument(platformPlayer != null, "PlatformPlayer cannot be null");

        if (!enabledAlerts.remove(platformPlayer)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-enabled", "%prefix% &fAlerts enabled");
            alertString = MessageUtil.replacePlaceholders(platformPlayer, alertString);
            platformPlayer.sendMessage(MessageUtil.miniMessage(alertString));
            enabledAlerts.add(platformPlayer);
            return true;
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-disabled", "%prefix% &fAlerts disabled");
            alertString = MessageUtil.replacePlaceholders(platformPlayer, alertString);
            platformPlayer.sendMessage(MessageUtil.miniMessage(alertString));
            return false; // Now disabled
        }
    }

    @Override
    public boolean hasVerboseEnabled(@NonNull GrimUser player) {
        Preconditions.checkArgument(player != null, "Player cannot be null");

        GrimPlayer grimPlayer = (GrimPlayer) player;
        Preconditions.checkArgument(grimPlayer.platformPlayer != null, "GrimPlayer has null platformPlayer");

        return hasVerboseEnabled(grimPlayer.platformPlayer);
    }

    public boolean hasVerboseEnabled(@NonNull PlatformPlayer platformPlayer) {
        Preconditions.checkArgument(platformPlayer != null, "PlatformPlayer cannot be null");
        return enabledVerbose.contains(platformPlayer);
    }

    @Override
    public boolean toggleVerbose(@NonNull GrimUser player) {
        Preconditions.checkArgument(player != null, "Player cannot be null");

        GrimPlayer grimPlayer = (GrimPlayer) player;
        Preconditions.checkArgument(grimPlayer.platformPlayer != null, "GrimPlayer has null platformPlayer");

        return toggleVerbose(grimPlayer.platformPlayer);
    }

    public boolean toggleVerbose(@NonNull PlatformPlayer platformPlayer) {
        Preconditions.checkArgument(platformPlayer != null, "PlatformPlayer cannot be null");

        if (!enabledVerbose.remove(platformPlayer)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-enabled", "%prefix% &fVerbose enabled");
            alertString = MessageUtil.replacePlaceholders(platformPlayer, alertString);
            platformPlayer.sendMessage(MessageUtil.miniMessage(alertString));
            enabledVerbose.add(platformPlayer);
            return true; // Now enabled
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-disabled", "%prefix% &fVerbose disabled");
            alertString = MessageUtil.replacePlaceholders(platformPlayer, alertString);
            platformPlayer.sendMessage(MessageUtil.miniMessage(alertString));
            return false; // Now disabled
        }
    }

    @Override
    public boolean hasBrandsEnabled(@NonNull GrimUser player) {
        Preconditions.checkArgument(player != null, "Player cannot be null");

        GrimPlayer grimPlayer = (GrimPlayer) player;
        // Some proxies break packet order in sending brand and send the data too early for performance
        // which causes us to iterate over all players with this method
        // before platformPlayer is intialized; while generally packet order is important to maintain
        // for compatibles sake lets just default to not sending alerts to these players
        if (grimPlayer.platformPlayer == null) return false;

        return hasBrandsEnabled(grimPlayer.platformPlayer);
    }

    public boolean hasBrandsEnabled(@NonNull PlatformPlayer platformPlayer) {
        Preconditions.checkArgument(platformPlayer != null, "PlatformPlayer cannot be null");
        return enabledBrands.contains(platformPlayer) && platformPlayer.hasPermission("grim.brand");
    }

    @Override
    public boolean toggleBrands(@NonNull GrimUser player) {
        Preconditions.checkArgument(player != null, "Player cannot be null");

        GrimPlayer grimPlayer = (GrimPlayer) player;
        Preconditions.checkArgument(grimPlayer.platformPlayer != null, "GrimPlayer has null platformPlayer");

        return toggleBrands(grimPlayer.platformPlayer);
    }

    public boolean toggleBrands(@NonNull PlatformPlayer platformPlayer) {
        Preconditions.checkArgument(platformPlayer != null, "PlatformPlayer cannot be null");

        if (!enabledBrands.remove(platformPlayer)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("brands-enabled", "%prefix% &fBrands enabled");
            alertString = MessageUtil.replacePlaceholders(platformPlayer, alertString);
            platformPlayer.sendMessage(MessageUtil.miniMessage(alertString));
            enabledBrands.add(platformPlayer);
            return true; // Now enabled
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("brands-disabled", "%prefix% &fBrands disabled");
            alertString = MessageUtil.replacePlaceholders(platformPlayer, alertString);
            platformPlayer.sendMessage(MessageUtil.miniMessage(alertString));
            return false; // Now disabled
        }
    }

    public void handlePlayerQuit(@NonNull GrimUser player) {
        Preconditions.checkArgument(player != null, "Player cannot be null");

        GrimPlayer grimPlayer = (GrimPlayer) player;
        if (grimPlayer.platformPlayer != null) {
            handlePlayerQuit(grimPlayer.platformPlayer);
        }
    }

    public void handlePlayerQuit(@NonNull PlatformPlayer platformPlayer) {
        Preconditions.checkArgument(platformPlayer != null, "PlatformPlayer cannot be null");
        enabledAlerts.remove(platformPlayer);
        enabledVerbose.remove(platformPlayer);
        enabledBrands.remove(platformPlayer);
    }
}
