package ac.grim.grimac.platform.fabric.mc1211.entity;

import ac.grim.grimac.platform.fabric.entity.FabricGrimEntity;
import ac.grim.grimac.platform.fabric.world.FabricPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class Fabric1211GrimEntity extends FabricGrimEntity {
    public Fabric1211GrimEntity(Entity entity) {
        super(entity);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return CompletableFuture.supplyAsync(() -> {
            if (entity.getWorld() instanceof ServerWorld) {
                entity.teleport(
                        ((FabricPlatformWorld) location.getWorld()).getFabricWorld(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        EnumSet.noneOf(PositionFlag.class), // todo change to match paper? Do they do this?
                        location.getYaw(),
                        location.getPitch()

                );
                return true;
            }
            return false;
        });
    }
}
