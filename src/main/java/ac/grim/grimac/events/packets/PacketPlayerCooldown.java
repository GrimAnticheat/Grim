package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCooldown;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;

public class PacketPlayerCooldown extends PacketListenerAbstract {

    public PacketPlayerCooldown() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SET_COOLDOWN) {
            WrapperPlayServerSetCooldown cooldown = new WrapperPlayServerSetCooldown(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            int lastTransactionSent = player.lastTransactionSent.get();

            if (cooldown.getCooldownTicks() == 0) { // for removing the cooldown
                player.latencyUtils.addRealTimeTask(lastTransactionSent + 1, () -> {
                    player.checkManager.getCompensatedCooldown().removeCooldown(Material.getMaterial(cooldown.getItem().getIdentifier().getKey().toUpperCase(Locale.ROOT)));
                });
            } else { // Not for removing the cooldown
                player.latencyUtils.addRealTimeTask(lastTransactionSent, () -> {
                    player.checkManager.getCompensatedCooldown().addCooldown(Material.getMaterial(cooldown.getItem().getIdentifier().getKey().toUpperCase(Locale.ROOT)),
                            cooldown.getCooldownTicks(), lastTransactionSent);
                });
            }
        }
    }
}
