package me.grim.bench;

import ac.grim.grimac.player.GrimPlayer;
import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.EventCreationUtil;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.netty.buffer.ByteBuf;
import me.grim.bench.blockchange.AbstractBenchmarkBlockChangeHandler;
import me.grim.bench.blockchange.multi_block_bit_repack.MultiBLockChangeBitRepack;
import me.grim.bench.blockchange.multi_block_change_as_int_array.MultiBlockChangeIntArrayBlockChangeHandler;
import me.grim.bench.blockchange.multi_block_change_long_pack.MultiBlockChangeLongPackArrayBlockChangeHandler;
import me.grim.bench.blockchange.no_waste_bit_packing.MultiBlockChangeNoWasteBitPack;
import me.grim.bench.blockchange.original.OriginalBlockChangeHandler;
import me.grim.bench.blockchange.original.OriginalLatencyUtils;
import me.grim.bench.blockchange.original_low_hanging_fruit.LowHangingFruitBlockChangeHandler;
import me.grim.bench.blockchange.original_low_hanging_fruit.LowHangingFruitLatencyUtils;
import me.grim.bench.blockchange.unsafe_no_waste_bit_packing.V1200MultiBlockChangeUnsafeHandler;
import me.grim.bench.blockchange.unsafe_no_waste_bit_packing.V1200MultiBlockChangeUnsafeNoWasteBitPackingHandler;
import me.grim.bench.setup.MockFactory;
import me.grim.bench.setup.TestPacketEventsBuilder;
import me.grim.bench.util.HeapSize;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class QuickMemProbe {

    private final GrimPlayer player;
    private final AbstractBenchmarkBlockChangeHandler blockChangeBenchmark;

    private static final int NUM_BLOCKS_IN_MULTI_BLOCK_UPDATE = 2;

    private static final Random RNG = new Random(1234L);
    private static final int MAX_STATE_ID = 26_000;   // vanilla 1.21.4 = 26884

    enum HandlerType { ORIGINAL, LOW_HANGING_FRUIT, MULTI_BLOCK_INT_ARRAY, MULTI_BLOCK_LONG_PACK, MULTI_BLOCK_BIT_REPACK, MULTI_BLOCK_NO_WASTE_BIT_REPACK, MULTI_BLOCK_UNSAFE_NO_WASTE_BIT_REPACK }

    /* ------------------ two quick ctors ------------------ */
    public QuickMemProbe()          { this(HandlerType.ORIGINAL); }         // default = original
    public QuickMemProbe(HandlerType handlerType) {
        player = MockFactory.newMockPlayer();
        MockFactory.addMockPlayer(player.user, player);
        MockFactory.set(player, "latencyUtils", switch (handlerType) {
                    case ORIGINAL -> new OriginalLatencyUtils(player);
                    case LOW_HANGING_FRUIT -> new LowHangingFruitLatencyUtils(player);
                    case MULTI_BLOCK_INT_ARRAY -> new LowHangingFruitLatencyUtils(player);
                    case MULTI_BLOCK_LONG_PACK ->  new LowHangingFruitLatencyUtils(player);
                    case MULTI_BLOCK_BIT_REPACK -> new LowHangingFruitLatencyUtils(player);
                    case MULTI_BLOCK_NO_WASTE_BIT_REPACK -> new LowHangingFruitLatencyUtils(player);
            case MULTI_BLOCK_UNSAFE_NO_WASTE_BIT_REPACK -> new LowHangingFruitLatencyUtils(player);
                }
        );

        blockChangeBenchmark = switch (handlerType) {
            case ORIGINAL -> new OriginalBlockChangeHandler();
            case LOW_HANGING_FRUIT -> new LowHangingFruitBlockChangeHandler();
            case MULTI_BLOCK_INT_ARRAY -> new MultiBlockChangeIntArrayBlockChangeHandler();
            case MULTI_BLOCK_LONG_PACK -> new MultiBlockChangeLongPackArrayBlockChangeHandler();
            case MULTI_BLOCK_BIT_REPACK -> new MultiBLockChangeBitRepack();
            case MULTI_BLOCK_NO_WASTE_BIT_REPACK -> new MultiBlockChangeNoWasteBitPack();
            case MULTI_BLOCK_UNSAFE_NO_WASTE_BIT_REPACK -> new V1200MultiBlockChangeUnsafeNoWasteBitPackingHandler();
        };
        player.lastTransactionReceived.decrementAndGet(); // To runnable from being executed immediately
    }


    private void addSingleBlockPayload() {
        WrapperPlayServerBlockChange playServerBlockChange = new WrapperPlayServerBlockChange(new Vector3i(0, 0, 0), 0);
        playServerBlockChange.prepareForSend(player.user.getChannel(), false, false);
        PacketSendEvent packetSendEvent = EventCreationUtil.createSendEvent(player.user.getChannel(), player.user, null, playServerBlockChange.buffer, false);
        blockChangeBenchmark.onPacketSend(packetSendEvent);
        ((ByteBuf) packetSendEvent.getByteBuf()).release();
    }

    /* -----------------------------------------------------------
     * Generates a Multi-Block-Change packet with 100 random records
     * all inside section (0,0,0)
     * ----------------------------------------------------------- */
    private void addMultiBlockPayload() {

        WrapperPlayServerMultiBlockChange.EncodedBlock[] encoded =
                new WrapperPlayServerMultiBlockChange.EncodedBlock[NUM_BLOCKS_IN_MULTI_BLOCK_UPDATE];

        for (int i = 0; i < encoded.length; i++) {

            int stateId = RNG.nextInt(MAX_STATE_ID);

            /* local coordinates 0-15 so they stay in the same section */
            int localX  = RNG.nextInt(16);
            int localY  = RNG.nextInt(16);
            int localZ  = RNG.nextInt(16);

            encoded[i] = new WrapperPlayServerMultiBlockChange.EncodedBlock(
                    stateId,
                /* global X/Y/Z = sectionOrigin + localCoord
                   sectionOrigin is (0,0,0) in this benchmark */
                    localX,
                    localY,
                    localZ
            );
        }

        /* assemble the actual packet (section position (0,0,0), trustEdges = null) */
        WrapperPlayServerMultiBlockChange pkt =
                new WrapperPlayServerMultiBlockChange(new Vector3i(0, 0, 0),
                        null,
                        encoded);

        /* hand it to PacketEvents / our handler, same as before */
        pkt.prepareForSend(player.user.getChannel(), false, false);

        PacketSendEvent pse = EventCreationUtil.createSendEvent(
                player.user.getChannel(), player.user, null, pkt.buffer, false);

        blockChangeBenchmark.onPacketSend(pse);

        ((ByteBuf) pse.getByteBuf()).release();  // release Netty buf
    }

    /* ------------------ drive some load ------------------ */
    public void fire(int count) {
        for (int i = 0; i < count; i++) {
//            util.addRealTimeTask(i, () -> {});               // no-op tasks
//            addSingleBlockPayload();
            addMultiBlockPayload();
        }
    }

    /* ------------------ dump footprint ------------------- */
    public void dump() {
        System.out.println(HeapSize.bytes(player.latencyUtils.getTasksObject()));
//        System.out.println(GraphLayout.parseInstance(player.latencyUtils.getTasksObject()).toFootprint());
    }

    /* ------------------ quick & dirty main --------------- */
    public static void main(String[] args) {
        if (!MockBukkit.isMocked()) MockBukkit.mock();
        Plugin p = MockBukkit.createMockPlugin("pe_global");
        PacketEvents.setAPI(TestPacketEventsBuilder.build(p));
        PacketType.prepare();

        WrappedBlockState.getByGlobalId(1);

        int N = 100_000;                                     // tasks to add

        System.out.println("=== OriginalLatencyUtils ===");
        QuickMemProbe orig = new QuickMemProbe();
        orig.fire(N);
        orig.dump();
        orig.cleanup();

        System.out.println("\n=== LowHangingFruitLatencyUtils ===");
        QuickMemProbe low  = new QuickMemProbe(HandlerType.LOW_HANGING_FRUIT);
        low.fire(N);
        low.dump();
        low.cleanup();

        System.out.println("\n=== MultiBlockChangeIntArray ===");
        QuickMemProbe multiBlockIntArray  = new QuickMemProbe(HandlerType.MULTI_BLOCK_INT_ARRAY);
        multiBlockIntArray.fire(N);
        multiBlockIntArray.dump();
        multiBlockIntArray.cleanup();

        System.out.println("\n=== MultiBlockLongPack ===");
        QuickMemProbe multiBlockLongPack  = new QuickMemProbe(HandlerType.MULTI_BLOCK_LONG_PACK);
        multiBlockLongPack.fire(N);
        multiBlockLongPack.dump();
        multiBlockLongPack.cleanup();

        System.out.println("\n=== MultiBlockLBitRepack ===");
        QuickMemProbe multiBlockBitRepack  = new QuickMemProbe(HandlerType.MULTI_BLOCK_BIT_REPACK);
        multiBlockBitRepack.fire(N);
        multiBlockBitRepack.dump();
        multiBlockBitRepack.cleanup();

        System.out.println("\n=== MultiBlockNoWasteBitRepack ===");
        QuickMemProbe multiBlockNoWasteBitRepack = new QuickMemProbe(HandlerType.MULTI_BLOCK_NO_WASTE_BIT_REPACK);
        multiBlockNoWasteBitRepack.fire(N);
        multiBlockNoWasteBitRepack.dump();
        multiBlockNoWasteBitRepack.cleanup();

        System.out.println("\n=== MultiBlockUnsafeNoWasteBitRepack ===");
        QuickMemProbe probe = new QuickMemProbe(
                HandlerType.MULTI_BLOCK_UNSAFE_NO_WASTE_BIT_REPACK /* or UNSAFE */);

        probe.fire(N);

        /* heap bytes */
        long heap = HeapSize.bytes(probe.player.latencyUtils.getTasksObject());

        /* native bytes (only for the Unsafe handler) */
        long off  = V1200MultiBlockChangeUnsafeHandler.getNativeBytes();

        System.out.println("heap  = " + heap + " B");
        System.out.println("native= " + off  + " B");
        System.out.println("total = " + (heap + off) + " B");

    }

    private void cleanup() {
        MockFactory.releaseMockPlayer(player);
    }
}