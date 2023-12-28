package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsT", experimental=true)
public class BadPacketsT extends Check implements PacketCheck {
    public BadPacketsT(final GrimPlayer player) {
        super(player);
    }

    // Player hitbox sizes
    // min X/Z: -0.4, max X/Z: 0.4
    private static final double MAX_XZ = 0.4001;
    // min Y: -0.1, max Y: 1.9
    private static final double MIN_Y = -0.1001;
    private static final double MAX_Y = 1.9001;

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
                if (!EntityTypes.PLAYER.equals(packetEntity.type)) {
                    // We can't check for any entity that is not a player
                    return;
                }
                // Perform the interaction vector check
                // TODO:
                //  27/12/2023 - Dynamic values for more than just one entity type?
                //  28/12/2023 - Player-only is fine
                if (targetVector.y > MIN_Y && targetVector.y < MAX_Y
                        && Math.abs(targetVector.x) < MAX_XZ
                        && Math.abs(targetVector.z) < MAX_XZ) {
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
