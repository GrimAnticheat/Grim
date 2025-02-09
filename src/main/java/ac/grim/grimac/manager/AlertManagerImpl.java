package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
public class AlertManagerImpl implements AlertManager {
    private final Set<Player> enabledAlerts = new CopyOnWriteArraySet<>(new HashSet<>());
    private final Set<Player> enabledVerbose = new CopyOnWriteArraySet<>(new HashSet<>());
    private final Set<Player> enabledBrands = new CopyOnWriteArraySet<>(new HashSet<>());

    @Override
    public boolean hasAlertsEnabled(Player player) {
        return enabledAlerts.contains(player);
    }

    @Override
    public void toggleAlerts(Player player, boolean silent) {
        if (!enabledAlerts.remove(player)) {
            if (!silent) {
                String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-enabled", "%prefix% &fAlerts enabled");
                alertString = MessageUtil.replacePlaceholders(player, alertString);
                MessageUtil.sendMessage(player, MessageUtil.miniMessage(alertString));
            }
            enabledAlerts.add(player);
        } else {
            if (silent) return;
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-disabled", "%prefix% &fAlerts disabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            MessageUtil.sendMessage(player, MessageUtil.miniMessage(alertString));
        }
    }

    @Override
    public boolean hasVerboseEnabled(Player player) {
        return enabledVerbose.contains(player);
    }

    public boolean hasBrandsEnabled(Player player) {
        return enabledBrands.contains(player) && player.hasPermission("grim.brand");
    }

    @Override
    public void toggleVerbose(Player player, boolean silent) {
        if (!enabledVerbose.remove(player)) {
            if (!silent) {
                String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-enabled", "%prefix% &fVerbose enabled");
                alertString = MessageUtil.replacePlaceholders(player, alertString);
                MessageUtil.sendMessage(player, MessageUtil.miniMessage(alertString));
            }
            enabledVerbose.add(player);
        } else {
            if (silent) return;
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-disabled", "%prefix% &fVerbose disabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            MessageUtil.sendMessage(player, MessageUtil.miniMessage(alertString));
        }
    }

    public void toggleBrands(Player player, boolean silent) {
        if (!enabledBrands.remove(player)) {
            if (!silent) {
                String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("brands-enabled", "%prefix% &fBrands enabled");
                alertString = MessageUtil.replacePlaceholders(player, alertString);
                MessageUtil.sendMessage(player, MessageUtil.miniMessage(alertString));
            }
            enabledBrands.add(player);
        } else {
            if (silent) return;
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("brands-disabled", "%prefix% &fBrands disabled");
            alertString = MessageUtil.replacePlaceholders(player, alertString);
            MessageUtil.sendMessage(player, MessageUtil.miniMessage(alertString));
        }
    }

    public void handlePlayerQuit(Player player) {
        enabledAlerts.remove(player);
        enabledVerbose.remove(player);
        enabledBrands.remove(player);
    }
}
