package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.manager.LastInstanceManager;
import ac.grim.grimac.utils.data.LastInstance;
import ac.grim.grimac.predictionengine.PointThreeEstimator;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import ac.grim.grimac.utils.latency.CompensatedEntities;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHook;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.math.TrigHandler;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.BlockProperties;
import ac.grim.grimac.utils.nmsutil.JumpPower;
import ac.grim.grimac.utils.nmsutil.MovementPhysics;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MovementPhysicsTest {
    private final List<MovementPhysics.Registration> registrations = new ArrayList<>();
    private MockedStatic<PacketEvents> packetEvents;

    @BeforeEach void runtime() {
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, withSettings().stubOnly());
        when(api.getSettings()).thenReturn(new PacketEventsSettings()
                .customResourceProvider(path -> PacketEvents.class.getClassLoader().getResourceAsStream(path)));
        ServerManager server = mock(ServerManager.class);
        when(server.getVersion()).thenReturn(ServerVersion.V_26_2);
        when(api.getServerManager()).thenReturn(server);
        when(api.getLogManager()).thenReturn(mock(LogManager.class));
        packetEvents = mockStatic(PacketEvents.class);
        packetEvents.when(PacketEvents::getAPI).thenReturn(api);
    }

    @AfterEach void cleanup() {
        registrations.forEach(MovementPhysics.Registration::close);
        packetEvents.close();
    }

    @Test void exactProtocolRegistrationClipsComponentsAndRetainsBoundaries() throws Exception {
        GrimPlayer player = player(ClientVersion.V_26_2);
        register(player, 0.005, false);
        assertVector(clipped(player, 0.0049, -0.0049, 0.6), 0, 0, 0.6);
        assertVector(clipped(player, 0.005, -0.005, 0.005), 0.005, -0.005, 0.005);
        assertVector(clipped(player, -0.005, 0.004, 0.004), -0.005, 0, 0);
    }

    @Test void unregisteredAndOtherProtocolsRetainNativeThresholds() throws Exception {
        GrimPlayer modern = player(ClientVersion.V_26_2);
        assertVector(clipped(modern, 0.0029, 0.0029, 0.004), 0.0029, 0, 0.004);
        register(modern, 0.005, false);
        assertVector(clipped(player(ClientVersion.V_26_2), 0.0029, 0.0029, 0.004), 0.0029, 0, 0.004);
        GrimPlayer other = player(ClientVersion.V_1_21_5);
        assertVector(clipped(other, 0.0029, 0.0029, 0.004), 0.0029, 0, 0.004);
        assertVector(clipped(player(ClientVersion.V_1_8), 0.0049, 0.0049, 0.006), 0, 0, 0.006);
    }

    @Test void vehiclesRetainTheirOwnPhysics() throws Exception {
        GrimPlayer player = player(ClientVersion.V_26_2);
        register(player, 0.005, false);
        when(player.inVehicle()).thenReturn(true);
        assertNull(MovementPhysics.profile(player));
        assertVector(clipped(player, 0.004, 0.004, 0.004), 0.004, 0.004, 0.004);
    }

    @Test void upwardJumpIsOverwrittenOnlyForTheRegisteredProfile() throws Exception {
        GrimPlayer player = player(ClientVersion.V_26_2);
        try (var blocks = mockStatic(BlockProperties.class)) {
            var nativeJump = new Vector3dm(0, 0.8, 0);
            JumpPower.jumpFromGround(player, nativeJump);
            assertEquals(0.8, nativeJump.getY());
            register(player, 0.005, false);
            var registeredJump = new Vector3dm(0.2, 0.8, -0.3);
            JumpPower.jumpFromGround(player, registeredJump);
            assertVector(registeredJump, 0.2, (double) 0.42F, -0.3);
            when(player.inVehicle()).thenReturn(true);
            var vehicle = new Vector3dm(0, 0.8, 0);
            JumpPower.jumpFromGround(player, vehicle);
            assertEquals(0.8, vehicle.getY());
        }
    }

    @Test void registeredSprintJumpUsesFloatProductBeforeWidening() throws Exception {
        GrimPlayer player = player(ClientVersion.V_26_2);
        player.isSprinting = true;
        player.yaw = 37.0F;
        var trig = mock(TrigHandler.class);
        var trigField = GrimPlayer.class.getDeclaredField("trigHandler");
        trigField.setAccessible(true);
        trigField.set(player, trig);
        float angle = GrimMath.radians(player.yaw);
        when(trig.sin(angle)).thenReturn(0.60181504F);
        when(trig.cos(angle)).thenReturn(0.79863554F);
        try (var blocks = mockStatic(BlockProperties.class)) {
            var nativeJump = new Vector3dm(0, 0, 0);
            JumpPower.jumpFromGround(player, nativeJump);
            assertEquals(-0.60181504F * 0.2, nativeJump.getX());
            assertEquals(0.79863554F * 0.2, nativeJump.getZ());
            registrations.add(MovementPhysics.register(player.uuid, player.getClientVersion().getProtocolVersion(), 0.005, false, true));
            var legacyJump = new Vector3dm(0, 0, 0);
            JumpPower.jumpFromGround(player, legacyJump);
            assertEquals((double) (-0.60181504F * 0.2F), legacyJump.getX());
            assertEquals((double) (0.79863554F * 0.2F), legacyJump.getZ());
            assertNotEquals(nativeJump.getX(), legacyJump.getX());
            double offset = 0.005 - (nativeJump.getX() + legacyJump.getX()) / 2;
            assertEquals(0, clipped(player, offset + legacyJump.getX(), 0, 0).getX());
            assertTrue(clipped(player, offset + nativeJump.getX(), 0, 0).getX() > 0);
            when(player.inVehicle()).thenReturn(true);
            var vehicleJump = new Vector3dm(0, 0, 0);
            JumpPower.jumpFromGround(player, vehicleJump);
            assertEquals(nativeJump.getX(), vehicleJump.getX());
            assertEquals(nativeJump.getZ(), vehicleJump.getZ());
        }
    }

    @Test void registeredFishingPullIncludesLiftAndCurrentVictimHeight() throws Exception {
        GrimPlayer player = player(ClientVersion.V_26_2);
        var instances = GrimPlayer.class.getDeclaredField("lastInstanceManager");
        instances.setAccessible(true);
        instances.set(player, mock(LastInstanceManager.class));
        var fireworkField = GrimPlayer.class.getDeclaredField("fireworks");
        fireworkField.setAccessible(true);
        fireworkField.set(player, mock(CompensatedFireworks.class));
        player.boundingBox = new SimpleCollisionBox(-0.3, 0, -0.3, 0.3, 1.5, 0.3);
        PacketEntity owner = mock(PacketEntity.class);
        when(owner.getAttributeValue(Attributes.SCALE)).thenReturn(1.0);
        when(owner.getPossibleCollisionBoxes()).thenAnswer(invocation ->
                new SimpleCollisionBox(3.7, 3, -0.3, 4.3, 4.8, 0.3));
        when(player.compensatedEntities.getEntity(80)).thenReturn(owner);
        try (var sizes = mockStatic(BoundingBoxSize.class)) {
            sizes.when(() -> BoundingBoxSize.getWidth(player, owner)).thenReturn(0.6F);
            sizes.when(() -> BoundingBoxSize.getHeight(player, owner)).thenReturn(1.8F);
            var handler = new UncertaintyHandler(player);
            handler.fishingRodPulls.add(80);
            handler.tickFireworksBox();
            assertEquals((3 - 0.8 * 1.8) * 0.1, handler.fishingRodPullBox.maxY, 1E-7);
            registrations.add(MovementPhysics.register(player.uuid, player.getClientVersion().getProtocolVersion(), 0.005, false, true, 0.08));
            handler.fishingRodPulls.add(80);
            handler.tickFireworksBox();
            double dy = 3 - 0.8 * 1.5;
            double expected = dy * 0.1 + (double) (float) Math.sqrt((double) (float) Math.sqrt(16 + dy * dy)) * 0.08;
            assertTrue(handler.fishingRodPullBox.maxY >= expected);
            assertTrue(handler.fishingRodPullBox.maxY - expected < 1E-7);
            handler.fishingRodPulls.add(80);
            handler.fishingRodPulls.add(80);
            handler.tickFireworksBox();
            assertTrue(handler.fishingRodPullBox.maxY >= expected * 2);
            when(owner.getPossibleCollisionBoxes()).thenAnswer(invocation ->
                    new SimpleCollisionBox(-2.3, -0.8, -2.3, 2.3, 5.0, 2.3));
            handler.fishingRodPulls.add(80);
            handler.tickFireworksBox();
            for (double x = -2; x <= 2; x += 0.25) for (double y = -2; y <= 2; y += 0.25) for (double z = -2; z <= 2; z += 0.25) {
                double vertical = y * 0.1 + (double) (float) Math.sqrt((double) (float) Math.sqrt(x * x + y * y + z * z)) * 0.08;
                assertTrue(vertical >= handler.fishingRodPullBox.minY && vertical <= handler.fishingRodPullBox.maxY);
            }
            assertTrue(handler.fishingRodPullBox.maxY < 0.35);
            when(owner.getPossibleCollisionBoxes()).thenAnswer(invocation ->
                    new SimpleCollisionBox(3.7, 3, -0.3, 4.3, 4.8, 0.3));
            when(player.inVehicle()).thenReturn(true);
            handler.fishingRodPulls.add(80);
            handler.tickFireworksBox();
            assertEquals((3 - 0.8 * 1.8) * 0.1, handler.fishingRodPullBox.maxY, 1E-7);
        }
    }

    @Test void immediateFishingReelUsesCompensatedHookPosition() throws Exception {
        verifyHookPull(false, new SimpleCollisionBox(0, 0.1, 0, 0, 0.1, 0), 0.1);
    }

    @Test void fishingPoseTransitionEnclosesTheNextAttachmentHeight() throws Exception {
        verifyHookPull(true, new SimpleCollisionBox(0, 1.44, 0, 0, 1.44, 0), 0.8 * 1.5);
    }

    private void verifyHookPull(boolean sneaking, SimpleCollisionBox hookPosition, double actualHookY) throws Exception {
        GrimPlayer player = player(ClientVersion.V_26_2);
        var instances = GrimPlayer.class.getDeclaredField("lastInstanceManager");
        instances.setAccessible(true);
        instances.set(player, mock(LastInstanceManager.class));
        var fireworkField = GrimPlayer.class.getDeclaredField("fireworks");
        fireworkField.setAccessible(true);
        fireworkField.set(player, mock(CompensatedFireworks.class));
        player.boundingBox = new SimpleCollisionBox(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
        player.isSneaking = sneaking;
        when(player.compensatedEntities.self.getAttributeValue(Attributes.SCALE)).thenReturn(1.0);
        PacketEntity owner = mock(PacketEntity.class);
        when(owner.getAttributeValue(Attributes.SCALE)).thenReturn(1.0);
        when(owner.getPossibleLocationBoxes()).thenAnswer(invocation -> new SimpleCollisionBox(4, 3, 0, 4, 3, 0));
        when(owner.getPossibleCollisionBoxes()).thenAnswer(invocation -> new SimpleCollisionBox(3.7, 3, -0.3, 4.3, 4.8, 0.3));
        when(player.compensatedEntities.getEntity(80)).thenReturn(owner);
        PacketEntityHook hook = mock(PacketEntityHook.class);
        when(hook.getPossibleLocationBoxes()).thenReturn(hookPosition);
        registrations.add(MovementPhysics.register(player.uuid, player.getClientVersion().getProtocolVersion(), 0.005, false, true, 0.08));
        try (var sizes = mockStatic(BoundingBoxSize.class)) {
            sizes.when(() -> BoundingBoxSize.getWidth(player, owner)).thenReturn(0.6F);
            sizes.when(() -> BoundingBoxSize.getHeight(player, owner)).thenReturn(1.8F);
            var handler = new UncertaintyHandler(player);
            handler.queueFishingRodPull(80, hook);
            handler.tickFireworksBox();
            double dy = 3 - actualHookY;
            double expected = dy * 0.1 + (double) (float) Math.sqrt((double) (float) Math.sqrt(16 + dy * dy)) * 0.08;
            assertTrue(handler.fishingRodPullBox.maxY >= expected);
            assertTrue(handler.fishingRodPullBox.maxY < 0.7);
            handler.queueFishingRodPull(80, hook);
            handler.queueFishingRodPull(80, hook);
            handler.tickFireworksBox();
            assertTrue(handler.fishingRodPullBox.maxY >= expected * 2);
            assertEquals(0.8, handler.fishingRodPullBox.maxX);
            handler.tickFireworksBox();
            assertNull(handler.fishingRodPullBox);
            registrations.get(registrations.size() - 1).close();
            handler.queueFishingRodPull(80, hook);
            handler.tickFireworksBox();
            assertEquals((3 - 0.8 * 1.8) * 0.1, handler.fishingRodPullBox.maxY, 1E-7);
        }
    }

    @Test void skippedTickEstimatorUsesTheRegisteredVerticalCutoff() throws Exception {
        GrimPlayer player = player(ClientVersion.V_26_2);
        when(player.getMovementThreshold()).thenReturn(0.03);
        var motion = new VectorData(new Vector3dm(0, 0.004, 0), VectorData.VectorType.ZeroPointZeroThree);
        try (var blocks = mockStatic(BlockProperties.class)) {
            blocks.when(() -> BlockProperties.getModifiedAirDrag(0.98F, player)).thenReturn(0.98F);
            assertTrue(new PointThreeEstimator(player).getAdditionalVerticalUncertainty(motion) > 0);
            register(player, 0.005, false);
            assertEquals(0.0, new PointThreeEstimator(player).getAdditionalVerticalUncertainty(motion));
        }
    }

    @Test void registrationIsValidatedExclusiveAndIdentitySafe() throws Exception {
        GrimPlayer player = player(ClientVersion.V_26_2);
        int protocol = player.getClientVersion().getProtocolVersion();
        for (double invalid : List.of(Double.NaN, Double.POSITIVE_INFINITY, -0.001)) {
            assertThrows(IllegalArgumentException.class, () -> MovementPhysics.register(player.uuid, protocol, invalid, false));
        }
        for (double invalid : List.of(Double.NaN, Double.POSITIVE_INFINITY, -0.08)) {
            assertThrows(IllegalArgumentException.class, () -> MovementPhysics.register(player.uuid, protocol, 0.005, false, true, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> MovementPhysics.register(player.uuid, -1, 0.005, false));
        assertThrows(NullPointerException.class, () -> MovementPhysics.register(null, protocol, 0.005, false));
        var first = register(player, 0.005, false);
        assertThrows(IllegalStateException.class, () -> MovementPhysics.register(player.uuid, protocol, 0.003, true));
        first.close();
        assertNull(MovementPhysics.profile(player));
        register(player, 0.006, true);
        first.close();
        assertEquals(new MovementPhysics.Profile(0.006, true), MovementPhysics.profile(player));
    }

    private MovementPhysics.Registration register(GrimPlayer player, double threshold, boolean preserve) {
        var registration = MovementPhysics.register(player.uuid, player.getClientVersion().getProtocolVersion(), threshold, preserve);
        registrations.add(registration);
        return registration;
    }

    private static Vector3dm clipped(GrimPlayer player, double x, double y, double z) {
        var data = new VectorData(new Vector3dm(x, y, z), VectorData.VectorType.Normal);
        new PredictionEngine().applyMovementThreshold(player, Set.of(data));
        return data.vector;
    }

    private static void assertVector(Vector3dm actual, double x, double y, double z) {
        assertEquals(x, actual.getX());
        assertEquals(y, actual.getY());
        assertEquals(z, actual.getZ());
    }

    private static GrimPlayer player(ClientVersion version) throws Exception {
        GrimPlayer player = mock(GrimPlayer.class, withSettings().stubOnly());
        when(player.getClientVersion()).thenReturn(version);
        var uuid = GrimPlayer.class.getDeclaredField("uuid");
        uuid.setAccessible(true);
        uuid.set(player, UUID.randomUUID());
        var handler = mock(UncertaintyHandler.class);
        var stuck = UncertaintyHandler.class.getDeclaredField("stuckOnEdge");
        stuck.setAccessible(true);
        stuck.set(handler, mock(LastInstance.class));
        var uncertainty = GrimPlayer.class.getDeclaredField("uncertaintyHandler");
        uncertainty.setAccessible(true);
        uncertainty.set(player, handler);
        var entities = mock(CompensatedEntities.class);
        entities.self = mock(PacketEntitySelf.class);
        when(entities.self.getAttributeValue(Attributes.JUMP_STRENGTH)).thenReturn((double) 0.42F);
        when(entities.getPotionLevelForPlayer(any())).thenReturn(OptionalInt.empty());
        var field = GrimPlayer.class.getDeclaredField("compensatedEntities");
        field.setAccessible(true);
        field.set(player, entities);
        return player;
    }
}
