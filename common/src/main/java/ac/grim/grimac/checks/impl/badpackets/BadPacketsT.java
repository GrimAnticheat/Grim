package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.velocity.VectorPrecisionConverter;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsT", stableKey = "grim.badpackets.invalid_interact_vector", description = "Sent an entity interaction vector outside the target player's hitbox")
public class BadPacketsT extends Check implements PreViaPacketReceiveListener {
    private static final Verbose V = Verbose.of("{f64:%.5f}/{f64:%.5f}/{f64:%.5f}");

    // pre-1.9 expands hit boxes by 0.1 on all sides; this is not lenience, it is vanilla.
    // TODO: do we even need an epsilon?
    private final double expansion = (player.getClientVersion().isOlderThan(ClientVersion.V_1_9) ? 0.1f : 0);
    private final boolean stupid = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_1);

    public BadPacketsT(final GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8);
    }

    @Override
    public void onPreViaPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.INTERACT_ENTITY)) {
            final WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            // Only INTERACT_AT actually has an interaction vector
            if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) return;
            Vector3d targetVector = wrapper.getLocation();
            if (targetVector == null) return; // shouldn't ever happen, but whatever

            if (!Double.isFinite(targetVector.x) || !Double.isFinite(targetVector.y) || !Double.isFinite(targetVector.z)) {
                flag(V.write(verbose()).f64(targetVector.x).f64(targetVector.y).f64(targetVector.z));
                return;
            }

            final PacketEntity entity = player.compensatedEntities.getEntity(wrapper.getEntityId());
            if (entity == null) return;

            if (stupid) {
                targetVector = VectorPrecisionConverter.lpToLegacy(targetVector);
            }

            final float scale = (float) entity.getAttributeValue(Attributes.SCALE);
            final float height = BoundingBoxSize.getHeight(player, entity) * scale;
            final float width = BoundingBoxSize.getWidth(player, entity) * scale;
            final double minVertical = -expansion; // scale is irrelevant
            final double maxVertical = height + expansion;
            final double maxHorizontal = (width / 2f) + expansion;

            player.sendMessage(targetVector.x + "/" + targetVector.y + "/" + targetVector.z
                    + ", minVertical=" + minVertical + ", maxVertical=" + maxVertical + ", maxHorizontal=" + maxHorizontal);

            if (targetVector.y >= minVertical && targetVector.y <= maxVertical
                    && Math.abs(targetVector.x) <= maxHorizontal
                    && Math.abs(targetVector.z) <= maxHorizontal) {
                return;
            }

            // Log the vector
            // We could pretty much ban the player at this point
            flag(V.write(verbose()).f64(targetVector.x).f64(targetVector.y).f64(targetVector.z));
        }
    }
}
