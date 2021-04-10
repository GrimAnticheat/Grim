package ac.grim.grimac.utils.data;

import ac.grim.grimac.GrimPlayer;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
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
    public AxisAlignedBB boundingBox;
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

        // Plugins changing these values breaks both sync and async checks, so we might as well be async
        // Other packets can't arrive before this one does because we are blocking other player packets from arriving
        // Meaning that isSprinting and isSneaking are thread safe, and are primitives so the values stay

        // playerWorld returns a final variable, so it is thread safe

        // boundingBox is before the movement because we are blocking the movement packet, so it is thread safe
        // we have to clone it manually because it will change immediately after we stop blocking
        AxisAlignedBB box = grimPlayer.entityPlayer.getBoundingBox();
        this.boundingBox = new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);

        this.isSprinting = grimPlayer.bukkitPlayer.isSprinting();
        this.isSneaking = grimPlayer.bukkitPlayer.isSneaking();
        this.isFlying = grimPlayer.bukkitPlayer.isFlying();
        this.isSwimming = grimPlayer.bukkitPlayer.isSwimming();
        this.playerWorld = grimPlayer.bukkitPlayer.getWorld();
    }
}
