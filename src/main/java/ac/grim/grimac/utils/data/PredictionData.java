package ac.grim.grimac.utils.data;

import ac.grim.grimac.GrimPlayer;
import org.bukkit.World;
import org.bukkit.entity.Vehicle;

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
    public World playerWorld;

    public float movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float flySpeed;
    public Vehicle playerVehicle;
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
        this.playerWorld = grimPlayer.bukkitPlayer.getWorld();
    }
}
