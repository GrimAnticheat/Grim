package org.abyssmc.reaperac.events.anticheat;

import net.minecraft.server.v1_16_R3.*;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.math.Mth;
import org.abyssmc.reaperac.utils.nmsImplementations.CheckIfChunksLoaded;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Iterator;

public class PlayerBaseTick {
    GrimPlayer player;

    public PlayerBaseTick(GrimPlayer player) {
        this.player = player;
    }

    public void doBaseTick() {
        updateInWaterStateAndDoFluidPushing();
        updateFluidOnEyes();
        // TODO: Swimming check
        //updateSwimming();
    }

    // Entity line 937
    public void updateInWaterStateAndDoFluidPushing() {
        player.fluidHeight.clear();
        updateInWaterStateAndDoWaterCurrentPushing();
        double d = player.entityPlayer.world.getDimensionManager().isNether() ? 0.007 : 0.0023333333333333335;
        this.updateFluidHeightAndDoFluidPushing(TagsFluid.LAVA, d);
    }

    private void updateFluidOnEyes() {
        player.wasEyeInWater = player.isEyeInFluid(TagsFluid.WATER);
        player.fluidOnEyes = null;
        double d0 = player.entityPlayer.getHeadY() - 0.1111111119389534D;
        Entity entity = player.entityPlayer.getVehicle();
        if (entity instanceof EntityBoat) {
            EntityBoat entityboat = (EntityBoat)entity;
            if (!entityboat.aI() && entityboat.getBoundingBox().maxY >= d0 && entityboat.getBoundingBox().minY <= d0) {
                return;
            }
        }

        BlockPosition blockposition = new BlockPosition(player.x, d0, player.z);
        Fluid fluid = ((CraftWorld) player.bukkitPlayer.getWorld()).getHandle().getFluid(blockposition);
        Iterator iterator = TagsFluid.b().iterator();

        while(iterator.hasNext()) {
            Tag tag = (Tag)iterator.next();
            if (fluid.a(tag)) {
                double d1 = (float)blockposition.getY() + fluid.getHeight(player.entityPlayer.getWorld(), blockposition);
                if (d1 > d0) {
                    player.fluidOnEyes = tag;
                }

                return;
            }
        }

    }

    // Entity line 945
    void updateInWaterStateAndDoWaterCurrentPushing() {
        if (player.bukkitPlayer.getVehicle() instanceof EntityBoat) {
            player.wasTouchingWater = false;
        } else if (this.updateFluidHeightAndDoFluidPushing(TagsFluid.WATER, 0.014)) {
            // Watersplash effect removed (Entity 981).  Shouldn't affect movement
            player.fallDistance = 0.0f;
            player.wasTouchingWater = true;
            //this.clearFire();
        } else {
            player.wasTouchingWater = false;
        }
    }

    // TODO: Idk if this is right
    public boolean updateFluidHeightAndDoFluidPushing(Tag.e<FluidType> tag, double d) {
        BoundingBox aABB = player.bukkitPlayer.getBoundingBox().expand(-0.001);
        int n2 = Mth.floor(aABB.getMinX());
        int n3 = Mth.ceil(aABB.getMaxX());
        int n4 = Mth.floor(aABB.getMinY());
        int n5 = Mth.ceil(aABB.getMaxY());
        int n6 = Mth.floor(aABB.getMinZ());
        int n = Mth.ceil(aABB.getMaxZ());
        if (!CheckIfChunksLoaded.hasChunksAt(player.bukkitPlayer.getWorld(), n2, n4, n6, n3, n5, n)) {
            return false;
        }
        double d2 = 0.0;
        boolean bl2 = false;
        Vec3D vec3 = Vec3D.ORIGIN;
        int n7 = 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        WorldServer playerWorld = ((CraftWorld) player.bukkitPlayer.getWorld()).getHandle();
        for (int i = n2; i < n3; ++i) {
            for (int j = n4; j < n5; ++j) {
                for (int k = n6; k < n; ++k) {
                    double d3;
                    mutableBlockPos.d(i, j, k);
                    Fluid fluid = playerWorld.getFluid(mutableBlockPos);
                    if (!fluid.a(tag) || !((d3 = (float) j + fluid.getHeight(playerWorld, mutableBlockPos)) >= aABB.getMinY()))
                        continue;
                    bl2 = true;
                    d2 = Math.max(d3 - aABB.getMinY(), d2);
                    fluid.c(playerWorld, mutableBlockPos);
                    Vec3D vec32 = fluid.c(playerWorld, mutableBlockPos);
                    if (d2 < 0.4) {
                        vec32 = vec32.a(d2);
                    }
                    vec3 = vec3.e(vec32);
                    ++n7;
                }
            }
        }
        // Originally length... thanks for a pointless square root
        if (vec3.g() > 0.0) {
            if (n7 > 0) {
                vec3 = vec3.a(1.0 / (double) n7);
            }

            Vector vec33 = player.clientVelocity;
            vec3 = vec3.a(d);
            // Originally length (sqrt) but I replaced with squared
            if (Math.abs(vec33.getX()) < 0.003 && Math.abs(vec33.getZ()) < 0.003 && vec3.g() < 0.00002025) {
                vec3 = vec3.d().a(0.0045000000000000005);
            }
            player.clientVelocity = vec33.add(new Vector(vec3.x, vec3.y, vec3.z));
        }
        player.fluidHeight.put(tag, d2);
        return bl2;
    }
}
