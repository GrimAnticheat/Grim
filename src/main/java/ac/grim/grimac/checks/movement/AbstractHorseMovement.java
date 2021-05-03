package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.util.Vector;

import static ac.grim.grimac.checks.movement.predictions.PredictionEngine.getMovementResultFromInput;

public class AbstractHorseMovement {

    // Wow, this is actually really close to the player's movement
    public static void travel(Vector inputMovement, GrimPlayer grimPlayer) {
        AbstractHorse horse = (AbstractHorse) grimPlayer.playerVehicle;
        if (horse.getInventory().getSaddle() != null) {

            float f = grimPlayer.vehicleHorizontal * 0.5F;
            float f1 = grimPlayer.vehicleForward;

            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }

            // TODO: This takes away control of the player when the horse is standing
            /*if (this.onGround && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
                f = 0.0F;
                f1 = 0.0F;
            }*/

            // TODO: Handle jump
            /*if (this.playerJumpPendingScale > 0.0F && !this.isJumping() && this.onGround) {
                double d0 = this.getCustomJump() * (double) this.playerJumpPendingScale * (double) this.getBlockJumpFactor();
                double d1;
                if (this.hasEffect(Effects.JUMP)) {
                    d1 = d0 + (double) ((float) (this.getEffect(Effects.JUMP).getAmplifier() + 1) * 0.1F);
                } else {
                    d1 = d0;
                }

                Vector3d vector3d = this.getDeltaMovement();
                this.setDeltaMovement(vector3d.x, d1, vector3d.z);
                this.setIsJumping(true);
                this.hasImpulse = true;
                if (f1 > 0.0F) {
                    float f2 = MathHelper.sin(this.yRot * ((float) Math.PI / 180F));
                    float f3 = MathHelper.cos(this.yRot * ((float) Math.PI / 180F));
                    this.setDeltaMovement(this.getDeltaMovement().add((double) (-0.4F * f2 * this.playerJumpPendingScale), 0.0D, (double) (0.4F * f3 * this.playerJumpPendingScale)));
                }

                this.playerJumpPendingScale = 0.0F;
            }*/

            //this.flyingSpeed = this.getSpeed() * 0.1F;

            //this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));

            // LivingEntity AIStep
            grimPlayer.clientVelocity.multiply(0.98);

            // TODO: This doesn't work with water or lava.
            float blockFriction = BlockProperties.getBlockFriction(grimPlayer);
            grimPlayer.friction = grimPlayer.lastOnGround ? blockFriction * 0.91f : 0.91f;
            // TODO: Check if horse is on ground
            grimPlayer.friction = blockFriction * 0.91f;


            float frictionSpeed = getFrictionInfluencedSpeed(blockFriction, grimPlayer);

            Vector movementInputResult = getMovementResultFromInput(new Vector(f, 0, f1), frictionSpeed, grimPlayer.xRot);
            grimPlayer.clientVelocity = grimPlayer.clientVelocity.clone().add(movementInputResult).multiply(grimPlayer.stuckSpeedMultiplier);
            MovementVelocityCheck.move(grimPlayer, MoverType.SELF, grimPlayer.clientVelocity);

            grimPlayer.clientVelocity.multiply(new Vector(grimPlayer.friction, 0.98, grimPlayer.friction));

            // More jumping stuff
            /*if (this.onGround) {
                this.playerJumpPendingScale = 0.0F;
                this.setIsJumping(false);
            }*/

            // TODO: Handle if the player has no saddle
        } /*else {
            this.flyingSpeed = 0.02F;
            super.travel(inputMovement);
        }*/
    }

    public static float getFrictionInfluencedSpeed(float f, GrimPlayer grimPlayer) {
        //Player bukkitPlayer = grimPlayer.bukkitPlayer;

        AbstractHorse horse = (AbstractHorse) grimPlayer.playerVehicle;

        return (float) (horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * (0.21600002f / (f * f * f)));
    }
}
