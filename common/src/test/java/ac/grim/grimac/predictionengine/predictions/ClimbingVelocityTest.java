package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.PointThreeEstimator;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import ac.grim.grimac.utils.data.PacketStateData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.latency.CompensatedEntities;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.ClimbingVelocity;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ClimbingVelocityTest {
    private MockedStatic<PacketEvents> packetEvents;
    private final List<ClimbingVelocity.Registration> registrations = new ArrayList<>();

    @BeforeEach
    void providePacketEventsRuntime() {
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, withSettings().stubOnly());
        when(api.getSettings()).thenReturn(new PacketEventsSettings()
                .customResourceProvider(path -> PacketEvents.class.getClassLoader().getResourceAsStream(path)));
        ServerManager server = mock(ServerManager.class);
        when(server.getVersion()).thenReturn(ServerVersion.V_26_2);
        when(api.getServerManager()).thenReturn(server);
        when(api.getLogManager()).thenReturn(mock(LogManager.class));
        packetEvents = mockStatic(PacketEvents.class, withSettings().stubOnly());
        packetEvents.when(PacketEvents::getAPI).thenReturn(api);
    }

    @AfterEach
    void restorePacketEventsRuntime() {
        registrations.forEach(ClimbingVelocity.Registration::close);
        packetEvents.close();
    }

    @Test
    void registeredDescentChangesOnlyTheVerticalLimit() throws Exception {
        GrimPlayer player = playerOn("example:fast_ladder");
        Vector3dm result = new PredictionEngineNormal().handleOnClimbable(new Vector3dm(1, -1, -1), player);
        assertEquals((double) 0.15F, result.getX());
        assertEquals((double) -0.15F, result.getZ());
        assertEquals(-0.15F * 2.0, result.getY());
        assertEquals(0.0, player.fallDistance);
    }

    @Test
    void vanillaAndOtherNamespacesKeepVanillaDescent() throws Exception {
        for (String key : List.of("minecraft:ladder", "other:fast_ladder", "example:stone")) {
            GrimPlayer player = playerOn(key);
            assertEquals((double) -0.15F,
                    new PredictionEngineNormal().handleOnClimbable(new Vector3dm(0, -1, 0), player).getY(), key);
        }
    }

    @Test
    void sneakingStillStopsDescentAndNonClimbingStillUsesOriginalMotion() throws Exception {
        GrimPlayer player = playerOn("example:fast_ladder");
        player.isSneaking = true;
        assertEquals(0.0, new PredictionEngineNormal().handleOnClimbable(new Vector3dm(0, -1, 0), player).getY());
        player.isClimbing = false;
        Vector3dm motion = new Vector3dm(1, -1, -1);
        assertSame(motion, new PredictionEngineNormal().handleOnClimbable(motion, player));
        assertEquals(-1.0, motion.getY());
    }

    @Test
    void doubledDescentReachesTheActualFloorBeforeGroundValidation() throws Exception {
        SimpleCollisionBox body = new SimpleCollisionBox(0.2, 1.18888, 0.2, 0.8, 2.98888, 0.8);
        SimpleCollisionBox floor = new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        List<Collisions.Axis> order = List.of(Collisions.Axis.Y, Collisions.Axis.X, Collisions.Axis.Z);
        Vector3dm registeredMotion = new PredictionEngineNormal().handleOnClimbable(
                new Vector3dm(0, -1, 0), playerOn("example:fast_ladder"));
        Vector3dm clipped = Collisions.collideBoundingBoxLegacy(registeredMotion, body, List.of(floor), order);
        assertNotEquals(registeredMotion.getY(), clipped.getY(), "Downward collision must establish physical grounding");
        assertEquals(1.0, body.minY + clipped.getY(), 1.0E-12);
        Vector3dm vanillaMotion = new PredictionEngineNormal().handleOnClimbable(
                new Vector3dm(0, -1, 0), playerOn("minecraft:ladder"));
        assertEquals(vanillaMotion.getY(),
                Collisions.collideBoundingBoxLegacy(vanillaMotion, body, List.of(floor), order).getY());
    }

    @Test
    void dryAscentSeedsRealGravityAndDragWithTheRegisteredSpeed() throws Exception {
        for (String key : List.of("example:fast_ladder", "minecraft:ladder")) {
            GrimPlayer player = playerOn(key);
            PredictionEngineNormal engine = spy(new PredictionEngineNormal());
            doReturn(false).when(engine).canSwimHop(player);
            try (MockedStatic<Collisions> collisions = mockStatic(Collisions.class)) {
                collisions.when(() -> Collisions.onClimbable(player, player.x, player.y, player.z)).thenReturn(true);
                engine.endOfTick(player, 0);
            }
            double ascent = key.startsWith("example:") ? 0.4 : 0.2;
            assertEquals((ascent - 0.08) * 0.98F, player.lastWasClimbing, 1.0E-12, key);
        }
    }

    @Test
    void waterPredictionRetainsItsOriginalMotion() throws Exception {
        GrimPlayer player = playerOn("example:fast_ladder");
        Vector3dm motion = new Vector3dm(0.3, -0.4, 0.3);
        assertSame(motion, new PredictionEngineWater().handleOnClimbable(motion, player));
        assertEquals(-0.4, motion.getY());
    }

    @Test
    void reducedMovementCandidatesDoubleOnlyDryRegisteredAscent() throws Exception {
        for (PredictionEngine original : List.of(new PredictionEngineNormal(), new PredictionEngineWater())) {
            GrimPlayer player = playerOn("example:fast_ladder");
            when(player.pointThreeEstimator.isNearClimbable()).thenReturn(true);
            PredictionEngine engine = spy(original);
            doNothing().when(engine).addJumpsToPossibilities(any(), anySet());
            doNothing().when(engine).addExplosionToPossibilities(any(), anySet());
            doAnswer(call -> new ArrayList<>(call.<Set<VectorData>>getArgument(1)))
                    .when(engine).applyInputsToVelocityPossibilities(any(), anySet(), anyFloat());
            List<VectorData> candidates = new ArrayList<>();
            var method = PredictionEngine.class.getDeclaredMethod("addZeroPointThreeToPossibilities",
                    float.class, GrimPlayer.class, List.class);
            method.setAccessible(true);
            method.invoke(engine, 0.1F, player, candidates);
            double ascent = original instanceof PredictionEngineNormal ? 0.4 : 0.2;
            assertEquals((ascent - 0.08) * 0.98F,
                    candidates.stream().mapToDouble(vector -> vector.vector.getY()).max().orElseThrow(), 1.0E-12);
        }
    }

    @Test
    void unregisteredBlocksNeverAcquireCustomPhysicsFromTheirName() throws Exception {
        GrimPlayer player = playerOn("example:fast_ladder", false);
        assertEquals(0.2, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
        assertEquals((double) -0.15F, ClimbingVelocity.descentMinimum(player, 0, 0, 0));
    }

    @Test
    void submergedLifecycleAscentStaysVanillaExceptDuringDryFlight() throws Exception {
        GrimPlayer player = playerOn("example:fast_ladder");
        for (boolean water : List.of(true, false)) {
            player.wasTouchingWater = water;
            player.wasTouchingLava = !water;
            player.isFlying = false;
            assertEquals(0.2, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
            player.isFlying = true;
            assertEquals(0.4, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
        }
        player.wasTouchingWater = false;
        player.wasTouchingLava = false;
        player.isFlying = false;
        assertEquals(0.4, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
    }

    @Test
    void closingRestoresVanillaAndCannotRemoveALaterRegistration() throws Exception {
        GrimPlayer player = playerOn("example:configurable_ladder", false);
        StateType block = player.compensatedWorld.getBlockType(0, 0, 0);
        ClimbingVelocity.Registration first = ClimbingVelocity.register(block, 0.6, -0.25);
        registrations.add(first);
        assertEquals(0.6, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
        assertEquals(-0.25, ClimbingVelocity.descentMinimum(player, 0, 0, 0));
        first.close();
        assertEquals(0.2, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
        assertEquals((double) -0.15F, ClimbingVelocity.descentMinimum(player, 0, 0, 0));
        registrations.add(ClimbingVelocity.register(block, 0.6, -0.25));
        first.close();
        assertEquals(0.6, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
    }

    @Test
    void duplicateProfilesFailWithoutReplacingTheExistingPhysics() throws Exception {
        GrimPlayer player = playerOn("example:fast_ladder");
        StateType block = player.compensatedWorld.getBlockType(0, 0, 0);
        assertThrows(IllegalStateException.class, () -> ClimbingVelocity.register(block, 1.0, -1.0));
        assertEquals(0.4, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
        assertEquals(-0.15F * 2.0, ClimbingVelocity.descentMinimum(player, 0, 0, 0));
    }

    @Test
    void invalidProfilesCannotAlterDefaultPhysics() throws Exception {
        GrimPlayer player = playerOn("example:configurable_ladder", false);
        StateType block = player.compensatedWorld.getBlockType(0, 0, 0);
        for (double[] invalid : List.of(new double[]{Double.NaN, -0.15}, new double[]{Double.POSITIVE_INFINITY, -0.15},
                new double[]{-0.2, -0.15}, new double[]{0.2, Double.NaN},
                new double[]{0.2, Double.NEGATIVE_INFINITY}, new double[]{0.2, 0.15})) {
            assertThrows(IllegalArgumentException.class, () -> ClimbingVelocity.register(block, invalid[0], invalid[1]));
        }
        assertThrows(NullPointerException.class, () -> ClimbingVelocity.register(null, 0.2, -0.15F));
        assertEquals(0.2, ClimbingVelocity.ascentSpeed(player, 0, 0, 0));
    }

    private GrimPlayer playerOn(String key) throws Exception {
        return playerOn(key, key.equals("example:fast_ladder"));
    }

    private GrimPlayer playerOn(String key, boolean registered) throws Exception {
        GrimPlayer player = mock(GrimPlayer.class, withSettings().stubOnly());
        when(player.getClientVersion()).thenReturn(ClientVersion.V_26_2);
        when(player.getPossibleVelocitiesMinusKnockback()).thenReturn(Set.of());
        player.isClimbing = true;
        player.hasGravity = true;
        player.fallDistance = 10;
        player.clientVelocity = new Vector3dm();
        StateType.Mapped mapped = mock(StateType.Mapped.class);
        when(mapped.getName()).thenReturn(new ResourceLocation(key));
        StateType block = mock(StateType.class);
        when(block.getMapped()).thenReturn(mapped);
        if (registered) {
            registrations.add(ClimbingVelocity.register(block, 0.4, -0.15F * 2.0));
        }
        CompensatedWorld world = mock(CompensatedWorld.class);
        when(world.getBlockType(anyDouble(), anyDouble(), anyDouble())).thenReturn(block);
        setField(player, "compensatedWorld", world);
        setField(player, "pointThreeEstimator", mock(PointThreeEstimator.class));
        setField(player, "packetStateData", new PacketStateData());
        CompensatedEntities entities = mock(CompensatedEntities.class);
        when(entities.getPotionLevelForPlayer(any())).thenReturn(OptionalInt.empty());
        when(entities.getSlowFallingAmplifier()).thenReturn(OptionalInt.empty());
        PacketEntitySelf self = mock(PacketEntitySelf.class);
        when(self.getAttributeValue(Attributes.GRAVITY)).thenReturn(0.08);
        when(self.getAttributeValue(Attributes.AIR_DRAG_MODIFIER)).thenReturn(1.0);
        entities.self = self;
        setField(player, "compensatedEntities", entities);
        return player;
    }

    private static void setField(GrimPlayer player, String name, Object value) throws Exception {
        var field = GrimPlayer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(player, value);
    }
}
