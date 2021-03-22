package org.abyssmc.reaperac.utils.nmsImplementations;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.math.Mth;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class JumpPower {
    public static Vector jumpFromGround(GrimPlayer grimPlayer) {
        Player bukkitPlayer = grimPlayer.bukkitPlayer;

        Vector clonedClientVelocity = grimPlayer.clientVelocity.clone();
        float f = getJumpPower(bukkitPlayer);

        if (bukkitPlayer.hasPotionEffect(PotionEffectType.JUMP)) {
            f += 0.1f * (float) (bukkitPlayer.getPotionEffect(PotionEffectType.JUMP).getAmplifier() + 1);
        }

        clonedClientVelocity.setY(f);

        // TODO: Use the stuff from the sprinting packet
        if (bukkitPlayer.isSprinting()) {
            float f2 = grimPlayer.xRot * 0.017453292f;
            clonedClientVelocity.add(new Vector(-Mth.sin(f2) * 0.2f, 0.0, Mth.cos(f2) * 0.2f));
        }

        return clonedClientVelocity;
    }

    public static Vector baseJumpFromGround(GrimPlayer grimPlayer) {
        Player bukkitPlayer = grimPlayer.bukkitPlayer;

        float f = getJumpPower(bukkitPlayer);

        if (bukkitPlayer.hasPotionEffect(PotionEffectType.JUMP)) {
            f += 0.1f * (float) (bukkitPlayer.getPotionEffect(PotionEffectType.JUMP).getAmplifier() + 1);
        }

        Vector additionalMovement = new Vector(0, f, 0);

        if (bukkitPlayer.isSprinting()) {
            float f2 = grimPlayer.xRot * 0.017453292f;
            additionalMovement.add(new Vector(-Mth.sin(f2) * 0.2f, 0.0, Mth.cos(f2) * 0.2f));
        }

        return additionalMovement;
    }

    public static float getJumpPower(Player bukkitPlayer) {
        return 0.42f * getPlayerJumpFactor(bukkitPlayer);
    }

    private static float getPlayerJumpFactor(Player bukkitPlayer) {
        float f = ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(), bukkitPlayer.getLocation().getBlockY(), bukkitPlayer.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getJumpFactor();
        float f2 = ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(), (int) (bukkitPlayer.getBoundingBox().getMinY() - 0.5000001),
                        bukkitPlayer.getLocation().getBlockZ()).getBlockData()).getState().getBlock().getJumpFactor();

        return (double) f == 1.0 ? f2 : f;
    }
}
