package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.EntityAction;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "BadPacketsF")
public class BadPacketsF extends PacketCheck {
    public boolean lastSprinting;
    boolean thanksMojang; // Support 1.14+ clients starting on either true or false sprinting, we don't know

    public BadPacketsF(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

            if (packet.getAction() == EntityAction.START_SPRINTING) {
                if (lastSprinting) {
                    if (!thanksMojang) {
                        thanksMojang = true;
                        return;
                    }
                    flagAndAlert();
                }

                lastSprinting = true;
            } else if (packet.getAction() == EntityAction.STOP_SPRINTING) {
                if (!lastSprinting) {
                    if (!thanksMojang) {
                        thanksMojang = true;
                        return;
                    }
                    flagAndAlert();
                }

                lastSprinting = false;
            }
        }
    }
}
