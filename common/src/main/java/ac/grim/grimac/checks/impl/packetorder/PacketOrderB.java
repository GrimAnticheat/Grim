package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(name = "PacketOrderB", stableKey = "grim.packetorder.noswing", description = "Did not swing for attack")
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
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION
            && new WrapperPlayClientAnimation(event).getHand() == InteractionHand.MAIN_HAND) {
            sentAnimationSinceLastAttack = sentAnimation = true;
            sentAttack = sentSlotSwitch = false;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                onAttack(event);
                return;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            onAttack(event);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.STAB) {
                onAttack(event);
                return;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE && !is1_9 && !sentSlotSwitch) {
            sentSlotSwitch = true;
            return; // do not set sentAnimation to false
        }

        if (!isAsync(event.getPacketType())) {
            if (sentAttack && is1_9) {
                flagAndAlert("post-attack");
            }

            sentAttack = sentAnimation = sentSlotSwitch = false;
        }
    }

    private void onAttack(PacketReceiveEvent event) {
        if (player.gamemode == GameMode.SPECTATOR && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11)) return;

        sentAttack = true;

        if (is1_9 ? !sentAnimationSinceLastAttack : !sentAnimation) {
            sentAttack = false; // don't flag twice
            if (flagAndAlert("pre-attack") && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        sentAnimationSinceLastAttack = sentAnimation = sentSlotSwitch = false;
    }
}
