package ac.grim.grimac;
;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

//This is used for grim's external API. It has its own class just for organization.
public class GrimExternalAPI implements GrimAbstractAPI, Initable {

    private final GrimAPI api;

    public GrimExternalAPI(GrimAPI api) {
        this.api = api;
    }

    @Nullable
    @Override
    public GrimUser getGrimUser(Player player) {
        return api.getPlayerDataManager().getPlayer(player);
    }

    @Override
    public void setServerName(String name) {
        variableReplacements.put("%server%", user -> name);
    }

    @Getter
    private final Map<String, Function<GrimUser, String>> variableReplacements = new ConcurrentHashMap<>();

    public String replaceVariables(GrimUser user, String content, boolean colors) {
        if (colors) content = ChatColor.translateAlternateColorCodes('&', content);
        for (Map.Entry<String, Function<GrimUser, String>> entry : variableReplacements.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue().apply(user));
        }
        return content;
    }

    @Override
    public void registerVariable(String string, Function<GrimUser, String> replacement) {
        variableReplacements.put(string, replacement);
    }

    @Override
    public void reload() {
        GrimAPI.INSTANCE.getConfigManager().reload();
        GrimAPI.INSTANCE.getPunishmentManager().reload();
        //Reload checks for all players
        for (GrimPlayer grimPlayer : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            ChannelHelper.runInEventLoop(grimPlayer.user.getChannel(), () -> {
                grimPlayer.onReload();
                grimPlayer.updatePermissions();
                grimPlayer.getCheckManager().reload();
                grimPlayer.punishmentManager.reloadCachedGroups();
            });
        }
        //Restart
        GrimAPI.INSTANCE.getDiscordManager().start();
        GrimAPI.INSTANCE.getSpectateManager().start();
        GrimAPI.INSTANCE.getExternalAPI().start();
    }

    @Override
    public void start() {
        variableReplacements.put("%last_location%", user -> {
            Player player = Bukkit.getPlayer(user.getUniqueId());
            return player != null ? locationToString(player.getLocation()) : "offline";
        });
        variableReplacements.put("%last_command%", user -> user instanceof GrimPlayer ?
                ((GrimPlayer) user).lastSendedCommand : "");
        variableReplacements.put("%last_action%", user -> user instanceof GrimPlayer ?
                ((GrimPlayer) user).lastInteractAction : "");
        variableReplacements.put("%player_ip%", user -> user instanceof GrimPlayer ?
                ((GrimPlayer) user).getIpAddress() : "");
        variableReplacements.put("%player%", GrimUser::getName);
        variableReplacements.put("%uuid%", user -> user.getUniqueId().toString());
        variableReplacements.put("%ping%", user -> String.valueOf(user.getTransactionPing()));
        variableReplacements.put("%brand%", GrimUser::getBrand);
        variableReplacements.put("%h_sensitivity%", user -> String.valueOf((int) Math.round(user.getHorizontalSensitivity() * 200)));
        variableReplacements.put("%v_sensitivity%", user -> String.valueOf((int) Math.round(user.getVerticalSensitivity() * 200)));
        variableReplacements.put("%fast_math%", user -> String.valueOf(!user.isVanillaMath()));
        variableReplacements.put("%tps%", user -> String.format("%.2f", SpigotReflectionUtil.getTPS()));
        variableReplacements.put("%version%", GrimUser::getVersionName);
        variableReplacements.put("%prefix%", user -> ChatColor.translateAlternateColorCodes('&',
                GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("prefix", "&7[&bEternalAC&7] &8")));
    }

    private String locationToString(Location location) {
        String yaw = "|" + location.getYaw() + "|" + location.getPitch();
        return location.getWorld().getName() + "|" + location.getBlockX() + "|" + location.getBlockY() + "|" + location.getBlockZ() + yaw;
    }
}
