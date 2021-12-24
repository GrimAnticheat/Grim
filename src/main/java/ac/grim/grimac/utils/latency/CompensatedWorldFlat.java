package ac.grim.grimac.utils.latency;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompensatedWorldFlat extends CompensatedWorld {
    private static final Material WATER = ItemTypes.WATER;
    private static final Material CAULDRON = ItemTypes.CAULDRON;
    public static List<BlockData> globalPaletteToBlockData;

    public CompensatedWorldFlat(GrimPlayer player) {
        super(player);
    }

    public static void init() {
        // The global palette only exists in 1.13+, 1.12- uses magic values for everything
        int version = ItemTypes.getVersion();
        // 1.18 and 1.17 have the same palette... no need to duplicate it
        if (version == 18) version = 17;

        BufferedReader paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAPI.INSTANCE.getPlugin().getResource(version + ".txt"))));
        int paletteSize = (int) paletteReader.lines().count();
        // Reset the reader after counting
        paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAPI.INSTANCE.getPlugin().getResource(version + ".txt"))));

        globalPaletteToBlockData = new ArrayList<>(paletteSize);

        String line;

        try {
            while ((line = paletteReader.readLine()) != null) {
                // Example line:
                // 109 minecraft:oak_wood[axis=x]
                String number = line.substring(0, line.indexOf(" "));

                // This is the integer used when sending chunks
                int globalPaletteID = Integer.parseInt(number);

                // This is the string saved from the block
                // Generated with a script - https://gist.github.com/MWHunter/b16a21045e591488354733a768b804f4
                // (Currently the files are from ViaVersion as their method is more accurate)
                //
                // I could technically generate this on startup but that requires setting blocks in the world
                // Would rather have a known clean file on all servers.
                String blockString = line.substring(line.indexOf(" ") + 1);
                org.bukkit.block.data.BlockData referencedBlockData = Bukkit.createBlockData(blockString);

                // Link this global palette ID to the blockdata for the second part of the script
                globalPaletteToBlockData.add(globalPaletteID, referencedBlockData);

                // We need to pre-compute all the block data stuff on bukkit main thread
                // This is to avoid a rare ConcurrentModificationException while bukkit goes from NMS -> Bukkit
                //
                // Caused by: java.util.ConcurrentModificationException
                //    at java.base/java.util.HashMap.computeIfAbsent(HashMap.java:1225)
                //    at org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData.toBukkit(CraftBlockData.java:154)
                //    at org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData.get(CraftBlockData.java:64)
                //    at org.bukkit.craftbukkit.v1_17_R1.block.impl.CraftWeatheringCopperSlab.getType(CraftWeatheringCopperSlab.java:22)
                //
                // This is an error only in Java 9+, as in Java 8, there is a bug where a CME is not called on putIfAbsent
                Method[] methods = referencedBlockData.getClass().getDeclaredMethods();
                for (Method method : methods) {
                    if (isGetter(method)) {
                        try {
                            // Clone just to avoid any accidental changes to this
                            method.invoke(referencedBlockData.clone());
                        } catch (InvocationTargetException | IllegalAccessException ignored) {
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Palette reading failed! Unsupported version?");
            e.printStackTrace();
        }
    }

    // https://asgteach.com/2012/11/finding-getters-and-setters-with-java-reflection/
    private static boolean isGetter(Method method) {
        if (Modifier.isPublic(method.getModifiers()) &&
                method.getParameterTypes().length == 0) {
            if (method.getName().matches("^get[A-Z].*") &&
                    !method.getReturnType().equals(void.class))
                return true;
            return method.getName().matches("^is[A-Z].*") &&
                    method.getReturnType().equals(boolean.class);
        }
        return false;
    }

    @Override
    public void tickOpenable(int blockX, int blockY, int blockZ) {
        FlatBlockState data = (FlatBlockState) player.compensatedWorld.getWrappedBlockStateAt(blockX, blockY, blockZ);

        if (data.getBlockData() instanceof Door) {
            Door door = (Door) data.getBlockData();
            FlatBlockState otherDoorState = (FlatBlockState) player.compensatedWorld.getWrappedBlockStateAt(blockX, blockY + (door.getHalf() == Bisected.Half.BOTTOM ? 1 : -1), blockZ);

            // Add the other door part to the likely to desync positions
            player.compensatedWorld.likelyDesyncBlockPositions.add(new Pair<>(player.lastTransactionSent.get(), new Vector3i(blockX, blockY + (door.getHalf() == Bisected.Half.BOTTOM ? 1 : -1), blockZ)));

            if (otherDoorState.getBlockData() instanceof Door) {
                Door otherDoor = (Door) otherDoorState.getBlockData().clone();
                otherDoor.setOpen(!otherDoor.isOpen());
                player.compensatedWorld.updateBlock(blockX, blockY + (door.getHalf() == Bisected.Half.BOTTOM ? 1 : -1), blockZ, getFlattenedGlobalID(otherDoor));
            }
        }

        if (data.getBlockData() instanceof Openable) {
            // Do NOT change the getBlockData() without cloning otherwise you will corrupt the (grim) global palette!
            Openable openable = (Openable) data.getBlockData().clone();
            openable.setOpen(!openable.isOpen());
            player.compensatedWorld.updateBlock(blockX, blockY, blockZ, getFlattenedGlobalID(openable));
        }
    }

    public static int getFlattenedGlobalID(BlockData blockData) {
        int id = globalPaletteToBlockData.indexOf(blockData);
        return id == -1 ? 0 : id;
    }

    @Override
    public boolean isFluidFalling(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

        // Cauldrons are technically levelled blocks
        if (Materials.checkFlag(bukkitBlock.getMaterial(), Materials.CAULDRON)) return false;
        if (((FlatBlockState) bukkitBlock).getBlockData() instanceof Levelled) {
            return ((Levelled) ((FlatBlockState) bukkitBlock).getBlockData()).getLevel() > 7;
        }

        return false;
    }

    @Override
    public double getLavaFluidLevelAt(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

        if (!Materials.checkFlag(bukkitBlock.getMaterial(), Materials.LAVA)) return 0;

        BaseBlockState aboveData = getWrappedBlockStateAt(x, y + 1, z);

        if (Materials.checkFlag(aboveData.getMaterial(), Materials.LAVA)) {
            return 1;
        }

        BlockData thisBlockData = ((FlatBlockState) bukkitBlock).getBlockData();

        if (thisBlockData instanceof Levelled) {
            // Falling lava has a level of 8
            if (((Levelled) thisBlockData).getLevel() >= 8) return 8 / 9f;

            return (8 - ((Levelled) thisBlockData).getLevel()) / 9f;
        }

        return 0;
    }

    @Override
    public boolean isWaterSourceBlock(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

        if (bukkitBlock.getMaterial() == WATER && ((FlatBlockState) bukkitBlock).getBlockData() instanceof Levelled) {
            return ((Levelled) ((FlatBlockState) bukkitBlock).getBlockData()).getLevel() == 0;
        }

        // Waterlogged blocks are always source blocks
        if (Materials.isWater(player.getClientVersion(), bukkitBlock)) {
            return true;
        }

        // These blocks are also considered source blocks (kelp, bubble column, etc.)
        return Materials.checkFlag(bukkitBlock.getMaterial(), player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) ? Materials.WATER_SOURCE : Materials.WATER_SOURCE_LEGACY);
    }

    @Override
    public double getWaterFluidLevelAt(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        boolean isWater = Materials.isWater(player.getClientVersion(), bukkitBlock);

        if (!isWater) return 0;

        BaseBlockState aboveData = getWrappedBlockStateAt(x, y + 1, z);

        // If water has water above it, it's block height is 1, even if it's waterlogged
        if (Materials.isWater(player.getClientVersion(), aboveData)) {
            return 1;
        }

        FlatBlockState flatBlockState = (FlatBlockState) bukkitBlock;

        if (flatBlockState.getBlockData() instanceof Levelled) {
            if (bukkitBlock.getMaterial() == WATER) {
                int waterLevel = ((Levelled) flatBlockState.getBlockData()).getLevel();

                // Falling water has a level of 8
                if (waterLevel >= 8) return 8 / 9f;

                return (8 - waterLevel) / 9f;
            }
        }

        // The block is water, isn't water material directly, and doesn't have block above, so it is waterlogged
        // or another source-like block such as kelp.
        return 8 / 9F;
    }
}
