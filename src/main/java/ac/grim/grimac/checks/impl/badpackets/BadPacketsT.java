package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsT", experimental=true)
public class BadPacketsT extends Check implements PacketCheck {
    public BadPacketsT(final GrimPlayer player) {
        super(player);
    }

    // 1.7 and 1.8 seem to have different hitbox "expansion" values than 1.9+
    // https://github.com/GrimAnticheat/Grim/pull/1274#issuecomment-1872458702
    // https://github.com/GrimAnticheat/Grim/pull/1274#issuecomment-1872533497
    private final double legacyExpansion = player.getClientVersion().isOlderThan(ClientVersion.V_1_9) ? 0.1 : 0;
    private final double maxXZ = 0.3001 + legacyExpansion;
    private final double minY = -0.0001 - legacyExpansion;
    private final double maxY = 1.8001 + legacyExpansion;

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!event.getPacketType().equals(PacketType.Play.Client.INTERACT_ENTITY)) return;

        final WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        // Only INTERACT_AT actually has an interaction vector
        if (!wrapper.getTarget().isPresent()) return;

        final Vector3f targetVector = wrapper.getTarget().get();

        final PacketEntity packetEntity = player.compensatedEntities.getEntity(wrapper.getEntityId());

        // Don't continue if the compensated entity hasn't been resolved
        if (packetEntity == null) return;

        // Make sure our target entity is actually a player (Player NPCs work too)
        // We can't check for any entity that is not a player
        if (!EntityTypes.PLAYER.equals(packetEntity.type)) return;

        // Perform the interaction vector check
        // TODO:
        //  27/12/2023 - Dynamic values for more than just one entity type?
        //  28/12/2023 - Player-only is fine
        //  30/12/2023 - Expansions differ in 1.9+
        if (targetVector.y > minY && targetVector.y < maxY
                && Math.abs(targetVector.x) < maxXZ
                && Math.abs(targetVector.z) < maxXZ) {
            return;
        }

        // Log the vector
        final String verbose = String.format("%.5f/%.5f/%.5f",
                targetVector.x, targetVector.y, targetVector.z);

        // We could pretty much ban the player at this point
        flagAndAlert(verbose);
    }
}
