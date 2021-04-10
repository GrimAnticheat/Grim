package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityvelocity.WrappedPacketOutEntityVelocity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class PlayerVelocityPackets extends PacketListenerDynamic {
    public PlayerVelocityPackets() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();
        if (packetID == PacketType.Play.Server.ENTITY_VELOCITY) {
            WrappedPacketOutEntityVelocity velocity = new WrappedPacketOutEntityVelocity(event.getNMSPacket());
            Entity entity = velocity.getEntity();
            if (entity != null) {
                if (entity.equals(event.getPlayer())) {
                    double velX = velocity.getVelocityX();
                    double velY = velocity.getVelocityY();
                    double velZ = velocity.getVelocityZ();

                    Vector playerVelocity = new Vector(velX, velY, velZ);
                    //Bukkit.broadcastMessage("Adding " + playerVelocity);
                    GrimAC.playerGrimHashMap.get(event.getPlayer()).possibleKnockback.add(playerVelocity);

                    for (Vector vector : GrimAC.playerGrimHashMap.get(event.getPlayer()).possibleKnockback) {
                        //Bukkit.broadcastMessage(ChatColor.AQUA + "Current vectors " + vector);
                    }

                    event.getPlayer().sendMessage("You have taken velocity!");
                }
            }
        }
    }

    /*public void registerPackets() {
        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.ENTITY_VELOCITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

                // This means we are not setting the velocity of the player
                if (packet.getIntegers().read(0) != event.getPlayer().getEntityId()) {
                    return;
                }

                double x = packet.getIntegers().read(1) / 8000d;
                double y = packet.getIntegers().read(2) / 8000d;
                double z = packet.getIntegers().read(3) / 8000d;

                Vector playerVelocity = new Vector(x, y, z);
                Bukkit.broadcastMessage("Adding " + playerVelocity);
                player.possibleKnockback.add(playerVelocity);

                for (Vector vector : player.possibleKnockback) {
                    Bukkit.broadcastMessage(ChatColor.AQUA + "Current vectors " + vector);
                }
            }
        });
    }*/
}
