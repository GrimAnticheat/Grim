package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "PacketOrderB", description = "Did not swing for attack")
public class PacketOrderB extends Check implements PacketCheck {
    // 1.9 packet order: INTERACT -> ANIMATION
    // 1.8 packet order: ANIMATION -> INTERACT
    // I personally think 1.8 made much more sense. You swing and THEN you hit!
    private final boolean is1_9 = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);

    private boolean sentAnimationSinceLastAttack = player.getClientVersion().isNewerThan(ClientVersion.V_1_8);
    private boolean sentAttack, sentAnimation, sentSlotSwitch;

    public PacketOrderB(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            sentAnimationSinceLastAttack = sentAnimation = true;
            sentAttack = sentSlotSwitch = false;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                sentAttack = true;

                if (is1_9 ? !sentAnimationSinceLastAttack : !sentAnimation) {
                    sentAttack = false; // don't flag twice
                    if (flagAndAlert("pre-attack") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }

                sentAnimationSinceLastAttack = sentAnimation = sentSlotSwitch = false;
                return;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE && !is1_9 && !sentSlotSwitch) {
            sentSlotSwitch = true;
            return; // do not set sentAnimation to false
        }

        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE) {
            if (sentAttack && is1_9) {
                flagAndAlert("post-attack");
            }

            sentAttack = sentAnimation = sentSlotSwitch = false;
        }
    }
}
