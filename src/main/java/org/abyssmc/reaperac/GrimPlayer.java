package org.abyssmc.reaperac;

import net.minecraft.server.v1_16_R3.EntityPlayer;
import org.abyssmc.reaperac.events.bukkit.PlayerLagback;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class GrimPlayer {
    // This is generous, but I don't see an issue with having a generous epsilon here
    public boolean isFlying;
    public boolean allowFlying;
    public boolean instantBreak;
    public Vector clientVelocity = new Vector();
    public Vector predictedVelocity;
    public Vector lastActualMovement = new Vector(0, 0, 0);
    public Vector actualMovement = new Vector(0, 0, 0);
    public Player bukkitPlayer;
    public EntityPlayer entityPlayer;

    public double lastX;
    public double lastY;
    public double lastZ;
    public float lastXRot;
    public float lastYRot;
    public boolean lastOnGround;

    // Timer check data
    public long offset = 0L;
    public long lastMovementPacket = System.currentTimeMillis() - 50000000L;
    public boolean lastPacketIsReminder = false;

    public GrimPlayer(Player player) {
        this.bukkitPlayer = player;
        this.entityPlayer = ((CraftPlayer) player).getHandle();
    }

    // TODO: STOP MAKING THIS A GOD CLASS AND PUT THIS IN IT'S OWN CLASS
    public void lagback() {
        // TODO: MAKE THIS BE THREAD SAFE!
        PlayerLagback.playersToLagback.add(bukkitPlayer.getUniqueId());

        Bukkit.broadcastMessage("Failed timer check!");
    }
}