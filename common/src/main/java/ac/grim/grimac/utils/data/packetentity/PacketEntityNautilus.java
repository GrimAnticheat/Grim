package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.BlockProperties;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.Set;
import java.util.UUID;

public class PacketEntityNautilus extends PacketEntity implements JumpableEntity, DashableEntity {

    private boolean jumping = false;
    private float jumpPower = 0;

    private boolean dashing = false;
    private int dashCooldown = 0;

    public PacketEntityNautilus(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
        this.trackEntityEquipment = true;
        trackAttribute(ValuedAttribute.ranged(Attributes.MOVEMENT_SPEED, 1d, 0d, 1024d));
        setAttribute(Attributes.STEP_HEIGHT, 1f);
    }

    @Override
    public boolean isJumping() {
        return this.jumping;
    }

    @Override
    public void setJumping(boolean jumping) {
        this.jumping = jumping;
    }

    @Override
    public float getJumpPower() {
        return this.jumpPower;
    }

    @Override
    public void setJumpPower(float jumpPower) {
        this.jumpPower = jumpPower;
    }

    @Override
    public boolean canPlayerJump(GrimPlayer player) {
        return this.hasSaddle() && this.dashCooldown <= 0;
    }

    @Override
    public boolean hasSaddle() {
        return hasItemInSlot(EquipmentSlot.SADDLE);
    }

    @Override
    public boolean isDashing() {
        return this.dashing;
    }

    @Override
    public void setDashing(boolean dashing) {
        this.dashing = dashing;
    }

    @Override
    public int getDashCooldown() {
        return this.dashCooldown;
    }

    @Override
    public void setDashCooldown(int dashCooldown) {
        this.dashCooldown = dashCooldown;
    }

    @Override
    public void executeJump(GrimPlayer player, Set<VectorData> possibleVectors) {
        final boolean wantsToJump = this.getJumpPower() > 0.0F && !this.isJumping();
        if (!wantsToJump) return;

        final float pitch = player.vehicleData.playerPitch, yaw = player.vehicleData.playerYaw;
        final double multiplier = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * BlockProperties.getBlockSpeedFactor(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ));
        Vector3dm jumpVelocity = ReachUtils.getLook(player, yaw, pitch)
                .multiply((player.wasTouchingWater ? 1.2F : 0.5F) * this.getJumpPower() * multiplier);

        for (VectorData vectorData : possibleVectors) {
            vectorData.vector.add(jumpVelocity);
        }

        this.setDashing(true);
        this.setDashCooldown(40);
        this.setJumpPower(0.0F);
    }

}
