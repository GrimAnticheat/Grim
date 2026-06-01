package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.fabric.utils.convert.FabricIntermediaryConversionUtil;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.PistonData;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(PistonBaseBlock.class)
public class FabricIntermediaryPistonBaseBlockMixin {

    private static final double MAX_HORIZONTAL_DISTANCE = 24.0;
    private static final double MAX_VERTICAL_DISTANCE = 64.0;


    private static boolean isCloseEnough(int ax, int ay, int az, double bx, double by, double bz) {
        return Math.abs(ax - bx) <= MAX_HORIZONTAL_DISTANCE
                && Math.abs(ay - by) <= MAX_VERTICAL_DISTANCE
                && Math.abs(az - bz) <= MAX_HORIZONTAL_DISTANCE;
    }

    @Redirect(method = "moveBlocks",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;resolve()Z"))
    private boolean grimac$onPistonResolve(PistonStructureResolver resolver,
                                           Level level, BlockPos pistonPos, Direction direction, boolean extending) {
        boolean resolved = resolver.resolve();
        if (resolved) {
            handlePiston(resolver, level, pistonPos, direction, extending);
        }
        return resolved;
    }

    private static void handlePiston(PistonStructureResolver resolver, Level level,
                                     BlockPos pistonPos, Direction direction, boolean extending) {
        boolean hasSlimeBlock = false;
        boolean hasHoneyBlock = false;

        List<SimpleCollisionBox> boxes = new ArrayList<>();

        int dx = direction.getStepX();
        int dy = direction.getStepY();
        int dz = direction.getStepZ();

        for (BlockPos blockPos : resolver.getToPush()) {
            int bx = blockPos.getX();
            int by = blockPos.getY();
            int bz = blockPos.getZ();

            boxes.add(new SimpleCollisionBox(bx, by, bz, bx + 1, by + 1, bz + 1, true));
            boxes.add(new SimpleCollisionBox(bx + dx, by + dy, bz + dz,
                    bx + dx + 1, by + dy + 1, bz + dz + 1, true));

            BlockState state = level.getBlockState(blockPos);
            if (state.is(Blocks.SLIME_BLOCK)) hasSlimeBlock = true;
            if (state.is(Blocks.HONEY_BLOCK)) hasHoneyBlock = true;
        }

        if (extending || resolver.getToPush().isEmpty()) {
            BlockPos head = pistonPos.relative(direction);
            int hx = head.getX(), hy = head.getY(), hz = head.getZ();
            boxes.add(new SimpleCollisionBox(hx, hy, hz, hx + 1, hy + 1, hz + 1, true));
        }

        final int chunkX = pistonPos.getX() >> 4;
        final int chunkZ = pistonPos.getZ() >> 4;
        final BlockFace blockFace = FabricIntermediaryConversionUtil.fromDirection(direction);
        final int px = pistonPos.getX(), py = pistonPos.getY(), pz = pistonPos.getZ();

        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            var pos = player.compensatedEntities.self.trackedServerPosition.getPos();
            if (isCloseEnough(px, py, pz, pos.getX(), pos.getY(), pos.getZ()) && player.compensatedWorld.isChunkLoaded(chunkX, chunkZ)) {
                int lastTrans = player.lastTransactionSent.get();
                PistonData data = new PistonData(blockFace, boxes, lastTrans, extending, hasSlimeBlock, hasHoneyBlock);
                player.latencyUtils.addRealTimeTaskAsync(lastTrans, () -> player.compensatedWorld.activePistons.add(data));
            }
        }
    }
}
