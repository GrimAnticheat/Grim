package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.BlockProperties;
import ac.grim.grimac.utils.nmsutil.JumpPower;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public class PacketEntityCamel extends PacketEntityHorse implements DashableEntity {

    private boolean dashing = false;
    private int dashCooldown = 0;

    public PacketEntityCamel(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z, float xRot) {
        super(player, uuid, type, x, y, z, xRot);

        setAttribute(Attributes.JUMP_STRENGTH, 0.42f);
        setAttribute(Attributes.MOVEMENT_SPEED, 0.09f);
        setAttribute(Attributes.STEP_HEIGHT, 1.5f);
    }

    @Override
    public boolean canPlayerJump(GrimPlayer player) {
        return this.hasSaddle() && this.dashCooldown <= 0 && player.onGround;
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
        final boolean wantsToJump = this.getJumpPower() > 0.0F && !this.isJumping() && player.lastOnGround;
        if (!wantsToJump) return;

        final double jumpFactor = this.getAttributeValue(Attributes.JUMP_STRENGTH) * JumpPower.getPlayerJumpFactor(player);

        double jumpYVelocity;

        final OptionalInt jumpBoost = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
        if (jumpBoost.isPresent()) {
            jumpYVelocity = jumpFactor + ((jumpBoost.getAsInt() + 1) * 0.1F);
        } else {
            jumpYVelocity = jumpFactor;
        }

        final double multiplier = (double) (22.2222F * this.getJumpPower()) * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (double) BlockProperties.getBlockSpeedFactor(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ));
        Vector3dm jumpVelocity = ReachUtils.getLook(player, player.yaw, player.pitch)
                .multiply(1.0, 0.0, 1.0)
                .normalize()
                .multiply(multiplier)
                .add(0, (double) (1.4285F * this.getJumpPower()) * jumpYVelocity, 0);

        for (VectorData vectorData : possibleVectors) {
            vectorData.vector.add(jumpVelocity);
        }

        this.setDashing(true);
        this.setDashCooldown(55);
        this.setJumpPower(0.0F);
    }

}
