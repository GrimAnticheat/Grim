package ac.grim.grimac.events.packets;

import ac.grim.grimac.manager.CheckManager;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.data.BlockPlaceSnapshot;
import ac.grim.grimac.utils.data.PacketStateData;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.latency.CompensatedDashableEntities;
import ac.grim.grimac.utils.latency.CompensatedEntities;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.LogManager;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies queued block place and break rotation resolution and deterministic
 * {@code CLIENT_TICK_END} flushing across protocol versions and ViaVersion/ViaBackwards setups.
 */
class QueuedBlockInteractionTickEndTest {

    private static PacketEventsAPI<?> previousApi;
    private static ServerManager serverManager;
    private static volatile ServerVersion currentServerVersion = ServerVersion.V_1_21_11;

    @BeforeAll
    static void setUpPacketEvents() {
        previousApi = PacketEvents.getAPI();
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class);
        serverManager = mock(ServerManager.class);
        when(api.getServerManager()).thenReturn(serverManager);
        when(api.getSettings()).thenReturn(new PacketEventsSettings());
        when(api.getLogManager()).thenReturn(mock(LogManager.class));
        when(serverManager.getVersion()).thenAnswer(inv -> currentServerVersion);
        PacketEvents.setAPI(api);
    }

    @AfterAll
    static void tearDownPacketEvents() {
        PacketEvents.setAPI(previousApi);
    }

    @Test
    void tickEndClientAppliesQueuedLookEvenWithHighNetworkJitter() {
        // Issue #2920: on 1.21.2+ client + 1.21.2+ server, >15ms jitter between place/break
        // and the same tick's flying rotation must still apply the tick's rotation.
        currentServerVersion = ServerVersion.V_1_21_11;
        GrimPlayer player = createPlayer(ClientVersion.V_1_21_11);
        player.yaw = 10f;
        player.pitch = 5f;
        player.lastBlockPlaceUseItem = 1_000L;
        player.lastBlockBreak = 1_000L;

        assertTrue(player.supportsEndTick());
        assertFalse(player.canSkipTicks());
        assertTrue(CheckManagerListener.shouldApplyQueuedLook(player, true, 1_000L, 1_500L));

        float[] observedPlaceLook = new float[2];
        doAnswer(inv -> {
            observedPlaceLook[0] = player.yaw;
            observedPlaceLook[1] = player.pitch;
            return null;
        }).when(player.compensatedWorld).startPredicting();

        float[] observedBreakLook = new float[2];
        doAnswer(inv -> {
            observedBreakLook[0] = player.yaw;
            observedBreakLook[1] = player.pitch;
            return null;
        }).when(player.checkManager).onPostFlyingBlockBreak(any());

        player.placeUseItemPackets.add(new BlockPlaceSnapshot(mockUseItem(Float.NaN, Float.NaN), false));
        player.queuedBreaks.add(createBlockBreak(player));

        CheckManagerListener.handleQueuedPlaces(player, true, 25f, 135f, 1_500L);
        CheckManagerListener.handleQueuedBreaks(player, true, 25f, 135f, 1_500L);

        assertEquals(135f, observedPlaceLook[0]);
        assertEquals(25f, observedPlaceLook[1]);
        assertEquals(135f, observedBreakLook[0]);
        assertEquals(25f, observedBreakLook[1]);
        // Player rotation must be restored after draining queues (handleFlying assigns final look afterwards)
        assertEquals(10f, player.yaw);
        assertEquals(5f, player.pitch);
        assertTrue(player.placeUseItemPackets.isEmpty());
        assertTrue(player.queuedBreaks.isEmpty());
    }

    @Test
    void clientTickEndFlushesIdleTickInteractionsBeforePoseUpdateAndNextTickRotation() {
        currentServerVersion = ServerVersion.V_1_21_11;
        GrimPlayer player = createPlayer(ClientVersion.V_1_21_11);
        player.yaw = 45f;
        player.pitch = 15f;
        player.pose = Pose.CROUCHING;
        player.wasSneaking = true;
        player.isSneaking = false;
        player.serverOpenedInventoryThisTick = true;
        player.packetStateData.didLastMovementIncludePosition = true;
        player.packetStateData.didSendMovementBeforeTickEnd = false;
        player.lastBlockPlaceUseItem = 2_000L;
        player.lastBlockBreak = 2_000L;

        List<String> events = new ArrayList<>();
        float[] placeLook = new float[2];
        AtomicReference<Pose> poseDuringPlace = new AtomicReference<>();
        boolean[] wasSneakingDuringPlace = new boolean[1];

        doAnswer(inv -> {
            events.add("place");
            placeLook[0] = player.yaw;
            placeLook[1] = player.pitch;
            poseDuringPlace.set(player.pose);
            wasSneakingDuringPlace[0] = player.wasSneaking;
            return null;
        }).when(player.compensatedWorld).startPredicting();

        float[] breakLook = new float[2];
        AtomicReference<Pose> poseDuringBreak = new AtomicReference<>();
        doAnswer(inv -> {
            events.add("break");
            breakLook[0] = player.yaw;
            breakLook[1] = player.pitch;
            poseDuringBreak.set(player.pose);
            return null;
        }).when(player.checkManager).onPostFlyingBlockBreak(any());

        doAnswer(inv -> {
            events.add("dashTick");
            return null;
        }).when(player.dashableEntities).tick();

        player.placeUseItemPackets.add(new BlockPlaceSnapshot(mockUseItem(Float.NaN, Float.NaN), true));
        player.queuedBreaks.add(createBlockBreak(player));

        // End of idle tick T
        CheckManagerListener.handleClientTickEnd(player, 2_001L);

        assertEquals(List.of("place", "break", "dashTick"), events);
        assertEquals(45f, placeLook[0]);
        assertEquals(15f, placeLook[1]);
        assertEquals(Pose.CROUCHING, poseDuringPlace.get());
        assertTrue(wasSneakingDuringPlace[0]);
        assertEquals(45f, breakLook[0]);
        assertEquals(15f, breakLook[1]);
        assertEquals(Pose.CROUCHING, poseDuringBreak.get());

        // Idle tick post-state updated after flushing queued interactions
        assertEquals(Pose.STANDING, player.pose);
        assertFalse(player.wasSneaking);
        assertFalse(player.serverOpenedInventoryThisTick);
        assertTrue(player.packetStateData.didLastLastMovementIncludePosition);
        assertFalse(player.packetStateData.didLastMovementIncludePosition);
        assertFalse(player.packetStateData.didSendMovementBeforeTickEnd);
        assertTrue(player.placeUseItemPackets.isEmpty());
        assertTrue(player.queuedBreaks.isEmpty());

        // Tick T+1 rotation arriving 1ms later cannot retroactively re-evaluate tick T interactions
        CheckManagerListener.handleQueuedPlaces(player, true, -60f, 180f, 2_002L);
        CheckManagerListener.handleQueuedBreaks(player, true, -60f, 180f, 2_002L);
        assertEquals(List.of("place", "break", "dashTick"), events);
    }

    @Test
    void clientTickEndStillFlushesOutOfOrderQueuedInteractionsAfterMovement() {
        // Even if a non-vanilla client sends a place/break after flying in the same tick,
        // CLIENT_TICK_END drains the queues so they never leak into tick T+1's rotation.
        currentServerVersion = ServerVersion.V_1_21_11;
        GrimPlayer player = createPlayer(ClientVersion.V_1_21_11);
        player.packetStateData.didSendMovementBeforeTickEnd = true;
        player.placeUseItemPackets.add(new BlockPlaceSnapshot(mockUseItem(Float.NaN, Float.NaN), false));
        player.queuedBreaks.add(createBlockBreak(player));

        CheckManagerListener.handleClientTickEnd(player, 3_000L);

        assertTrue(player.placeUseItemPackets.isEmpty());
        assertTrue(player.queuedBreaks.isEmpty());
        assertFalse(player.packetStateData.didSendMovementBeforeTickEnd);
        verify(player.compensatedWorld).startPredicting();
        verify(player.checkManager).onPostFlyingBlockBreak(any());
    }

    @Test
    void viaVersionPre1212ServerFallsBackTo15msWindowFor1212Client() {
        // When a 1.21.2+ client connects to a <1.21.2 server, ViaVersion strips CLIENT_TICK_END,
        // so supportsEndTick() is false and canSkipTicks() is true.
        currentServerVersion = ServerVersion.V_1_20_6;
        GrimPlayer player = createPlayer(ClientVersion.V_1_21_11);

        assertFalse(player.supportsEndTick());
        assertTrue(player.canSkipTicks());
        assertTrue(CheckManagerListener.shouldApplyQueuedLook(player, true, 1_000L, 1_014L));
        assertFalse(CheckManagerListener.shouldApplyQueuedLook(player, true, 1_000L, 1_015L));
        assertFalse(CheckManagerListener.shouldApplyQueuedLook(player, true, 1_000L, 1_200L));
    }

    @Test
    void viaBackwardsPre1212ClientOn1212ServerKeeps15msWindow() {
        // ViaBackwards synthetic CLIENT_TICK_END is not trusted for <1.21.2 clients.
        currentServerVersion = ServerVersion.V_1_21_11;
        GrimPlayer player = createPlayer(ClientVersion.V_1_21);

        assertFalse(player.supportsEndTick());
        assertTrue(player.canSkipTicks());
        assertTrue(CheckManagerListener.shouldApplyQueuedLook(player, true, 1_000L, 1_010L));
        assertFalse(CheckManagerListener.shouldApplyQueuedLook(player, true, 1_000L, 1_015L));
    }

    @Test
    void legacy18ClientAlwaysAppliesQueuedLookRegardlessOfDelay() {
        currentServerVersion = ServerVersion.V_1_21_11;
        GrimPlayer player = createPlayer(ClientVersion.V_1_8);

        assertFalse(player.canSkipTicks());
        assertTrue(CheckManagerListener.shouldApplyQueuedLook(player, true, 1_000L, 5_000L));
        assertFalse(CheckManagerListener.shouldApplyQueuedLook(player, false, 1_000L, 1_001L));
    }

    @Test
    void teleportRotationIsNeverAppliedToQueuedInteractions() {
        currentServerVersion = ServerVersion.V_1_21_11;
        for (ClientVersion version : new ClientVersion[]{ClientVersion.V_1_8, ClientVersion.V_1_21, ClientVersion.V_1_21_11}) {
            GrimPlayer player = createPlayer(version);
            player.yaw = 20f;
            player.pitch = 10f;
            player.lastBlockPlaceUseItem = 1_000L;
            player.lastBlockBreak = 1_000L;
            player.packetStateData.lastPacketWasTeleport = true;

            assertFalse(CheckManagerListener.shouldApplyQueuedLook(player, true, 1_000L, 1_001L),
                    "Teleport rotation must never apply for " + version);

            float[] observedBreakLook = new float[2];
            doAnswer(inv -> {
                observedBreakLook[0] = player.yaw;
                observedBreakLook[1] = player.pitch;
                return null;
            }).when(player.checkManager).onPostFlyingBlockBreak(any());

            player.queuedBreaks.add(createBlockBreak(player));
            CheckManagerListener.handleQueuedBreaks(player, true, -80f, 270f, 1_001L);

            assertEquals(20f, observedBreakLook[0]);
            assertEquals(10f, observedBreakLook[1]);
        }
    }

    @Test
    void queuedPlaceRotationDoesNotLeakIntoStaleQueuedBreakOnTickSkippingClient() {
        // On 1.9-1.21.1, a recent place (<15ms) and a stale break (>=15ms) handled in the same
        // handleFlying call must not leak the flying rotation from handleQueuedPlaces into handleQueuedBreaks.
        currentServerVersion = ServerVersion.V_1_20_6;
        GrimPlayer player = createPlayer(ClientVersion.V_1_21);
        player.yaw = 30f;
        player.pitch = 12f;
        player.lastBlockPlaceUseItem = 1_995L; // 5ms ago -> apply look
        player.lastBlockBreak = 1_900L;        // 100ms ago -> keep previous look

        float[] observedPlaceLook = new float[2];
        doAnswer(inv -> {
            observedPlaceLook[0] = player.yaw;
            observedPlaceLook[1] = player.pitch;
            return null;
        }).when(player.compensatedWorld).startPredicting();

        float[] observedBreakLook = new float[2];
        doAnswer(inv -> {
            observedBreakLook[0] = player.yaw;
            observedBreakLook[1] = player.pitch;
            return null;
        }).when(player.checkManager).onPostFlyingBlockBreak(any());

        player.placeUseItemPackets.add(new BlockPlaceSnapshot(mockUseItem(Float.NaN, Float.NaN), false));
        player.queuedBreaks.add(createBlockBreak(player));

        CheckManagerListener.handleQueuedPlaces(player, true, -40f, 160f, 2_000L);
        CheckManagerListener.handleQueuedBreaks(player, true, -40f, 160f, 2_000L);

        assertEquals(160f, observedPlaceLook[0]);
        assertEquals(-40f, observedPlaceLook[1]);
        assertEquals(30f, observedBreakLook[0]);
        assertEquals(12f, observedBreakLook[1]);
        assertEquals(30f, player.yaw);
        assertEquals(12f, player.pitch);
    }

    @Test
    void useItemPacketRotationTakesPrecedenceAndRestoresPlayerLook() {
        // Regression guard for PR #3018: on 1.21+, USE_ITEM's embedded rotation overrides
        // the flying rotation, clamps pitch to [-90, 90], and restores player look afterwards.
        currentServerVersion = ServerVersion.V_1_21_11;
        GrimPlayer player = createPlayer(ClientVersion.V_1_21_11);
        player.yaw = 15f;
        player.pitch = 5f;
        player.lastBlockPlaceUseItem = 1_000L;

        float[] observedUseLook = new float[2];
        doAnswer(inv -> {
            observedUseLook[0] = player.yaw;
            observedUseLook[1] = player.pitch;
            return null;
        }).when(player.compensatedWorld).startPredicting();

        player.placeUseItemPackets.add(new BlockPlaceSnapshot(mockUseItem(222f, 120f), false));

        CheckManagerListener.handleQueuedPlaces(player, true, 30f, 90f, 1_500L);

        assertEquals(222f, observedUseLook[0]);
        assertEquals(90f, observedUseLook[1]);
        assertEquals(15f, player.yaw);
        assertEquals(5f, player.pitch);
    }

    @Test
    void multipleQueuedInteractionsInSameTickEachReceiveTickRotationAndRestoreLook() {
        currentServerVersion = ServerVersion.V_1_21_11;
        GrimPlayer player = createPlayer(ClientVersion.V_1_21_11);
        player.yaw = -45f;
        player.pitch = 10f;
        player.lastBlockPlaceUseItem = 4_000L;
        player.lastBlockBreak = 4_000L;

        List<float[]> observedPlaceLooks = new ArrayList<>();
        doAnswer(inv -> {
            observedPlaceLooks.add(new float[]{player.yaw, player.pitch});
            return null;
        }).when(player.compensatedWorld).startPredicting();

        List<float[]> observedBreakLooks = new ArrayList<>();
        doAnswer(inv -> {
            observedBreakLooks.add(new float[]{player.yaw, player.pitch});
            return null;
        }).when(player.checkManager).onPostFlyingBlockBreak(any());

        player.placeUseItemPackets.add(new BlockPlaceSnapshot(mockUseItem(Float.NaN, Float.NaN), false));
        player.placeUseItemPackets.add(new BlockPlaceSnapshot(mockUseItem(Float.NaN, Float.NaN), true));
        player.queuedBreaks.add(createBlockBreak(player));
        player.queuedBreaks.add(createBlockBreak(player));

        CheckManagerListener.handleQueuedPlaces(player, true, 32.5f, 180f, 4_450L);
        CheckManagerListener.handleQueuedBreaks(player, true, 32.5f, 180f, 4_450L);

        assertEquals(2, observedPlaceLooks.size());
        assertEquals(2, observedBreakLooks.size());
        for (float[] look : observedPlaceLooks) {
            assertEquals(180f, look[0]);
            assertEquals(32.5f, look[1]);
        }
        for (float[] look : observedBreakLooks) {
            assertEquals(180f, look[0]);
            assertEquals(32.5f, look[1]);
        }
        assertEquals(-45f, player.yaw);
        assertEquals(10f, player.pitch);
    }

    private static GrimPlayer createPlayer(ClientVersion clientVersion) {
        GrimPlayer player = mock(GrimPlayer.class);
        when(player.getClientVersion()).thenReturn(clientVersion);
        when(player.supportsEndTickPreVia()).thenCallRealMethod();
        when(player.supportsEndTick()).thenCallRealMethod();
        when(player.canSkipTicksPreVia()).thenCallRealMethod();
        when(player.canSkipTicks()).thenCallRealMethod();
        when(player.inVehicle()).thenReturn(false);

        PacketStateData stateData = new PacketStateData();
        stateData.lastClaimedPosition = new Vector3d(10.0, 64.0, 20.0);

        CompensatedWorld compensatedWorld = mock(CompensatedWorld.class);
        when(compensatedWorld.getMinHeight()).thenReturn(-64);
        when(compensatedWorld.getMaxHeight()).thenReturn(320);

        CheckManager checkManager = mock(CheckManager.class);
        PacketWorldBorder worldBorder = mock(PacketWorldBorder.class);
        when(worldBorder.getMinX()).thenReturn(-1_000.0);
        when(worldBorder.getMaxX()).thenReturn(1_000.0);
        when(worldBorder.getMinZ()).thenReturn(-1_000.0);
        when(worldBorder.getMaxZ()).thenReturn(1_000.0);
        when(checkManager.get(PacketWorldBorder.class)).thenReturn(worldBorder);

        CompensatedEntities compensatedEntities = mock(CompensatedEntities.class);
        PacketEntitySelf self = mock(PacketEntitySelf.class);
        when(self.getAttributeValue(Attributes.SCALE)).thenReturn(1.0);
        compensatedEntities.self = self;

        CompensatedDashableEntities dashableEntities = mock(CompensatedDashableEntities.class);

        player.x = 10.0;
        player.y = 64.0;
        player.z = 20.0;
        player.lastX = 10.0;
        player.lastY = 64.0;
        player.lastZ = 20.0;
        player.gamemode = GameMode.SPECTATOR;
        player.pose = Pose.STANDING;

        setField(player, "packetStateData", stateData);
        setField(player, "placeUseItemPackets", new LinkedBlockingQueue<BlockPlaceSnapshot>());
        setField(player, "queuedBreaks", new LinkedBlockingQueue<BlockBreak>());
        setField(player, "compensatedWorld", compensatedWorld);
        setField(player, "checkManager", checkManager);
        setField(player, "compensatedEntities", compensatedEntities);
        setField(player, "dashableEntities", dashableEntities);
        return player;
    }

    private static WrapperPlayClientUseItem mockUseItem(float yaw, float pitch) {
        WrapperPlayClientUseItem useItem = mock(WrapperPlayClientUseItem.class);
        when(useItem.getHand()).thenReturn(InteractionHand.MAIN_HAND);
        when(useItem.getYaw()).thenReturn(yaw);
        when(useItem.getPitch()).thenReturn(pitch);
        return useItem;
    }

    private static BlockBreak createBlockBreak(GrimPlayer player) {
        WrappedBlockState block = mock(WrappedBlockState.class);
        return new BlockBreak(player, new Vector3i(10, 64, 21), BlockFace.NORTH, 2, DiggingAction.START_DIGGING, 1, block);
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = GrimPlayer.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to initialize GrimPlayer." + name, e);
        }
    }
}
