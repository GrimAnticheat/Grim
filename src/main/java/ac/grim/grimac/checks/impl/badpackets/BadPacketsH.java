package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsH", description = "Ensures users swing their arm when breaking blocks/attacking")
public class BadPacketsH extends Check implements PacketCheck {
    private boolean needsSwing;
    private PacketTypeCommon swingPacketType;

    public BadPacketsH(final GrimPlayer player) {
        super(player);
    }

    private void setNeedsSwing(PacketTypeCommon packetType) {
        this.needsSwing = true;
        this.swingPacketType = packetType;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // This check hasn't been tested on versions older before 1.17
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_17)) return;


        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);
            if (action.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                setNeedsSwing(event.getPacketType());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging action = new WrapperPlayClientPlayerDigging(event);
            if (action.getAction() == DiggingAction.START_DIGGING) {
                setNeedsSwing(event.getPacketType());
            }
        } else if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            this.needsSwing = false;
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // If we receive a flying packet from the client before the arm swing,
            // we can be pretty sure something ain't right.
            if (needsSwing) {
                needsSwing = false;
                flag(true, false, "packetType=" + swingPacketType.getName());
            }
        }
    }
}
