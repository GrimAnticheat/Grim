package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;

@CheckData(name = "PacketOrderD", experimental = true)
public class PacketOrderD extends Check implements PacketCheck {
    public PacketOrderD(final GrimPlayer player) {
        super(player);
    }

    private boolean sentMainhand;
    private int requiredEntity;
    private boolean requiredSneaking;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            InteractAction action = packet.getAction();
            if (action != InteractAction.ATTACK) {
                final boolean sneaking = packet.isSneaking().orElse(false);
                final int entity = packet.getEntityId();

                if (packet.getHand() == InteractionHand.OFF_HAND) {
                    if (action == InteractAction.INTERACT) {
                        if (!sentMainhand) {
                            if (flagAndAlert("Skipped Mainhand") && shouldModifyPackets()) {
                                event.setCancelled(true);
                                player.onPacketCancel();
                            }
                        }
                        sentMainhand = false;
                    } else if (sneaking != requiredSneaking || entity != requiredEntity) {
                        String verbose = "requiredEntity=" + requiredEntity + ", entity=" + entity
                                + ", requiredSneaking=" + requiredSneaking + ", sneaking=" + sneaking;
                        if (flagAndAlert(verbose) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }
                } else {
                    requiredEntity = entity;
                    requiredSneaking = sneaking;
                    sentMainhand = true;
                }
            }
        }

        if (isTickPacket(event.getPacketType())) {
            sentMainhand = false;
        }
    }
}
