package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.entity.EntityUtils;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PacketPlayerAttack extends PacketListenerAbstract {
    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.USE_ENTITY) {
            WrappedPacketInUseEntity action = new WrappedPacketInUseEntity(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            if (player == null) return;

            if (action.getAction() == WrappedPacketInUseEntity.EntityUseAction.ATTACK) {
                Entity attackedEntity = EntityUtils.getEntity(action);
                if (attackedEntity != null && (!(attackedEntity instanceof LivingEntity) || attackedEntity instanceof Player)) {
                    Bukkit.broadcastMessage("Player has been slowed!");
                }
            }
        }
    }
}
