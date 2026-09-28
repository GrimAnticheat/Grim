package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.tags.SyncedTags;
import ac.grim.grimac.utils.data.tags.SyncedTag;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.FluidTypeFlowing;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.generation.fluids.Fluid;
import com.github.retrooper.packetevents.protocol.world.generation.fluids.Fluids;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EntityFluidInteraction {
    private final GrimPlayer player;
    private final EnumMap<FluidTag, EntityFluidInteraction.Tracker> trackerByFluid = new EnumMap<>(FluidTag.class);
    private final Map<Fluid, Tracker> fluidTrackers = new HashMap<>();
    private final Map<FluidTag, CurrentAccumulator> currentAccumulators = new LinkedHashMap<>();

    public EntityFluidInteraction(final GrimPlayer player, final FluidTag... fluids) {
        this.player = player;
        for (FluidTag tagKey : fluids) {
            this.trackerByFluid.put(tagKey, new EntityFluidInteraction.Tracker());
            this.currentAccumulators.put(tagKey, new CurrentAccumulator());
        }
    }

    public void update(final GrimPlayer player, final boolean ignoreCurrent) {
        this.trackerByFluid.values().forEach(EntityFluidInteraction.Tracker::reset);
        this.fluidTrackers.values().removeIf(Tracker::reset);
        this.currentAccumulators.values().forEach(CurrentAccumulator::reset);

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

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    float fluidHeight = player.compensatedWorld.getFluidLevelAt(x, y, z);
                    if (fluidHeight == 0) {
                        continue;
                    }

                    double fluidHeightToWorld = (double) y + fluidHeight;
                    if (fluidHeightToWorld < aabb.minY) {
                        continue;
                    }

                    WrappedBlockState block = player.compensatedWorld.getBlock(x, y, z);
                    FluidTag newFluid = Materials.isWater(player.getClientVersion(), block) ? FluidTag.WATER : FluidTag.LAVA;

                    Tracker tracker;
                    CurrentAccumulator accumulator = null;
                    if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3)) {
                        Fluid fluidType = newFluid == FluidTag.WATER
                                ? (Materials.isWaterSource(player.getClientVersion(), block) ? Fluids.WATER : Fluids.FLOWING_WATER)
                                : (block.getLevel() == 0 ? Fluids.LAVA : Fluids.FLOWING_LAVA);

                        tracker = this.fluidTrackers.computeIfAbsent(fluidType, ignored -> new Tracker());
                        if (!ignoreCurrent) {
                            for (Map.Entry<FluidTag, CurrentAccumulator> entry : this.currentAccumulators.entrySet()) {
                                if (getTag(entry.getKey()).contains(fluidType)) {
                                    accumulator = entry.getValue();
                                    break;
                                }
                            }
                        }
                    } else {
                        tracker = this.getTrackerFor(newFluid);
                        if (!ignoreCurrent) accumulator = this.currentAccumulators.get(newFluid);
                    }

                    if (tracker != null) {
                        if (x == playerX && z == playerZ && playerEyeY >= (double) y) {
                            tracker.eyesInside |= playerEyeY <= getFluidTopForCamera(player, block, newFluid, x, y, z, fluidHeightToWorld);
                        }

                        tracker.height = Math.max(fluidHeightToWorld - aabbMinY, tracker.height);
                        if (accumulator != null) {
                            Vector3dm current = FluidTypeFlowing.getFlow(player, x, y, z);
                            accumulator.height = Math.max(tracker.height, accumulator.height);
                            if (accumulator.height < 0.4) {
                                current = current.multiply(accumulator.height);
                            }

                            accumulator.accumulateCurrent(current);
                        }
                    }
                }
            }
        }
    }

    private static final boolean HAS_FLUID_COUNT = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_26_1);

    private static double getFluidTopForCamera(GrimPlayer player, WrappedBlockState block, FluidTag fluid, int x, int y, int z, double fluidTop) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3)
                && (fluid == FluidTag.WATER ? Materials.isWaterSource(player.getClientVersion(), block) : block.getLevel() == 0)
                && hasSolidBottom(player, x, y + 1, z)) {
            return y + 1.0;
        }

        return fluidTop;
    }

    private static boolean hasSolidBottom(GrimPlayer player, int x, int y, int z) {
        WrappedBlockState state = player.compensatedWorld.getBlock(x, y, z);
        StateType type = state.getType();

        if (BlockTags.LEAVES.contains(type) || type == StateTypes.CHORUS_FLOWER || type == StateTypes.SCAFFOLDING) return false;
        if (type == StateTypes.SNOW) return true;

        if (Materials.isShulker(type)) {
            for (ShulkerData shulker : player.compensatedWorld.openShulkerBoxes) {
                if (shulker.blockPos != null && shulker.blockPos.x == x && shulker.blockPos.y == y && shulker.blockPos.z == z) {
                    return state.getFacing() == BlockFace.UP;
                }
            }
            return true;
        }

        SimpleCollisionBox[] boxes = new SimpleCollisionBox[ComplexCollisionBox.DEFAULT_MAX_COLLISION_BOX_SIZE];
        int count = CollisionData.getData(state.getType()).getMovementCollisionBox(player, player.getClientVersion(), state).downCast(boxes);
        for (int i = 0; i < count; i++) {
            SimpleCollisionBox box = boxes[i];
            if (box.minY == 0.0 && box.minX == 0.0 && box.minZ == 0.0 && box.maxX == 1.0 && box.maxZ == 1.0) {
                return true;
            }
        }

        return false;
    }

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
        CurrentAccumulator accumulator = this.currentAccumulators.get(fluid);
        if (accumulator != null) {
            accumulator.applyCurrentTo(entity, scale);
        }
    }

    public double getFluidHeight(final FluidTag fluid) {
        if (this.player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3)) return getFluidHeight(getTag(fluid));
        EntityFluidInteraction.Tracker tracker = this.trackerByFluid.get(fluid);
        return tracker != null ? tracker.height : 0.0;
    }

    public boolean isInFluid(final FluidTag fluid) {
        return this.getFluidHeight(fluid) > 0.0;
    }

    public boolean isEyeInFluid(final FluidTag fluid) {
        if (this.player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3)) return isEyeInFluid(getTag(fluid));
        EntityFluidInteraction.Tracker tracker = this.trackerByFluid.get(fluid);
        return tracker != null && tracker.eyesInside;
    }

    private SyncedTag<Fluid> getTag(FluidTag fluid) {
        return this.player.tagManager.fluid(fluid == FluidTag.WATER ? SyncedTags.WATER : SyncedTags.LAVA);
    }

    public double getFluidHeight(SyncedTag<Fluid> tag) {
        double height = 0.0;
        for (Map.Entry<Fluid, Tracker> entry : this.fluidTrackers.entrySet()) {
            if (tag.contains(entry.getKey())) height = Math.max(height, entry.getValue().height);
        }
        return height;
    }

    public boolean isEyeInFluid(SyncedTag<Fluid> tag) {
        for (Map.Entry<Fluid, Tracker> entry : this.fluidTrackers.entrySet()) {
            if (entry.getValue().eyesInside && tag.contains(entry.getKey())) return true;
        }
        return false;
    }

    private static class Tracker {
        private double height;
        private boolean eyesInside;

        public boolean reset() {
            boolean inactive = this.height == 0.0 && !this.eyesInside;
            this.height = 0.0;
            this.eyesInside = false;
            return inactive;
        }
    }

    private static class CurrentAccumulator {
        private double height;
        private Vector3dm accumulatedCurrent = new Vector3dm();
        private int currentCount;

        public void reset() {
            this.height = 0.0;
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
                    current = VectorUtils.normalize(ClientVersion.V_26_1, this.accumulatedCurrent);
                } else {
                    current = this.accumulatedCurrent.multiply(1.0 / this.currentCount);
                }

                current = current.multiply(scale);
                // Store the vector before handling 0.003, so knockback can use it
                // However, do this after the multiplier, so that we don't have to recompute it
                player.baseTickAddWaterPushing(current);
                if (Math.abs(player.clientVelocity.getX()) < 0.003 && Math.abs(player.clientVelocity.getZ()) < 0.003 && current.length() < 0.0045000000000000005) {
                    current = VectorUtils.normalize(ClientVersion.V_26_1, current).multiply(0.0045000000000000005);
                }

                player.baseTickAddVector(current);
            }
        }
    }
}
