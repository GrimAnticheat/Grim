package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.velocity.VectorPrecisionConverter;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityArrow;
import ac.grim.grimac.utils.data.tags.SyncedTags;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.color.Color;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleDustData;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.MathUtil;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "InvalidInteractCursor", stableKey = "grim.badpackets.invalid_interact_vector", description = "Sent an entity interaction vector outside the target entity's hitbox")
public class InvalidInteractCursor extends Check implements PreViaPacketReceiveListener {
    private static final Verbose V = Verbose.of("{f64:%.5f}/{f64:%.5f}/{f64:%.5f}");

    public InvalidInteractCursor(final GrimPlayer player) {
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
            Vector3d target = wrapper.getLocation();
            if (target == null) return; // shouldn't ever happen, but whatever

            if (!Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) {
                flag(V.write(verbose()).f64(target.x).f64(target.y).f64(target.z));
                return;
            }

            final PacketEntity entity = player.compensatedEntities.getEntity(wrapper.getEntityId());
            if (entity == null) return;

            // the entity doesn't exist on the player's client version
            if (entity.getType().getId(player.getClientVersion()) == -1) return;

            // On 1.8, I managed to get a 13.30429/3.68953/-2.04708 hit on a large fireball.
            // The reason for this is a mystery, but it doesn't seem to happen on any other version.
            if (entity.getType() == EntityTypes.FIREBALL && player.getClientVersion() == ClientVersion.V_1_8) return;

            final float scale = (float) entity.getAttributeValue(Attributes.SCALE);
            final float height = BoundingBoxSize.getHeight(player, entity) * scale;
            final float width = BoundingBoxSize.getWidth(player, entity) * scale;
            final double expansion = getExpansion(entity);

            double minVertical = -expansion; // scale is irrelevant
            double maxVertical = height + expansion;
            double maxHorizontal = width / 2f + expansion;

            // in 26.1, Mojang started using LpVectors for the target location...
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_1)) {
                // calculate the maximum value they could've sent for each axis,
                // given the precision at the point where they clicked
                // put at the edge of the hitbox on that axis
                maxHorizontal = withPrecision(maxHorizontal, target);
                maxVertical = withPrecision(maxVertical, target);

                // add an epsilon for floating point errors (?) from using LpVectors
                // TODO: can this be stricter?
                maxHorizontal += 1e-15;
                maxVertical += 1e-15;
            }

            if (target.y >= minVertical && target.y <= maxVertical
                    && Math.abs(target.x) <= maxHorizontal
                    && Math.abs(target.z) <= maxHorizontal) {
                return;
            }

            // Log the vector
            // We could pretty much ban the player at this point
            flag(V.write(verbose()).f64(target.x).f64(target.y).f64(target.z));
        }
    }

    public static double withPrecision(double value, @NotNull Vector3d lp) {
        // precision is determined by the value in the vector furthest from zero
        double max = MathUtil.absMax(lp.x, MathUtil.absMax(lp.y, lp.z));
        return VectorPrecisionConverter.legacyToLp(new Vector3d(value, max, 0)).x;
    }

    // MCP: Entity#getCollisionBorderSize()
    // Mojmap: Entity#getPickRadius()
    // should be accurate for 1.7 to 26.3
    private double getExpansion(@NotNull PacketEntity entity) {
        EntityType type = entity.getType();

        if (type == EntityTypes.WIND_CHARGE && player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5)) {
            return 1f;
        }

        if (type == EntityTypes.SHULKER_BULLET && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
            return 1f;
        }

        if (isProjectile(type) && entity.hasTag(SyncedTags.REDIRECTABLE_PROJECTILE)
                && !(entity instanceof PacketEntityArrow arrow && arrow.inGround)) {
            return 1f;
        }

        // technically also applies to versions above 1.8, but it doesn't matter because the expansion value below is 0.
        if (type == EntityTypes.ITEM_FRAME && player.getClientVersion() == ClientVersion.V_1_8) {
            return 0f;
        }

        return player.getClientVersion().isOlderThan(ClientVersion.V_1_9) ? 0.1f : 0.0f;
    }

    private static boolean isProjectile(EntityType type) {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.PROJECTILE_ABSTRACT)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_ARROW)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_FIREBALL)
                || type == EntityTypes.FIREWORK_ROCKET
                || type == EntityTypes.LLAMA_SPIT
                || type == EntityTypes.FISHING_BOBBER;
    }
}
