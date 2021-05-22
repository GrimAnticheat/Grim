package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.entityaction.WrappedPacketInEntityAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class PacketEntityAction extends PacketListenerDynamic {
    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.ENTITY_ACTION) {
            WrappedPacketInEntityAction action = new WrappedPacketInEntityAction(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            switch (action.getAction()) {
                case START_SPRINTING:
                    player.isPacketSprinting = true;
                    player.isPacketSprintingChange = true;
                    Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "START SPRINTING");
                    break;
                case STOP_SPRINTING:
                    player.isPacketSprinting = false;
                    player.isPacketSprintingChange = true;
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "STOP SPRINTING");
                    break;
                case START_SNEAKING:
                    player.isPacketSneaking = true;
                    player.isPacketSneakingChange = true;
                    break;
                case STOP_SNEAKING:
                    player.isPacketSneaking = false;
                    player.isPacketSneakingChange = true;
                    break;
            }
        }
    }
}
