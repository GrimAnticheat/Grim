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


                    if (discrete instanceof VoxelShapeBitSet) {
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

                        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeBitSet bits = new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeBitSet(intA, intB, intC, intE, intF, intG, intH, intI, intJ);

                        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeArray voxelShapeArray = new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeArray(bits, bList, cList, dList, true);
                        blockShapes[i] = voxelShapeArray;
                    }

                    // This code isn't ever used???
                    if (discrete instanceof VoxelShapeDiscreteSlice) {
                        Field d2 = discrete.getClass().getDeclaredField("d");
                        Field e2 = discrete.getClass().getDeclaredField("e");
                        Field f2 = discrete.getClass().getDeclaredField("f");
                        Field g2 = discrete.getClass().getDeclaredField("g");
                        Field h2 = discrete.getClass().getDeclaredField("h");
                        Field i2 = discrete.getClass().getDeclaredField("i");
                        Field j2 = discrete.getClass().getDeclaredField("j");

                        VoxelShapeDiscrete d3 = (VoxelShapeDiscrete) d2.get(discrete);

                        Field a4 = d3.getClass().getDeclaredField("a");
                        Field b4 = d3.getClass().getDeclaredField("b");
                        Field c4 = d3.getClass().getDeclaredField("c");

                        int a5 = a4.getInt(d3);
                        int b5 = b4.getInt(d3);
                        int c5 = c4.getInt(d3);
                        //ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeDiscrete dis = new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeDiscrete(a5, b5, c5);

                        int e3 = e2.getInt(discrete);
                        int f3 = f2.getInt(discrete);
                        int g3 = g2.getInt(discrete);
                        int h3 = h2.getInt(discrete);
                        int i3 = i2.getInt(discrete);
                        int j3 = j2.getInt(discrete);

                        //ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeDiscreteSlice slice = new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.VoxelShapeDiscreteSlice(d3, e3, f3, g3, h3, i3, j3);
                    }

                    /*Field disA = discrete.getClass().getDeclaredField("a");
                    Field disB = discrete.getClass().getDeclaredField("b");
                    Field disC = discrete.getClass().getDeclaredField("c");
                    disA.setAccessible(true);
                    disB.setAccessible(true);
                    disC.setAccessible(true);

                    int intA = disA.getInt(discrete);
                    int intB = disB.getInt(discrete);
                    int intC = disC.getInt(discrete);*/

                    //new VoxelShapeArray();

                } else if (vanillaShape instanceof VoxelShapeCube) {

                } else if (vanillaShape instanceof VoxelShapeSlice) {

                }
            } else {
                blockShapes[i] = b;
            }
        }
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
