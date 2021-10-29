package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.setcooldown.WrappedPacketOutSetCooldown;

public class PacketPlayerCooldown extends PacketListenerAbstract {

    public PacketPlayerCooldown() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.SET_COOLDOWN) {
            WrappedPacketOutSetCooldown cooldown = new WrappedPacketOutSetCooldown(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            int lastTransactionSent = player.lastTransactionSent.get();

            if (cooldown.getCooldownTicks() == 0) { // for removing the cooldown
                player.latencyUtils.addRealTimeTask(lastTransactionSent + 1, () -> {
                    player.checkManager.getCompensatedCooldown().removeCooldown(cooldown.getItemStack().getType());
                });
            } else { // Not for removing the cooldown
                player.latencyUtils.addRealTimeTask(lastTransactionSent, () -> {
                    player.checkManager.getCompensatedCooldown().addCooldown(cooldown.getItemStack().getType(), cooldown.getCooldownTicks(), lastTransactionSent);
                });
            }
        }
    }
}
