package ac.grim.grimac.events.packets;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;

public class PacketMountVehicle extends PacketListenerDynamic {

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.MOUNT) {
            try {
                // PacketPlayOutMount
                Object mountVehicle = event.getNMSPacket().getRawNMSPacket();

                Field idField = mountVehicle.getClass().getDeclaredField("a");
                Field inVehicle = mountVehicle.getClass().getDeclaredField("b");

                idField.setAccessible(true);
                inVehicle.setAccessible(true);

                int vehicle = idField.getInt(mountVehicle);
                int[] mountedID = (int[]) inVehicle.get(mountVehicle);

                Bukkit.broadcastMessage("Vehicle " + vehicle + " mountedID " + mountedID);

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
