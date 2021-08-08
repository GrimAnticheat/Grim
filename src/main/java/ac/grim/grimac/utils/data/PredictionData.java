package ac.grim.grimac.utils.data;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.Hand;
import org.bukkit.World;

public class PredictionData {
    public GrimPlayer player;
    public double playerX;
    public double playerY;
    public double playerZ;
    public float xRot;
    public float yRot;
    public boolean onGround;
    public boolean isSprinting;
    public boolean isSneaking;
    public boolean isTryingToRiptide = false;
    public AlmostBoolean isUsingItem = AlmostBoolean.FALSE;
    public Hand usingHand = Hand.MAIN_HAND;
    public World playerWorld;
    public int jumpAmplifier = 0;
    public int levitationAmplifier = 0;
    public int slowFallingAmplifier = 0;
    public int dolphinsGraceAmplifier = 0;
    public float flySpeed;
    public float vehicleHorizontal;
    public float vehicleForward;
    public boolean isJustTeleported = false;
    public VelocityData firstBreadKB;
    public VelocityData requiredKB;
    public VelocityData firstBreadExplosion = null;
    public VelocityData possibleExplosion = null;
    public int minimumTickRequiredToContinue;
    public int lastTransaction;
    public int itemHeld;
    public float horseJump = 0;
    public boolean inVehicle = false;

    public int minPlayerAttackSlow = 0;
    public int maxPlayerAttackSlow = 0;

    public boolean isDummy = false;
    public boolean didGroundStatusChangeWithoutPositionPacket = false;

    public boolean isCheckNotReady;

    // For regular movement
    public PredictionData(GrimPlayer player, double playerX, double playerY, double playerZ, float xRot, float yRot, boolean onGround) {
        this.player = player;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.onGround = onGround;

        this.isSprinting = player.packetStateData.isPacketSprinting;
        this.isSneaking = player.packetStateData.isPacketSneaking;
        this.isTryingToRiptide = player.packetStateData.tryingToRiptide;
        player.packetStateData.tryingToRiptide = false;

        this.isUsingItem = player.packetStateData.slowedByUsingItem;
        this.usingHand = player.packetStateData.eatingHand;

        this.playerWorld = player.bukkitPlayer.getWorld();

        player.compensatedPotions.handleTransactionPacket(player.packetStateData.packetLastTransactionReceived.get());
        this.jumpAmplifier = player.compensatedPotions.getPotionLevel("JUMP");
        this.levitationAmplifier = player.compensatedPotions.getPotionLevel("LEVITATION");
        this.slowFallingAmplifier = player.compensatedPotions.getPotionLevel("SLOW_FALLING");
        this.dolphinsGraceAmplifier = player.compensatedPotions.getPotionLevel("DOLPHINS_GRACE");

        this.flySpeed = player.bukkitPlayer.getFlySpeed() / 2;

        firstBreadKB = player.knockbackHandler.getFirstBreadOnlyKnockback();
        requiredKB = player.knockbackHandler.getRequiredKB();

        firstBreadExplosion = player.explosionHandler.getFirstBreadAddedExplosion();
        possibleExplosion = player.explosionHandler.getPossibleExplosions();

        minimumTickRequiredToContinue = GrimAC.getCurrentTick() + 3;
        lastTransaction = player.packetStateData.packetLastTransactionReceived.get();

        itemHeld = player.packetStateData.lastSlotSelected;
        player.packetStateData.horseJump = 0;

        didGroundStatusChangeWithoutPositionPacket = player.packetStateData.didGroundStatusChangeWithoutPositionPacket;
        player.packetStateData.didGroundStatusChangeWithoutPositionPacket = false;

        minPlayerAttackSlow = player.packetStateData.minPlayerAttackSlow;
        player.packetStateData.minPlayerAttackSlow = 0;
        maxPlayerAttackSlow = player.packetStateData.maxPlayerAttackSlow;
        player.packetStateData.maxPlayerAttackSlow = 0;
    }

    // For riding entity movement while in control
    public PredictionData(GrimPlayer player, double boatX, double boatY, double boatZ, float xRot, float yRot) {
        this.player = player;
        this.playerX = boatX;
        this.playerY = boatY;
        this.playerZ = boatZ;
        this.xRot = xRot;
        this.yRot = yRot;
        this.onGround = true;
        this.isSprinting = false;
        this.isSneaking = false;
        this.vehicleForward = player.packetStateData.packetVehicleForward;
        this.vehicleHorizontal = player.packetStateData.packetVehicleHorizontal;

        player.compensatedPotions.handleTransactionPacket(player.packetStateData.packetLastTransactionReceived.get());
        this.jumpAmplifier = player.compensatedPotions.getPotionLevel("JUMP");
        this.levitationAmplifier = player.compensatedPotions.getPotionLevel("LEVITATION");
        this.slowFallingAmplifier = player.compensatedPotions.getPotionLevel("SLOW_FALLING");
        this.dolphinsGraceAmplifier = player.compensatedPotions.getPotionLevel("DOLPHINS_GRACE");

        this.playerWorld = player.bukkitPlayer.getWorld();

        firstBreadKB = player.knockbackHandler.getFirstBreadOnlyKnockback();
        requiredKB = player.knockbackHandler.getRequiredKB();

        firstBreadExplosion = player.explosionHandler.getFirstBreadAddedExplosion();
        possibleExplosion = player.explosionHandler.getPossibleExplosions();

        minimumTickRequiredToContinue = GrimAC.getCurrentTick() + 3;
        lastTransaction = player.packetStateData.packetLastTransactionReceived.get();

        itemHeld = player.packetStateData.lastSlotSelected;

        if (player.packetStateData.horseJump > 0) {
            if (player.packetStateData.horseJump >= 90) {
                horseJump = 1.0F;
            } else {
                horseJump = 0.4F + 0.4F * player.packetStateData.horseJump / 90.0F;
            }
        }

        inVehicle = true;

        player.packetStateData.horseJump = 0;
        player.packetStateData.tryingToRiptide = false;

        player.packetStateData.didGroundStatusChangeWithoutPositionPacket = false;

        player.packetStateData.minPlayerAttackSlow = 0;
        player.packetStateData.maxPlayerAttackSlow = 0;
    }

    public PredictionData(GrimPlayer player) {
        this.player = player;
        this.playerWorld = player.bukkitPlayer.getWorld();

        firstBreadKB = player.knockbackHandler.getFirstBreadOnlyKnockback();
        requiredKB = player.knockbackHandler.getRequiredKB();

        firstBreadExplosion = player.explosionHandler.getFirstBreadAddedExplosion();
        possibleExplosion = player.explosionHandler.getPossibleExplosions();

        minimumTickRequiredToContinue = GrimAC.getCurrentTick() + 3;
        lastTransaction = player.packetStateData.packetLastTransactionReceived.get();

        itemHeld = player.packetStateData.lastSlotSelected;

        inVehicle = true;
        isDummy = true;
        player.packetStateData.horseJump = 0;
        player.packetStateData.tryingToRiptide = false;

        player.packetStateData.didGroundStatusChangeWithoutPositionPacket = false;

        player.packetStateData.minPlayerAttackSlow = 0;
        player.packetStateData.maxPlayerAttackSlow = 0;
    }
}
