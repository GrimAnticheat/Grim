package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "PacketOrderB", description = "Did not swing for attack")
public class PacketOrderB extends Check implements PacketCheck {
    // 1.9 packet order: INTERACT -> ANIMATION
    // 1.8 packet order: ANIMATION -> INTERACT
    // I personally think 1.8 made much more sense. You swing and THEN you hit!
    private final boolean is1_9 = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);

    // There is a "bug" in ViaRewind
    // 1.8 packet order: ANIMATION -> INTERACT
    // 1.9 packet order: INTERACT -> ANIMATION
    // ViaRewind, on 1.9+ servers, delays a 1.8 client's ANIMATION to be after INTERACT (but before flying).
    // Which means we see 1.9 packet order for 1.8 clients
    // Due to ViaRewind also delaying the swings, we then see packet order above 20CPS like:
    // INTERACT -> INTERACT -> ANIMATION -> ANIMATION
    // I will simply disable this check for 1.8- clients on 1.9+ servers as I can't be bothered to find a way around this.
    // Stop supporting such old clients on modern servers!
    private final boolean exempt = player.getClientVersion().isOlderThan(ClientVersion.V_1_9)
            && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9);

    private boolean sentAnimationSinceLastAttack = player.getClientVersion().isNewerThan(ClientVersion.V_1_8);
    private boolean sentAttack, sentAnimation, sentSlotSwitch;

    public PacketOrderB(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (exempt) return;

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

        if (!isAsync(event.getPacketType())) {
            if (sentAttack && is1_9) {
                flagAndAlert("post-attack");
            }

            sentAttack = sentAnimation = sentSlotSwitch = false;
        }
    }
}
