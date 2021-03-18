package org.abyssmc.reaperac;

import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.FluidType;
import net.minecraft.server.v1_16_R3.Tag;
import org.abyssmc.reaperac.events.bukkit.PlayerLagback;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class GrimPlayer {
    // This is generous, but I don't see an issue with having a generous epsilon here
    public boolean isFlying;
    public boolean allowFlying;
    public boolean instantBreak;
    public Vector clientVelocity = new Vector();
    public Vector predictedVelocity = new Vector(0,0,0);
    public Vector lastActualMovement = new Vector(0, 0, 0);
    public Vector actualMovement = new Vector(0, 0, 0);
    public Player bukkitPlayer;
    public EntityPlayer entityPlayer;

    // Set from packet
    public double x;
    public double y;
    public double z;
    public float xRot;
    public float yRot;
    public boolean onGround;

    // We determine this
    public boolean isActuallyOnGround;

    // We guess this
    public double bestX;
    public double bestZ;
    public boolean bestJumping;

    // Set from base tick
    public Object2DoubleMap<Tag.e<FluidType>> fluidHeight = new Object2DoubleArrayMap<>(2);
    public boolean wasTouchingWater = false;
    public boolean wasEyeInWater = false;
    public Tag fluidOnEyes;

    // Placeholder, currently not used in any checks
    public double fallDistance = 0f;

    // Set after checks
    public double lastX;
    public double lastY;
    public double lastZ;
    public float lastXRot;
    public float lastYRot;
    public boolean lastOnGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;

    // Timer check data
    public long offset = 0L;
    public long lastMovementPacket = System.currentTimeMillis() - 50000000L;
    public boolean lastPacketIsReminder = false;

    public GrimPlayer(Player player) {
        this.bukkitPlayer = player;
        this.entityPlayer = ((CraftPlayer) player).getHandle();

        Location loginLocation = player.getLocation();
        lastX = loginLocation.getX();
        lastY = loginLocation.getY();
        lastZ = loginLocation.getZ();
    }

    // TODO: STOP MAKING THIS A GOD CLASS AND PUT THIS IN IT'S OWN CLASS
    public void lagback() {
        // TODO: MAKE THIS BE THREAD SAFE!
        PlayerLagback.playersToLagback.add(bukkitPlayer.getUniqueId());

        Bukkit.broadcastMessage("Failed timer check!");
    }

    public boolean isEyeInFluid(Tag tag) {
        return this.fluidOnEyes == tag;
    }
}