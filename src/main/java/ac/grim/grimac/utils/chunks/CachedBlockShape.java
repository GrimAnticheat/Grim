package ac.grim.grimac.utils.chunks;

import ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShape;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.server.v1_16_R3.*;

import java.lang.reflect.Field;
import java.util.List;

public class CachedBlockShape {
    private static final VoxelShape b = SystemUtils.a(() -> {
        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeBitSet voxelshapebitset = new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeBitSet(1, 1, 1);

        voxelshapebitset.a(0, 0, 0, true, true);
        return new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeCube(voxelshapebitset);
    });
    VoxelShape[] blockShapes;


    public CachedBlockShape() throws NoSuchFieldException, IllegalAccessException {
        RegistryBlockID<IBlockData> REGISTRY_ID = Block.REGISTRY_ID;

        Field field = RegistryBlockID.class.getDeclaredField("c");
        field.setAccessible(true);

        // The index of this list is the block ID
        List<IBlockData> blockDataList = (List<IBlockData>) field.get(REGISTRY_ID);
        blockShapes = new VoxelShape[blockDataList.size()];

        for (int i = 0; i < blockDataList.size(); i++) {
            IBlockData block = blockDataList.get(i);

            // Shulker boxes require reading the world to get bounding box
            if (block.getBlock() instanceof BlockShulkerBox) continue;
            // Requires block position to get bounding box
            if (block.getBlock() instanceof BlockBamboo) continue;
            if (block.getBlock() instanceof BlockBambooSapling) continue;
            if (block.getBlock() instanceof BlockFlowers) continue;

            net.minecraft.server.v1_16_R3.VoxelShape vanillaShape = block.getShape(null, null);

            boolean canCollide = getCanCollideWith(block.getBlock());

            if (canCollide) {
                if (vanillaShape instanceof VoxelShapeArray) {
                    Field b = vanillaShape.getClass().getDeclaredField("b");
                    Field c = vanillaShape.getClass().getDeclaredField("c");
                    Field d = vanillaShape.getClass().getDeclaredField("d");
                    b.setAccessible(true);
                    c.setAccessible(true);
                    d.setAccessible(true);

                    DoubleList bList = (DoubleList) b.get(vanillaShape);
                    DoubleList cList = (DoubleList) c.get(vanillaShape);
                    DoubleList dList = (DoubleList) d.get(vanillaShape);


                    Field a = vanillaShape.getClass().getSuperclass().getDeclaredField("a");
                    a.setAccessible(true);
                    VoxelShapeDiscrete discrete = (VoxelShapeDiscrete) a.get(vanillaShape);


                    // Always instance of VoxelShapeBitSet, at least on 1.16
                    if (discrete instanceof VoxelShapeBitSet) {
                        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeBitSet bits = getBitSet((VoxelShapeBitSet) discrete);

                        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeArray voxelShapeArray = new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeArray(bits, bList, cList, dList, true);
                        blockShapes[i] = voxelShapeArray;
                    } else {
                        new Exception().printStackTrace();
                    }

                } else if (vanillaShape instanceof VoxelShapeCube) {
                    Field bitSet = vanillaShape.getClass().getSuperclass().getDeclaredField("a");
                    bitSet.setAccessible(true);
                    VoxelShapeBitSet nmsBit = (VoxelShapeBitSet) bitSet.get(vanillaShape);

                    ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeBitSet bits = getBitSet(nmsBit);
                } else if (vanillaShape instanceof VoxelShapeSlice) {

                }
            } else {
                blockShapes[i] = b;
            }
        }
    }

    private ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeBitSet getBitSet(VoxelShapeBitSet discrete) throws NoSuchFieldException, IllegalAccessException {
        Field disA = discrete.getClass().getSuperclass().getDeclaredField("a");
        Field disB = discrete.getClass().getSuperclass().getDeclaredField("b");
        Field disC = discrete.getClass().getSuperclass().getDeclaredField("c");

        disA.setAccessible(true);
        disB.setAccessible(true);
        disC.setAccessible(true);

        int intA = disA.getInt(discrete);
        int intB = disB.getInt(discrete);
        int intC = disC.getInt(discrete);

        Field disE = discrete.getClass().getDeclaredField("e");
        Field disF = discrete.getClass().getDeclaredField("f");
        Field disG = discrete.getClass().getDeclaredField("g");
        Field disH = discrete.getClass().getDeclaredField("h");
        Field disI = discrete.getClass().getDeclaredField("i");
        Field disJ = discrete.getClass().getDeclaredField("j");

        disE.setAccessible(true);
        disF.setAccessible(true);
        disG.setAccessible(true);
        disH.setAccessible(true);
        disI.setAccessible(true);
        disJ.setAccessible(true);

        int intE = disE.getInt(discrete);
        int intF = disF.getInt(discrete);
        int intG = disG.getInt(discrete);
        int intH = disH.getInt(discrete);
        int intI = disI.getInt(discrete);
        int intJ = disJ.getInt(discrete);

        return new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeBitSet(intA, intB, intC, intE, intF, intG, intH, intI, intJ);
    }

    public static double[] getSublist(DoubleList list) {
        double[] doubles = new double[2];
        doubles[0] = list.getDouble(0);
        doubles[1] = list.getDouble(1);

        return doubles;
    }

    // TODO: Compile all these values into an array on startup to improve performance
    public static boolean getCanCollideWith(Object object) {
        Class clazz = object.getClass();

        while (clazz != null) {
            try {
                Field canCollide = clazz.getDeclaredField("at");
                canCollide.setAccessible(true);

                return canCollide.getBoolean(object);
            } catch (NoSuchFieldException | IllegalAccessException noSuchFieldException) {
                clazz = clazz.getSuperclass();
            }
        }

        // We should always be able to get a field
        new Exception().printStackTrace();
        return false;
    }
}
