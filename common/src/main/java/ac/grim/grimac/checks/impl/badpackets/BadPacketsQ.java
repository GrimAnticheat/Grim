package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;

@CheckData(name = "BadPacketsQ", stableKey = "grim.badpackets.invalid_horse_jump", verboseVersion = 2, description = "Sent a horse jump packet with an invalid entity, action, or boost value")
public class BadPacketsQ extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of(2, "boost:zz", "action:enum", "entity:zz");

    public BadPacketsQ(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            // you are able to send negative jump boost, how and why!?
            if (Math.abs(wrapper.getJumpBoost()) > 100
                    || wrapper.getEntityId() != player.entityID
                    || wrapper.getAction() != Action.START_JUMPING_WITH_HORSE && wrapper.getJumpBoost() != 0) {
                int boost = wrapper.getJumpBoost();
                int action = VerboseCodecs.enumOrdinal(wrapper.getAction());
                int entity = wrapper.getEntityId();
                if (flagAndAlert(V.write(verbose()).zz(boost).vi(action).zz(entity)) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}