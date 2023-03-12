package ac.grim.grimac.checks.impl.respawn;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.GameRule.DO_IMMEDIATE_RESPAWN;

@CheckData(name = "InstantRespawn")
public class InstantRespawn extends Check implements PacketCheck {
    private final JavaPlugin plugin = JavaPlugin.getPlugin(GrimAC.class);
    private long lastRespawnPacket;

    public InstantRespawn(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (player.bukkitPlayer == null) return;
        if (Boolean.TRUE.equals(player.bukkitPlayer.getWorld().getGameRuleValue(DO_IMMEDIATE_RESPAWN))) return;
        if (!event.getPacketType().equals(PacketType.Play.Server.DEATH_COMBAT_EVENT)) return;
        lastRespawnPacket = System.currentTimeMillis();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (lastRespawnPacket == 0 || player.bukkitPlayer == null) return;
        if (Boolean.TRUE.equals(player.bukkitPlayer.getWorld().getGameRuleValue(DO_IMMEDIATE_RESPAWN))) return;
        if (!event.getPacketType().equals(PacketType.Play.Client.CLIENT_STATUS)) return;
        long diff = System.currentTimeMillis() - lastRespawnPacket;
        if (diff < 900) Bukkit.getScheduler().runTask(plugin, () ->
                player.bukkitPlayer.kickPlayer("§cSomething went wrong"));
        lastRespawnPacket = 0;
    }
}
