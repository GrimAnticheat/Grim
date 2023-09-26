package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsS", experimental = true)
public class BadPacketsS extends Check implements PacketCheck {

    public BadPacketsS(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity.InteractAction i = new WrapperPlayClientInteractEntity(event).getAction();
            if (i == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                if (player.packetStateData.slowedByUsingItem) {
                    flagAndAlert();
                    event.setCancelled(true);
                }
            }
        }
    }
}
