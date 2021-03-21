package org.abyssmc.reaperac.utils.nmsImplementations;

import net.minecraft.server.v1_16_R3.*;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.stream.Stream;

public class Collisions {
    public static final double maxUpStep = 0.6f;

    // Entity line 686
    public static Vector collide(Vector vector, GrimPlayer grimPlayer) {
        Vec3D vec3 = new Vec3D(vector.getX(), vector.getY(), vector.getZ());

        AxisAlignedBB aABB = grimPlayer.entityPlayer.getBoundingBox();
        VoxelShapeCollision collisionContext = VoxelShapeCollision.a(grimPlayer.entityPlayer);
        VoxelShape voxelShape = grimPlayer.entityPlayer.getWorld().getWorldBorder().c();
        Stream<VoxelShape> stream = VoxelShapes.c(voxelShape, VoxelShapes.a(aABB.shrink(1.0E-7)), OperatorBoolean.AND) ? Stream.empty() : Stream.of(voxelShape);
        Stream<VoxelShape> stream2 = grimPlayer.entityPlayer.getWorld().c(grimPlayer.entityPlayer, aABB.b(vec3), entity -> true);
        StreamAccumulator<VoxelShape> rewindableStream = new StreamAccumulator<>(Stream.concat(stream2, stream));

        Vec3D vec32 = vec3.g() == 0.0 ? vec3 : Entity.a(grimPlayer.entityPlayer, vec3, aABB, grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
        boolean bl2 = vec3.x != vec32.x;
        boolean bl3 = vec3.y != vec32.y;
        boolean bl4 = vec3.z != vec32.z;
        boolean bl = grimPlayer.lastOnGround || bl3 && vec3.y < 0.0;
        if (bl && (bl2 || bl4)) {
            Vec3D vec33;
            Vec3D vec34 = Entity.a(grimPlayer.entityPlayer, new Vec3D(vec3.x, maxUpStep, vec3.z), aABB, grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
            Vec3D vec35 = Entity.a(grimPlayer.entityPlayer, new Vec3D(0.0, maxUpStep, 0.0), aABB.b(vec3.x, 0.0, vec3.z), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
            if (vec35.y < maxUpStep && Entity.c(vec33 = Entity.a(grimPlayer.entityPlayer, new Vec3D(vec3.x, 0.0, vec3.z), AxisAlignedBB.a(vec35), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream).e(vec35)) > Entity.c(vec34)) {
                vec34 = vec33;
            }
            if (Entity.c(vec34) > Entity.c(vec32)) {
                Vec3D allowedMovement = Entity.a(grimPlayer.entityPlayer, new Vec3D(0.0, -vec34.y + vec3.y, 0.0), aABB.c(vec34), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
                vec34 = vec34.e(allowedMovement);
                return new Vector(vec34.x, vec34.y, vec34.z);
            }
        }
        return new Vector(vec32.x, vec32.y, vec32.z);
    }

    // MCP mappings PlayerEntity 959
    // Mojang mappings 936
    public static Vector maybeBackOffFromEdge(Vector vec3, MoverType moverType, GrimPlayer grimPlayer) {
        Player bukkitPlayer = grimPlayer.bukkitPlayer;

        if (!bukkitPlayer.isFlying() && (moverType == MoverType.SELF || moverType == MoverType.PLAYER) && bukkitPlayer.isSneaking() && isAboveGround(grimPlayer)) {
            double d = vec3.getX();
            double d2 = vec3.getZ();
            while (d != 0.0 && ((CraftWorld) bukkitPlayer.getWorld()).getHandle().getCubes(((CraftPlayer) bukkitPlayer).getHandle(),
                    ((CraftPlayer) bukkitPlayer).getHandle().getBoundingBox().d(d, -maxUpStep, 0.0))) {
                if (d < 0.05 && d >= -0.05) {
                    d = 0.0;
                    continue;
                }
                if (d > 0.0) {
                    d -= 0.05;
                    continue;
                }
                d += 0.05;
            }
            while (d2 != 0.0 && ((CraftWorld) bukkitPlayer.getWorld()).getHandle().getCubes(((CraftPlayer) bukkitPlayer).getHandle(),
                    ((CraftPlayer) bukkitPlayer).getHandle().getBoundingBox().d(0.0, -maxUpStep, d2))) {
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    continue;
                }
                if (d2 > 0.0) {
                    d2 -= 0.05;
                    continue;
                }
                d2 += 0.05;
            }
            while (d != 0.0 && d2 != 0.0 && ((CraftWorld) bukkitPlayer.getWorld()).getHandle().getCubes(((CraftPlayer) bukkitPlayer).getHandle(),
                    ((CraftPlayer) bukkitPlayer).getHandle().getBoundingBox().d(d, -maxUpStep, d2))) {
                d = d < 0.05 && d >= -0.05 ? 0.0 : (d > 0.0 ? (d -= 0.05) : (d += 0.05));
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    continue;
                }
                if (d2 > 0.0) {
                    d2 -= 0.05;
                    continue;
                }
                d2 += 0.05;
            }
            vec3 = new Vector(d, vec3.getY(), d2);
        }
        return vec3;
    }

    // What the fuck is this?
    private static boolean isAboveGround(GrimPlayer grimPlayer) {
        Player bukkitPlayer = grimPlayer.bukkitPlayer;

        return grimPlayer.lastOnGround || bukkitPlayer.getFallDistance() < Collisions.maxUpStep && !
                ((CraftWorld) bukkitPlayer.getWorld()).getHandle().getCubes(((CraftPlayer) bukkitPlayer).getHandle(), ((CraftPlayer) bukkitPlayer).getHandle().getBoundingBox().d(0.0, bukkitPlayer.getFallDistance() - Collisions.maxUpStep, 0.0));
    }
}
