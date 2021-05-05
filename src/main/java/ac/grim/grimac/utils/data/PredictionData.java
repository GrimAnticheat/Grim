package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.Collisions;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PredictionData {
    public GrimPlayer grimPlayer;
    public double playerX;
    public double playerY;
    public double playerZ;
    public float xRot;
    public float yRot;
    public boolean onGround;
    public boolean isSprinting;
    public boolean isSneaking;
    public boolean isFlying;
    public boolean isClimbing;
    public boolean isFallFlying;
    public World playerWorld;
    public WorldBorder playerWorldBorder;

    public double movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float flySpeed;

    public double fallDistance;

    // Debug, does nothing.
    public int number;

    public boolean inVehicle;
    public Entity playerVehicle;
    public float vehicleHorizontal;
    public float vehicleForward;

    public boolean isSprintingChange;
    public boolean isSneakingChange;

    // For regular movement
    public PredictionData(GrimPlayer grimPlayer, double playerX, double playerY, double playerZ, float xRot, float yRot, boolean onGround) {
        this.grimPlayer = grimPlayer;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.onGround = onGround;
        this.inVehicle = grimPlayer.playerVehicle != null;

        this.number = grimPlayer.taskNumber.getAndIncrement();

        this.isSprinting = grimPlayer.isPacketSprinting;
        this.isSneaking = grimPlayer.isPacketSneaking;

        this.isSprintingChange = grimPlayer.isPacketSprintingChange;
        this.isSneakingChange = grimPlayer.isPacketSneakingChange;
        grimPlayer.isPacketSprintingChange = false;
        grimPlayer.isPacketSneakingChange = false;

        // Don't let the player fly with packets - Don't rely on non-lag compensated bukkit
        this.isFlying = grimPlayer.packetFlyingDanger && grimPlayer.compensatedFlying.getCanPlayerFlyLagCompensated();
        // Stop false from if a player is flying, we toggle their fly off, they land, we toggle their flight on
        grimPlayer.packetFlyingDanger = isFlying;

        this.isClimbing = Collisions.onClimbable(grimPlayer);
        this.isFallFlying = grimPlayer.bukkitPlayer.isGliding();
        this.playerWorld = grimPlayer.bukkitPlayer.getWorld();
        this.fallDistance = grimPlayer.bukkitPlayer.getFallDistance();
        this.movementSpeed = grimPlayer.bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();

        // When a player punches a mob, bukkit thinks the player isn't sprinting (?)
        // But they are, so we need to multiply by sprinting speed boost until I just get the player's attributes from packets
        if (isSprinting && !grimPlayer.bukkitPlayer.isSprinting()) this.movementSpeed *= 1.3;

        PotionEffect jumpEffect = grimPlayer.bukkitPlayer.getPotionEffect(PotionEffectType.JUMP);
        this.jumpAmplifier = jumpEffect == null ? 0 : jumpEffect.getAmplifier();

        PotionEffect levitationEffect = grimPlayer.bukkitPlayer.getPotionEffect(PotionEffectType.LEVITATION);
        this.levitationAmplifier = levitationEffect == null ? 0 : levitationEffect.getAmplifier();

        this.flySpeed = grimPlayer.bukkitPlayer.getFlySpeed() / 2;
        this.playerVehicle = grimPlayer.bukkitPlayer.getVehicle();
    }

    // For boat movement
    public PredictionData(GrimPlayer grimPlayer, double boatX, double boatY, double boatZ, float xRot, float yRot) {
        this.grimPlayer = grimPlayer;
        this.playerX = boatX;
        this.playerY = boatY;
        this.playerZ = boatZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.playerVehicle = grimPlayer.bukkitPlayer.getVehicle();
        this.vehicleForward = grimPlayer.packetVehicleForward;
        this.vehicleHorizontal = grimPlayer.packetVehicleHorizontal;

        this.inVehicle = true;

        this.isFlying = false;
        this.isClimbing = false;
        this.isFallFlying = false;
        this.playerWorld = grimPlayer.bukkitPlayer.getWorld();
        this.fallDistance = grimPlayer.bukkitPlayer.getFallDistance();
        this.movementSpeed = grimPlayer.bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
    }
}
