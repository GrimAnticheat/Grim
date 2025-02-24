package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsT")
public class BadPacketsT extends Check implements PacketCheck {
    public BadPacketsT(final GrimPlayer player) {
        super(player);
    }

    // 1.7 and 1.8 seem to have different hitbox "expansion" values than 1.9+
    // https://github.com/GrimAnticheat/Grim/pull/1274#issuecomment-1872458702
    // https://github.com/GrimAnticheat/Grim/pull/1274#issuecomment-1872533497
    private final boolean hasLegacyExpansion = player.getClientVersion().isOlderThan(ClientVersion.V_1_9);
    private final double maxHorizontalDisplacement = 0.3001 + (hasLegacyExpansion ? 0.1 : 0);
    private final double minVerticalDisplacement = -0.0001 - (hasLegacyExpansion ? 0.1 : 0);
    private final double maxVerticalDisplacement = 1.8001 + (hasLegacyExpansion ? 0.1 : 0);

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.INTERACT_ENTITY)) {
            final WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            // Only INTERACT_AT actually has an interaction vector
            wrapper.getTarget().ifPresent(targetVector -> {
                final PacketEntity packetEntity = player.compensatedEntities.getEntity(wrapper.getEntityId());
                // Don't continue if the compensated entity hasn't been resolved
                if (packetEntity == null) {
                    return;
                }

                // Make sure our target entity is actually a player (Player NPCs work too)
                if (!EntityTypes.PLAYER.equals(packetEntity.getType())) {
                    // We can't check for any entity that is not a player
                    return;
                }

                // Perform the interaction vector check
                // TODO:
                //  27/12/2023 - Dynamic values for more than just one entity type?
                //  28/12/2023 - Player-only is fine
                //  30/12/2023 - Expansions differ in 1.9+
                final float scale = (float) packetEntity.getAttributeValue(Attributes.SCALE);
                if (targetVector.y > (minVerticalDisplacement * scale) && targetVector.y < (maxVerticalDisplacement * scale)
                        && Math.abs(targetVector.x) < (maxHorizontalDisplacement * scale)
                        && Math.abs(targetVector.z) < (maxHorizontalDisplacement * scale)) {
                    return;
                }

                // Log the vector
                final String verbose = String.format("%.5f/%.5f/%.5f",
                        targetVector.x, targetVector.y, targetVector.z);
                // We could pretty much ban the player at this point
                flagAndAlert(verbose);
            });
        }
    }
}
