package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.FluidTypeFlowing;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;

import java.util.EnumMap;

public class EntityFluidInteraction {
    private final EnumMap<FluidTag, EntityFluidInteraction.Tracker> trackerByFluid = new EnumMap<>(FluidTag.class);

    public EntityFluidInteraction(final FluidTag... fluids) {
        for (FluidTag tagKey : fluids) {
            this.trackerByFluid.put(tagKey, new EntityFluidInteraction.Tracker());
        }
    }

    public void update(final GrimPlayer player, final boolean ignoreCurrent) {
        this.trackerByFluid.values().forEach(EntityFluidInteraction.Tracker::reset);

        SimpleCollisionBox aabb = player.boundingBox.copy().expand(-0.001);

        int minX = GrimMath.floor(aabb.minX);
        int minY = GrimMath.floor(aabb.minY);
        int minZ = GrimMath.floor(aabb.minZ);
        int maxX = GrimMath.ceil(aabb.maxX) - 1;
        int maxY = GrimMath.ceil(aabb.maxY) - 1;
        int maxZ = GrimMath.ceil(aabb.maxZ) - 1;

        if (!hasFluidAndLoaded(player.compensatedWorld, minX - 1, minY, minZ - 1, maxX + 1, maxY, maxZ + 1)) {
            return;
        }

        double aabbMinY = player.boundingBox.minY;

        int playerX = GrimMath.floor(player.lastX);
        double playerEyeY = player.lastY + player.getEyeHeight() - 0.1111111119389534D;
        int playerZ = GrimMath.floor(player.lastZ);

        FluidTag fluid = null;
        EntityFluidInteraction.Tracker tracker = null;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double fluidHeight = player.compensatedWorld.getFluidLevelAt(x, y, z);
                    if (fluidHeight == 0) {
                        continue;
                    }

                    double fluidHeightToWorld = (double) y + fluidHeight;
                    if (fluidHeightToWorld < aabb.minY) {
                        continue;
                    }

                    FluidTag newFluid = Materials.isWater(player.getClientVersion(), player.compensatedWorld.getBlock(x, y, z)) ? FluidTag.WATER : FluidTag.LAVA;
                    if (newFluid != fluid) {
                        fluid = newFluid;
                        tracker = this.getTrackerFor(newFluid);
                    }

                    if (tracker != null) {
                        if (x == playerX && z == playerZ && playerEyeY >= (double) y && playerEyeY <= fluidHeightToWorld) {
                            tracker.eyesInside = true;
                        }

                        tracker.height = Math.max(fluidHeightToWorld - aabbMinY, tracker.height);
                        if (!ignoreCurrent) {
                            Vector3dm current = FluidTypeFlowing.getFlow(player, x, y, z);
                            if (tracker.height < 0.4) {
                                current = current.multiply(tracker.height);
                            }

                            tracker.accumulateCurrent(current);
                        }
                    }
                }
            }
        }
    }

    private static final boolean HAS_FLUID_COUNT = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_26_1);

    private static boolean hasFluidAndLoaded(final CompensatedWorld level, final int x0, final int y0, final int z0, final int x1, final int y1, final int z1) {
        int minX = x0 >> 4;
        int minY = y0 >> 4;
        int minZ = z0 >> 4;
        int maxX = x1 >> 4;
        int maxY = y1 >> 4;
        int maxZ = z1 >> 4;
        boolean hasFluidAndLoaded = false;

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                Column chunk = level.getChunk(x, z);
                if (chunk == null) {
                    return false;
                }

                BaseChunk[] sections = chunk.chunks();

                for (int y = minY; y <= maxY; y++) {
                    int sectionY = y - (level.getMinHeight() >> 4);
                    if (sectionY >= 0 && sectionY < sections.length) {
                        if (HAS_FLUID_COUNT && sections[sectionY] instanceof Chunk_v1_18 target) {
                            hasFluidAndLoaded |= target.getFluidCount() != 0;
                        } else {
                            hasFluidAndLoaded = true;
                        }
                    }
                }
            }
        }

        return hasFluidAndLoaded;
    }

    private EntityFluidInteraction.Tracker getTrackerFor(final FluidTag fluid) {
        return this.trackerByFluid.get(fluid);
    }

    public void applyCurrentTo(final FluidTag fluid, final GrimPlayer entity, final double scale) {
        EntityFluidInteraction.Tracker tracker = this.trackerByFluid.get(fluid);
        if (tracker != null) {
            tracker.applyCurrentTo(entity, scale);
        }
    }

    public double getFluidHeight(final FluidTag fluid) {
        EntityFluidInteraction.Tracker tracker = this.trackerByFluid.get(fluid);
        return tracker != null ? tracker.height : 0.0;
    }

    public boolean isInFluid(final FluidTag fluid) {
        return this.getFluidHeight(fluid) > 0.0;
    }

    public boolean isEyeInFluid(final FluidTag fluid) {
        EntityFluidInteraction.Tracker tracker = this.trackerByFluid.get(fluid);
        return tracker != null && tracker.eyesInside;
    }

    private static class Tracker {
        private double height;
        private boolean eyesInside;
        private Vector3dm accumulatedCurrent = new Vector3dm();
        private int currentCount;

        public void reset() {
            this.height = 0.0;
            this.eyesInside = false;
            this.accumulatedCurrent = new Vector3dm();
            this.currentCount = 0;
        }

        public void accumulateCurrent(final Vector3dm flow) {
            this.accumulatedCurrent = this.accumulatedCurrent.add(flow);
            this.currentCount++;
        }

        public void applyCurrentTo(final GrimPlayer player, final double scale) {
            if (this.currentCount != 0 && !(this.accumulatedCurrent.lengthSquared() < 1.0E-5F)) {
                Vector3dm current;
                if (player.inVehicle()) {
                    current = this.accumulatedCurrent.normalize();
                } else {
                    current = this.accumulatedCurrent.multiply(1.0 / this.currentCount);
                }

                current = current.multiply(scale);
                // Store the vector before handling 0.003, so knockback can use it
                // However, do this after the multiplier, so that we don't have to recompute it
                player.baseTickAddWaterPushing(current);
                if (Math.abs(player.clientVelocity.getX()) < 0.003 && Math.abs(player.clientVelocity.getZ()) < 0.003 && current.length() < 0.0045000000000000005) {
                    current = current.normalize().multiply(0.0045000000000000005);
                }

                player.baseTickAddVector(current);
            }
        }
    }
}
