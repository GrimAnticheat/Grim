package ac.grim.grimac.utils.data;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
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
    public boolean isSwimming;
    public boolean isClimbing;
    public boolean isFallFlying;
    public World playerWorld;
    public WorldBorder playerWorldBorder;

    public double movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float flySpeed;
    public Entity playerVehicle;
    public double fallDistance;

    public int number;

    public PredictionData(GrimPlayer grimPlayer, double playerX, double playerY, double playerZ, float xRot, float yRot, boolean onGround) {
        this.grimPlayer = grimPlayer;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.onGround = onGround;

        this.number = grimPlayer.taskNumber.getAndIncrement();

        this.isSprinting = grimPlayer.isPacketSprinting;
        this.isSneaking = grimPlayer.isPacketSneaking;

        this.isFlying = grimPlayer.bukkitPlayer.isFlying();
        this.isSwimming = grimPlayer.bukkitPlayer.isSwimming();
        this.isClimbing = Collisions.onClimbable(grimPlayer);
        this.isFallFlying = grimPlayer.bukkitPlayer.isGliding();
        this.playerWorld = grimPlayer.bukkitPlayer.getWorld();
        this.fallDistance = grimPlayer.bukkitPlayer.getFallDistance();
        this.movementSpeed = grimPlayer.bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();

        PotionEffect jumpEffect = grimPlayer.bukkitPlayer.getPotionEffect(PotionEffectType.JUMP);
        this.jumpAmplifier = jumpEffect == null ? 0 : jumpEffect.getAmplifier();

        PotionEffect levitationEffect = grimPlayer.bukkitPlayer.getPotionEffect(PotionEffectType.LEVITATION);
        this.levitationAmplifier = levitationEffect == null ? 0 : levitationEffect.getAmplifier();

        this.flySpeed = grimPlayer.entityPlayer.abilities.flySpeed;
        this.playerVehicle = grimPlayer.bukkitPlayer.getVehicle();
    }
}
