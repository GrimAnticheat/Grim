package ac.grim.grimac.utils.data;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.Collection;

public class PredictionData {
    private static final Method onePointEightAttribute;
    private static Object movementSpeedAttribute;

    static {
        onePointEightAttribute = Reflection.getMethod(NMSUtils.entityHumanClass, "getAttributeInstance", 0);
        try {
            if (XMaterial.getVersion() == 8) {
                // 1.8 mappings
                movementSpeedAttribute = NMSUtils.getNMSClass("GenericAttributes").getDeclaredField("MOVEMENT_SPEED").get(null);
            } else if (XMaterial.getVersion() < 8) {
                // 1.7 mappings
                movementSpeedAttribute = NMSUtils.getNMSClass("GenericAttributes").getDeclaredField("d").get(null);
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public GrimPlayer player;
    public double playerX;
    public double playerY;
    public double playerZ;
    public float xRot;
    public float yRot;
    public boolean onGround;
    public boolean isSprinting;
    public boolean isSneaking;
    public World playerWorld;
    public double movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float slowFallingAmplifier;
    public float dolphinsGraceAmplifier;
    public float flySpeed;
    public double fallDistance;
    public boolean inVehicle;
    public Entity playerVehicle;
    public float vehicleHorizontal;
    public float vehicleForward;
    public boolean isJustTeleported = false;
    public VelocityData firstBreadKB = null;
    public VelocityData requiredKB = null;
    public VelocityData firstBreadExplosion = null;
    public VelocityData possibleExplosion = null;
    public int minimumTickRequiredToContinue;
    public int lastTransaction;

    // For regular movement
    public PredictionData(GrimPlayer player, double playerX, double playerY, double playerZ, float xRot, float yRot, boolean onGround) {
        this.player = player;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.onGround = onGround;
        this.inVehicle = player.playerVehicle != null;

        this.isSprinting = player.packetStateData.isPacketSprinting;
        this.isSneaking = player.packetStateData.isPacketSneaking;
        this.playerWorld = player.bukkitPlayer.getWorld();
        this.fallDistance = player.bukkitPlayer.getFallDistance();
        this.movementSpeed = getMovementSpeedAttribute(player.bukkitPlayer);

        // When a player punches a mob, bukkit thinks the player isn't sprinting
        if (isSprinting && !player.bukkitPlayer.isSprinting()) this.movementSpeed *= 1.3D;

        Collection<PotionEffect> playerPotionEffects = player.bukkitPlayer.getActivePotionEffects();

        this.jumpAmplifier = getHighestPotionEffect(playerPotionEffects, "JUMP", 0);
        this.levitationAmplifier = getHighestPotionEffect(playerPotionEffects, "LEVITATION", 9);
        this.slowFallingAmplifier = getHighestPotionEffect(playerPotionEffects, "SLOW_FALLING", 13);
        this.dolphinsGraceAmplifier = getHighestPotionEffect(playerPotionEffects, "DOLPHINS_GRACE", 13);

        this.flySpeed = player.bukkitPlayer.getFlySpeed() / 2;
        this.playerVehicle = player.bukkitPlayer.getVehicle();

        firstBreadKB = player.knockbackHandler.getFirstBreadOnlyKnockback();
        requiredKB = player.knockbackHandler.getRequiredKB();

        firstBreadExplosion = player.explosionHandler.getFirstBreadAddedExplosion();
        possibleExplosion = player.explosionHandler.getPossibleExplosions();

        minimumTickRequiredToContinue = GrimAC.getCurrentTick() + 1;
        lastTransaction = player.packetStateData.packetLastTransactionReceived;
    }

    // For boat movement
    public PredictionData(GrimPlayer player, double boatX, double boatY, double boatZ, float xRot, float yRot) {
        this.player = player;
        this.playerX = boatX;
        this.playerY = boatY;
        this.playerZ = boatZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.playerVehicle = player.bukkitPlayer.getVehicle();
        this.vehicleForward = player.packetStateData.packetVehicleForward;
        this.vehicleHorizontal = player.packetStateData.packetVehicleHorizontal;

        this.inVehicle = true;

        this.playerWorld = player.bukkitPlayer.getWorld();
        this.fallDistance = player.bukkitPlayer.getFallDistance();
        this.movementSpeed = getMovementSpeedAttribute(player.bukkitPlayer);

        minimumTickRequiredToContinue = GrimAC.getCurrentTick() + 1;
        lastTransaction = player.packetStateData.packetLastTransactionReceived;
    }

    private double getMovementSpeedAttribute(Player player) {
        if (XMaterial.getVersion() > 8) {
            return player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
        }

        try {
            Method handle = Reflection.getMethod(player.getClass(), "getHandle", 0);
            Object attribute = onePointEightAttribute.invoke(handle.invoke(player), movementSpeedAttribute);
            Method valueField = attribute.getClass().getMethod("getValue");
            return (double) valueField.invoke(attribute);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.1f;
    }

    private float getHighestPotionEffect(Collection<PotionEffect> effects, String typeName, int minimumVersion) {
        if (XMaterial.getVersion() < minimumVersion) return 0;

        PotionEffectType type = PotionEffectType.getByName(typeName);

        float highestEffect = 0;
        for (PotionEffect effect : effects) {
            if (effect.getType() == type && effect.getAmplifier() > highestEffect)
                highestEffect = effect.getAmplifier();
        }

        return highestEffect;
    }
}
