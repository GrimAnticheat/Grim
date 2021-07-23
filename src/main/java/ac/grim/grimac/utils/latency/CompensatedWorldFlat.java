package ac.grim.grimac.utils.latency;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompensatedWorldFlat extends CompensatedWorld {
    public static final Material WATER = XMaterial.WATER.parseMaterial();
    public static List<BlockData> globalPaletteToBlockData;

    static {
        // The global palette only exists in 1.13+, 1.12- uses magic values for everything
        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", 0);

        BufferedReader paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAC.staticGetResource(XMaterial.getVersion() + ".txt"))));
        int paletteSize = (int) paletteReader.lines().count();
        // Reset the reader after counting
        paletteReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GrimAC.staticGetResource(XMaterial.getVersion() + ".txt"))));

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
                // I could technically generate this on startup but that requires setting blocks in the world
                // Would rather have a known clean file on all servers.
                String blockString = line.substring(line.indexOf(" ") + 1);
                org.bukkit.block.data.BlockData referencedBlockData = Bukkit.createBlockData(blockString);

                // Link this global palette ID to the blockdata for the second part of the script
                globalPaletteToBlockData.add(globalPaletteID, referencedBlockData);
            }
        } catch (IOException e) {
            System.out.println("Palette reading failed! Unsupported version?");
            e.printStackTrace();
        }
    }

    public CompensatedWorldFlat(GrimPlayer player) {
        super(player);
    }

    public static int getFlattenedGlobalID(BlockData blockData) {
        int id = globalPaletteToBlockData.indexOf(blockData);
        return id == -1 ? 0 : id;
    }

    @Override
    public boolean isFluidFalling(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);

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

        if (((FlatBlockState) bukkitBlock).getBlockData() instanceof Levelled && bukkitBlock.getMaterial() == WATER) {
            return ((Levelled) ((FlatBlockState) bukkitBlock).getBlockData()).getLevel() == 0;
        }

        // These blocks are also considered source blocks

        return Materials.checkFlag(bukkitBlock.getMaterial(), player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) ? Materials.WATER_SOURCE : Materials.WATER_SOURCE_LEGACY);
    }

    @Override
    public double getWaterFluidLevelAt(int x, int y, int z) {
        BaseBlockState bukkitBlock = getWrappedBlockStateAt(x, y, z);
        boolean isWater = Materials.isWaterFlat(player.getClientVersion(), bukkitBlock);

        if (!isWater) return 0;

        BaseBlockState aboveData = getWrappedBlockStateAt(x, y + 1, z);

        // If water has water above it, it's block height is 1, even if it's waterlogged
        if (Materials.isWaterFlat(player.getClientVersion(), aboveData)) {
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
