package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import net.minecraft.server.v1_16_R3.*;

import java.util.Iterator;

import static net.minecraft.server.v1_16_R3.FluidTypeFlowing.FALLING;

public class FluidTypeFlowing {
    public static Vec3D getFlow(GrimPlayer player, BlockPosition blockposition, Fluid fluid) {
        // Only do this for flowing liquids
        if (fluid.getType() instanceof FluidTypeEmpty) return Vec3D.ORIGIN;

        double d0 = 0.0D;
        double d1 = 0.0D;
        BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();
        Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();


        while (iterator.hasNext()) {
            EnumDirection enumdirection = (EnumDirection) iterator.next();
            position.a(blockposition, enumdirection);
            Fluid fluid1 = player.compensatedWorld.getBlockDataAt(position.getX(), position.getY(), position.getZ()).getFluid();
            if (affectsFlow(fluid1, fluid.getType())) {
                float f = fluid1.d(); // getOwnHeight
                float f1 = 0.0F;
                if (f == 0.0F) {
                    if (!player.compensatedWorld.getBlockDataAt(position.getX(), position.getY(), position.getZ()).getMaterial().isSolid()) {
                        BlockPosition blockposition1 = position.down();
                        Fluid fluid2 = player.compensatedWorld.getBlockDataAt(blockposition1.getX(), blockposition1.getY(), blockposition1.getZ()).getFluid();
                        if (affectsFlow(fluid1, fluid.getType())) {
                            f = fluid2.d();
                            if (f > 0.0F) {
                                f1 = fluid.d() - (f - 0.8888889F);
                            }
                        }
                    }
                } else if (f > 0.0F) {
                    f1 = fluid.d() - f;
                }

                if (f1 != 0.0F) {
                    d0 += (float) enumdirection.getAdjacentX() * f1;
                    d1 += (float) enumdirection.getAdjacentZ() * f1;
                }
            }
        }

        Vec3D vec3d = new Vec3D(d0, 0.0D, d1);

        if (fluid.get(FALLING)) {
            for (EnumDirection enumdirection1 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                position.a(blockposition, enumdirection1);
                if (isSolidFace(player, position, enumdirection1, fluid.getType()) || isSolidFace(player, position.up(), enumdirection1, fluid.getType())) {
                    vec3d = vec3d.d().add(0.0D, -6.0D, 0.0D);
                    break;
                }
            }
        }

        return vec3d.d();
    }

    private static boolean affectsFlow(Fluid fluid, FluidType fluid2) {
        return fluid.isEmpty() || fluid.getType().a(fluid2);
    }

    // Check if both are a type of water or both are a type of lava
    public static boolean isSame(FluidType fluid1, FluidType fluid2) {
        return fluid1 == FluidTypes.FLOWING_WATER || fluid1 == FluidTypes.WATER &&
                fluid2 == FluidTypes.FLOWING_WATER || fluid2 == FluidTypes.WATER ||
                fluid1 == FluidTypes.FLOWING_LAVA || fluid1 == FluidTypes.LAVA &&
                fluid2 == FluidTypes.FLOWING_LAVA || fluid2 == FluidTypes.LAVA;
    }

    // I believe this is safe to do async??
    // Should spit out a stacktrace if it isn't.
    protected static boolean isSolidFace(GrimPlayer player, BlockPosition blockposition, EnumDirection enumdirection, FluidType fluidType) {
        IBlockData blockState = player.compensatedWorld.getBlockDataAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
        Fluid fluidState = player.compensatedWorld.getBlockDataAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()).getFluid();

        if (isSame(fluidState.getType(), fluidType)) {
            return false;
        } else if (enumdirection == EnumDirection.UP) {
            return true;
        } else {
            // Short circuit out getting block collision for shulker boxes, as they read the world sync
            return blockState.getMaterial() != Material.ICE && blockState.getBlock() instanceof BlockShulkerBox || blockState.d(null, blockposition, enumdirection);
        }
    }
}
