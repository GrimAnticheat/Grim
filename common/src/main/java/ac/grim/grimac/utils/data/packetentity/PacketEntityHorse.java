package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.JumpPower;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public class PacketEntityHorse extends PacketEntityTrackXRot implements JumpableEntity {

    public boolean isRearing = false;
    public boolean hasSaddle = false;
    public boolean isTame = false;

    private boolean horseJumping = false;
    private float horseJump = 0;

    public PacketEntityHorse(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z, float xRot) {
        super(player, uuid, type, x, y, z, xRot);
        this.trackEntityEquipment = true;
        setAttribute(Attributes.STEP_HEIGHT, 1.0f);

        final boolean preAttribute = player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5);
        // This was horse.jump_strength pre-attribute
        trackAttribute(ValuedAttribute.ranged(Attributes.JUMP_STRENGTH, 0.7, 0, preAttribute ? 2 : 32)
                .withSetRewriter((oldValue, newValue) -> {
                    // Seems viabackwards doesn't rewrite this (?)
                    if (preAttribute && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5)) {
                        return oldValue;
                    }
                    // Modern player OR an old server setting legacy horse.jump_strength attribute
                    return newValue;
                }));
        trackAttribute(ValuedAttribute.ranged(Attributes.MOVEMENT_SPEED, 0.225f, 0, 1024));

        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.CHESTED_HORSE)) {
            setAttribute(Attributes.JUMP_STRENGTH, 0.5);
            setAttribute(Attributes.MOVEMENT_SPEED, 0.175f);
        }

        if (type == EntityTypes.ZOMBIE_HORSE || type == EntityTypes.SKELETON_HORSE) {
            setAttribute(Attributes.MOVEMENT_SPEED, 0.2f);
        }
    }

    private static final boolean HAS_SADDLE_SENT_BY_SERVER = PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_21_4);

    @Override
    public boolean hasSaddle() {
        if (HAS_SADDLE_SENT_BY_SERVER) {
            return this.hasSaddle;
        }

        return hasItemInSlot(EquipmentSlot.SADDLE);
    }

    @Override
    public boolean isJumping() {
        return this.horseJumping;
    }

    @Override
    public void setJumping(boolean jumping) {
        this.horseJumping = jumping;
    }

    @Override
    public float getJumpPower() {
        return this.horseJump;
    }

    @Override
    public void setJumpPower(float jumpPower) {
        this.horseJump = jumpPower;
    }

    @Override
    public boolean canPlayerJump(GrimPlayer player) {
        return this.hasSaddle();
    }

    @Override
    public void executeJump(GrimPlayer player, Set<VectorData> possibleVectors) {
        // If the player wants to jump on a horse
        // Listen to Entity Action -> start jump with horse, stop jump with horse
        final boolean wantsToJump = this.getJumpPower() > 0.0F && !this.isJumping() && player.lastOnGround;
        if (!wantsToJump) return;

        float forwardInput = player.vehicleData.vehicleForward;

        if (forwardInput <= 0.0F) {
            forwardInput *= 0.25F;
        }

        double jumpFactor = (float) this.getAttributeValue(Attributes.JUMP_STRENGTH) * this.getJumpPower() * JumpPower.getPlayerJumpFactor(player);
        double jumpVelocity;

        // This doesn't even work because vehicle jump boost has (likely) been
        // broken ever since vehicle control became client sided
        //
        // But plugins can still send this, so support it anyways
        final OptionalInt jumpBoost = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
        if (jumpBoost.isPresent()) {
            jumpVelocity = jumpFactor + ((jumpBoost.getAsInt() + 1) * 0.1F);
        } else {
            jumpVelocity = jumpFactor;
        }

        this.setJumping(true);

        float yawRadians = GrimMath.radians(player.yaw);
        float f2 = player.trigHandler.sin(yawRadians);
        float f3 = player.trigHandler.cos(yawRadians);

        for (VectorData vectorData : possibleVectors) {
            vectorData.vector.setY(jumpVelocity);
            if (forwardInput > 0.0F) {
                vectorData.vector.add(new Vector3dm(-0.4F * f2 * this.getJumpPower(), 0.0D, 0.4F * f3 * this.getJumpPower()));
            }
        }

        this.setJumpPower(0.0F);
    }

}
