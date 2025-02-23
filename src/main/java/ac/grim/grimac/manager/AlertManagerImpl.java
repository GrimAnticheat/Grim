package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
public class AlertManagerImpl implements AlertManager {
    private final Set<GrimUser> enabledAlerts = new CopyOnWriteArraySet<>(new HashSet<>());
    private final Set<GrimUser> enabledVerbose = new CopyOnWriteArraySet<>(new HashSet<>());
    private final Set<GrimUser> enabledBrands = new CopyOnWriteArraySet<>(new HashSet<>());

    @Override
    public boolean hasAlertsEnabled(GrimUser player) {
        return enabledAlerts.contains(player);
    }

    @Override
    public void toggleAlerts(GrimUser player) {
        if (!enabledAlerts.remove(player)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-enabled", "%prefix% &fAlerts enabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            ((GrimPlayer) player).sendMessage(MessageUtil.miniMessage(alertString));
            enabledAlerts.add(player);
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-disabled", "%prefix% &fAlerts disabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            ((GrimPlayer) player).sendMessage(MessageUtil.miniMessage(alertString));
        }
    }

    @Override
    public boolean hasVerboseEnabled(GrimUser player) {
        return enabledVerbose.contains(player);
    }

    public boolean hasBrandsEnabled(GrimUser player) {
        return enabledBrands.contains(player) && player.hasPermission("grim.brand");
    }

    @Override
    public void toggleVerbose(GrimUser player) {
        if (!enabledVerbose.remove(player)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-enabled", "%prefix% &fVerbose enabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            ((GrimPlayer) player).sendMessage(MessageUtil.miniMessage(alertString));
            enabledVerbose.add(player);
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-disabled", "%prefix% &fVerbose disabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            ((GrimPlayer) player).sendMessage(MessageUtil.miniMessage(alertString));
        }
    }

    public void toggleBrands(GrimUser player) {
        if (!enabledBrands.remove(player)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("brands-enabled", "%prefix% &fBrands enabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            ((GrimPlayer) player).sendMessage(MessageUtil.miniMessage(alertString));
            enabledBrands.add(player);
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("brands-disabled", "%prefix% &fBrands disabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            ((GrimPlayer) player).sendMessage(MessageUtil.miniMessage(alertString));
        }
    }

    public void handlePlayerQuit(GrimUser player) {
        enabledAlerts.remove(player);
        enabledVerbose.remove(player);
        enabledBrands.remove(player);
    }
}
