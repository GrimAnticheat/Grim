package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsM", experimental = true)
public class BadPacketsM extends Check implements PacketCheck {
    public BadPacketsM(final GrimPlayer player) {
        super(player);
    }

    private boolean sentInteractAt = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            switch (new WrapperPlayClientInteractEntity(event).getAction()) {
                // INTERACT_AT then INTERACT
                case INTERACT:
                    if (!sentInteractAt) {
                        if (flagAndAlert("Missed Interact-At") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }
                    sentInteractAt = false;
                    break;
                case INTERACT_AT:
                    if (sentInteractAt) {
                        if (flagAndAlert("Missed Interact") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }
                    sentInteractAt = true;
                    break;
            }
        }
    }
}
