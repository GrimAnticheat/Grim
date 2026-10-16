package ac.grim.grimac.utils.inventory;

import ac.grim.grimac.checks.GrimProcessor;
import ac.grim.grimac.events.packets.PacketPlayerDigging;
import ac.grim.grimac.manager.CheckManager;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PacketStateData;
import ac.grim.grimac.utils.latency.CompensatedCooldown;
import ac.grim.grimac.utils.latency.CompensatedInventory;
import ac.grim.grimac.utils.lists.CorrectingPlayerInventoryStorage;
import ac.grim.grimac.utils.nmsutil.MovementPhysics;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.LogManager;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import io.github.retrooper.packetevents.impl.netty.NettyManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OffHandPredictionTest {
    private final List<MovementPhysics.Registration> registrations = new ArrayList<>();
    private MockedStatic<PacketEvents> packets;
    private record Fixture(GrimPlayer player, Inventory inventory, InventoryStorage storage, CompensatedInventory compensated) {}

    @BeforeEach void runtime() {
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, withSettings().stubOnly());
        when(api.getSettings()).thenReturn(new PacketEventsSettings()
            .customResourceProvider(path -> PacketEvents.class.getClassLoader().getResourceAsStream(path)));
        ServerManager server = mock(ServerManager.class);
        when(server.getVersion()).thenReturn(ServerVersion.V_26_2);
        when(api.getServerManager()).thenReturn(server);
        when(api.getLogManager()).thenReturn(mock(LogManager.class));
        packets = mockStatic(PacketEvents.class);
        packets.when(PacketEvents::getAPI).thenReturn(api);
        when(api.getNettyManager()).thenReturn(new NettyManagerImpl());
    }

    @AfterEach void cleanup() {
        registrations.forEach(MovementPhysics.Registration::close);
        packets.close();
    }

    private static void field(Object object, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private Fixture fixture(ClientVersion version) throws Exception {
        GrimPlayer player = mock(GrimPlayer.class, withSettings().stubOnly());
        when(player.getClientVersion()).thenReturn(version);
        field(player, GrimPlayer.class, "uuid", UUID.randomUUID());
        field(player, GrimPlayer.class, "packetStateData", new PacketStateData());
        field(player, GrimPlayer.class, "lastTransactionReceived", new AtomicInteger());
        player.gamemode = GameMode.SURVIVAL;
        InventoryStorage backing = new InventoryStorage(46);
        CorrectingPlayerInventoryStorage correcting = mock(CorrectingPlayerInventoryStorage.class);
        when(correcting.getItem(anyInt())).thenAnswer(call -> backing.getItem(call.getArgument(0)));
        when(correcting.getSize()).thenReturn(46);
        when(correcting.getMaxStackSize()).thenReturn(64);
        doAnswer(call -> { backing.setItem(call.getArgument(0), call.getArgument(1)); return null; })
            .when(correcting).setItem(anyInt(), any());
        when(correcting.removeItem(anyInt(), anyInt())).thenAnswer(call -> backing.removeItem(call.getArgument(0), call.getArgument(1)));
        Inventory inventory = new Inventory(player, correcting);
        CompensatedInventory compensated = mock(CompensatedInventory.class);
        field(compensated, GrimProcessor.class, "player", player);
        field(compensated, CompensatedInventory.class, "inventory", inventory);
        when(compensated.getHeldItem()).thenCallRealMethod();
        when(compensated.getOffHand()).thenCallRealMethod();
        when(compensated.getItemInHand(any())).thenCallRealMethod();
        field(player, GrimPlayer.class, "inventory", compensated);
        CheckManager checks = mock(CheckManager.class);
        when(checks.getCompensatedCooldown()).thenReturn(mock(CompensatedCooldown.class));
        field(player, GrimPlayer.class, "checkManager", checks);
        return new Fixture(player, inventory, backing, compensated);
    }

    private MovementPhysics.Registration closeOffHand(Fixture f) {
        MovementPhysics.Registration registration = MovementPhysics.register(f.player.uuid, 776, 0.005, false, true, 0.08, false);
        registrations.add(registration);
        return registration;
    }

    private ItemStack shield() { return ItemStack.builder().type(ItemTypes.SHIELD).amount(1).build(); }
    private ItemStack stone(int count) { return ItemStack.builder().type(ItemTypes.STONE).amount(count).build(); }

    @Test void activatedShiftClickImmediatelyExposesMainShieldAndNativeUseSlowdown() throws Exception {
        Fixture f = fixture(ClientVersion.V_26_2);
        closeOffHand(f);
        f.storage.setItem(9, shield());
        f.inventory.doClick(0, 9, WindowClickType.QUICK_MOVE);
        PacketPlayerDigging.handleUseItem(f.player, InteractionHand.MAIN_HAND);
        assertAll(
            () -> assertTrue(f.storage.getItem(9).isEmpty()),
            () -> assertTrue(f.storage.getItem(45).isEmpty()),
            () -> assertEquals(ItemTypes.SHIELD, f.inventory.getHeldItem().getType()),
            () -> assertEquals(ItemTypes.SHIELD, f.compensated.getItemInHand(InteractionHand.MAIN_HAND).getType()),
            () -> assertTrue(f.player.packetStateData.isSlowedByUsingItem()));
        f.inventory.doClick(0, 36, WindowClickType.QUICK_MOVE);
        assertTrue(f.storage.getItem(36).isEmpty());
        assertEquals(ItemTypes.SHIELD, f.storage.getItem(9).getType());
        assertTrue(f.storage.getItem(45).isEmpty());
    }

    @Test void preActivationUnregisteredOtherProtocolAndOldOverloadKeepNativeRouting() throws Exception {
        Fixture nativeClient = fixture(ClientVersion.V_26_2);
        assertNativeShieldRouting(nativeClient);
        Fixture older = fixture(ClientVersion.V_1_21_5);
        closeOffHand(older); // Exact UUID but mismatched protocol must not select the new profile.
        assertNativeShieldRouting(older);
        Fixture oldApi = fixture(ClientVersion.V_26_2);
        registrations.add(MovementPhysics.register(oldApi.player.uuid, 776, 0.005, false, true, 0.08));
        assertNativeShieldRouting(oldApi);
        Fixture unrelated = fixture(ClientVersion.V_26_2);
        assertNativeShieldRouting(unrelated);
    }

    private void assertNativeShieldRouting(Fixture f) {
        f.storage.setItem(9, shield());
        f.inventory.doClick(0, 9, WindowClickType.QUICK_MOVE);
        assertEquals(ItemTypes.SHIELD, f.storage.getItem(45).getType());
        assertTrue(f.inventory.getHeldItem().isEmpty());
        assertTrue(f.inventory.getSlots().get(45).mayPlace(shield()));
        PacketPlayerDigging.handleUseItem(f.player, InteractionHand.OFF_HAND);
        assertTrue(f.player.packetStateData.isSlowedByUsingItem());
    }

    @Test void registrationCloseRestoresNativeAndCannotRemoveReplacement() throws Exception {
        Fixture f = fixture(ClientVersion.V_26_2);
        var first = closeOffHand(f);
        assertFalse(MovementPhysics.offHandEnabled(f.player));
        first.close();
        assertTrue(MovementPhysics.offHandEnabled(f.player));
        var replacement = closeOffHand(f);
        first.close();
        assertFalse(MovementPhysics.offHandEnabled(f.player));
        replacement.close();
        assertNativeShieldRouting(f);
    }

    @Test void vehicleRetainsNativeMovementButClosedInventoryEligibility() throws Exception {
        Fixture f = fixture(ClientVersion.V_26_2);
        closeOffHand(f);
        when(f.player.inVehicle()).thenReturn(true);
        assertNull(MovementPhysics.profile(f.player));
        assertFalse(MovementPhysics.offHandEnabled(f.player));
        f.storage.setItem(9, shield());
        f.inventory.doClick(0, 9, WindowClickType.QUICK_MOVE);
        assertEquals(ItemTypes.SHIELD, f.inventory.getHeldItem().getType());
        assertTrue(f.storage.getItem(45).isEmpty());
        PacketPlayerDigging.handleUseItem(f.player, InteractionHand.MAIN_HAND);
        assertTrue(f.player.packetStateData.isSlowedByUsingItem());
    }

    @Test void armorQuickMoveRetainsNativeEquipmentRouting() throws Exception {
        Fixture f = fixture(ClientVersion.V_26_2);
        closeOffHand(f);
        f.storage.setItem(9, ItemStack.builder().type(ItemTypes.DIAMOND_HELMET).amount(1).build());
        f.inventory.doClick(0, 9, WindowClickType.QUICK_MOVE);
        assertTrue(f.storage.getItem(9).isEmpty());
        assertEquals(ItemTypes.DIAMOND_HELMET, f.storage.getItem(Inventory.SLOT_HELMET).getType());
        assertTrue(f.storage.getItem(45).isEmpty());
    }

    @Test void fullHotbarConservesShieldAndClosedSlotAllowsOnlyResidualPickup() throws Exception {
        Fixture f = fixture(ClientVersion.V_26_2);
        closeOffHand(f);
        for (int i = 36; i < 45; i++) f.storage.setItem(i, stone(64));
        f.storage.setItem(9, shield());
        assertTrue(f.inventory.quickMoveStack(9).isEmpty());
        assertEquals(ItemTypes.SHIELD, f.storage.getItem(9).getType());
        assertTrue(f.storage.getItem(45).isEmpty());
        assertFalse(f.inventory.getSlots().get(45).mayPlace(shield()));
        f.storage.setItem(45, shield());
        assertTrue(f.inventory.getSlots().get(45).mayPickup(f.player));
        f.inventory.doClick(0, 45, WindowClickType.QUICK_MOVE);
        assertTrue(f.storage.getItem(45).isEmpty());
        assertEquals(ItemTypes.SHIELD, f.storage.getItem(10).getType());
    }

    @Test void closedMergeCandidatesAndForbiddenSwapDoNotChangeRawResidualOrInventory() throws Exception {
        Fixture f = fixture(ClientVersion.V_26_2);
        closeOffHand(f);
        f.storage.setItem(45, stone(32));
        f.storage.setItem(9, stone(16));
        assertEquals(9, f.inventory.getSlotWithRemainingSpace(stone(4)));
        for (int i = 9; i < 45; i++) f.storage.setItem(i, stone(64));
        assertEquals(-1, f.inventory.getSlotWithRemainingSpace(stone(4)));
        f.storage.setItem(9, shield());
        f.inventory.doClick(40, 9, WindowClickType.SWAP);
        assertEquals(ItemTypes.SHIELD, f.storage.getItem(9).getType());
        assertEquals(32, f.storage.getItem(45).getAmount());
    }

    @Test void eligibleHandViewCannotUseLockedResidualWhileRawStorageStaysRecoverable() throws Exception {
        Fixture f = fixture(ClientVersion.V_26_2);
        closeOffHand(f);
        f.storage.setItem(45, shield());
        assertTrue(f.compensated.getOffHand().isEmpty());
        PacketPlayerDigging.handleUseItem(f.player, InteractionHand.OFF_HAND);
        assertFalse(f.player.packetStateData.isSlowedByUsingItem());
        assertEquals(ItemTypes.SHIELD, f.storage.getItem(45).getType());
    }
}
