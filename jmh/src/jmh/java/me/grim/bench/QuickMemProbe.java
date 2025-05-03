package me.grim.bench;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.ILatencyUtils;
import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.EventCreationUtil;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.netty.buffer.ByteBuf;
import me.grim.bench.blockchange.AbstractBenchmarkBlockChangeHandler;
import me.grim.bench.blockchange.AbstractBlockChangeBenchmark;
import me.grim.bench.blockchange.multi_block_change_as_int_array.MultiBlockChangeIntArrayBlockChangeHandler;
import me.grim.bench.blockchange.original.OriginalBlockChangeHandler;
import me.grim.bench.blockchange.original.OriginalLatencyUtils;
import me.grim.bench.blockchange.original_low_hanging_fruit.LowHangingFruitBlockChangeHandler;
import me.grim.bench.blockchange.original_low_hanging_fruit.LowHangingFruitLatencyUtils;
import me.grim.bench.setup.MockFactory;
import me.grim.bench.setup.TestPacketEventsBuilder;
import org.bukkit.plugin.Plugin;
import org.openjdk.jol.info.GraphLayout;

public class QuickMemProbe {

    private final GrimPlayer player;
    private final AbstractBenchmarkBlockChangeHandler blockChangeBenchmark;

    enum HandlerType { ORIGINAL, LOW_HANGING_FRUIT, MULTI_BLOCK_INT_ARRAY }

    /* ------------------ two quick ctors ------------------ */
    public QuickMemProbe()          { this(HandlerType.ORIGINAL); }         // default = original
    public QuickMemProbe(HandlerType handlerType) {
        player = MockFactory.newMockPlayer();
        MockFactory.addMockPlayer(player.user, player);
        MockFactory.set(player, "latencyUtils", switch (handlerType) {
                    case ORIGINAL -> new OriginalLatencyUtils(player);
                    case LOW_HANGING_FRUIT -> new LowHangingFruitLatencyUtils(player);
                    case MULTI_BLOCK_INT_ARRAY -> new LowHangingFruitLatencyUtils(player);
                }
        );

        blockChangeBenchmark = switch (handlerType) {
            case ORIGINAL -> new OriginalBlockChangeHandler();
            case LOW_HANGING_FRUIT -> new LowHangingFruitBlockChangeHandler();
            case MULTI_BLOCK_INT_ARRAY -> new MultiBlockChangeIntArrayBlockChangeHandler();
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

    private void addMultiBlockPayload() {
        WrapperPlayServerMultiBlockChange.EncodedBlock[] encodedBlocks = new WrapperPlayServerMultiBlockChange.EncodedBlock[100];
        for(int i=0;i<encodedBlocks.length;i++)
            encodedBlocks[i]=new WrapperPlayServerMultiBlockChange.EncodedBlock(0,0,0,0);
        WrapperPlayServerMultiBlockChange wrapperPlayServerMultiBlockChange = new WrapperPlayServerMultiBlockChange(new Vector3i(0,0,0),null, encodedBlocks);
        wrapperPlayServerMultiBlockChange.prepareForSend(player.user.getChannel(), false, false);
        PacketSendEvent packetSendEvent = EventCreationUtil.createSendEvent(player.user.getChannel(), player.user, null, wrapperPlayServerMultiBlockChange.buffer, false);
        blockChangeBenchmark.onPacketSend(packetSendEvent);
        ((ByteBuf) packetSendEvent.getByteBuf()).release();
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
//        System.out.println(GraphLayout.parseInstance(util).toFootprint());
        System.out.println(GraphLayout.parseInstance(player.latencyUtils.getTasksObject()).toFootprint());
    }

    /* ------------------ quick & dirty main --------------- */
    public static void main(String[] args) {
        if (!MockBukkit.isMocked()) MockBukkit.mock();
        Plugin p = MockBukkit.createMockPlugin("pe_global");
        PacketEvents.setAPI(TestPacketEventsBuilder.build(p));
        PacketType.prepare();

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
    }

    private void cleanup() {
        MockFactory.releaseMockPlayer(player);
    }
}