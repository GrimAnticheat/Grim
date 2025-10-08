package ac.grim.grimac.utils.lists;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is responsible for lag compensation of the player's inventory
 * Since I don't want to spend months finding version differences in inventory
 * Or copy (and debug) over around 5k lines of code to accomplish inventories
 * Grim uses a hybrid system for inventories - we lag compensate but rely on the server
 * for the ultimate source of truth, and resync if what we found is different from what the server sees
 * <p>
 * This also patches most desync's that happen with inventories on some versions like 1.8 or
 * other desync's introduced by mojang or viabackwards
 *
 * <p>
 * To accomplish this we:
 * - Track items changed when the player swaps or moves items in a basic inventory
 * - Track items when the player has placed a block, for example
 * - Track other item predictions by the client
 * <p>
 * There is somewhat of a race condition
 * The server's inventory state can only truly be read at the start of the tick
 * However, we read inventories async for performance reasons
 * This shouldn't cause any negative issues in practice, but it technically is wrong
 * <p>
 * Apply this only to the player's inventory for simplicity reasons
 * Horses and stuff, the metadata for saddles is server authoritative
 * No inventory directly affects us other than the player's inventory.
 */
public class CorrectingPlayerInventoryStorage extends InventoryStorage {

    // TODO: How the hell does creative mode work?
    private static final Set<String> SUPPORTED_INVENTORIES = new HashSet<>(
            Arrays.asList("CHEST", "DISPENSER", "DROPPER", "PLAYER", "ENDER_CHEST", "SHULKER_BOX", "BARREL", "CRAFTING", "CREATIVE")
    );
    private final GrimPlayer player;
    // The key for this map is the inventory slot ID
    // The value for this map is the transaction that we care about
    // Returns -1 if the entry is null
    private final Map<Integer, Integer> serverIsCurrentlyProcessingThesePredictions = new ConcurrentHashMap<>();
    // A list of predictions the client has made for inventory changes
    // Remove if the server rejects these changes
    private final Map<Integer, Integer> pendingFinalizedSlot = new ConcurrentHashMap<>();

    public CorrectingPlayerInventoryStorage(GrimPlayer player, int size) {
        super(size);
        this.player = player;
    }

    // 1.17+ clients send what slots they have changed.  This makes our jobs much easier.
    // Required as server now only sends changes if client disagrees with them.
    public void handleClientClaimedSlotSet(int slotID) {
        if (slotID >= 0 && slotID <= Inventory.ITEMS_END) {
            pendingFinalizedSlot.put(slotID, GrimAPI.INSTANCE.getTickManager().currentTick + 5);
        }
    }

    public void handleServerCorrectSlot(int slotID) {
        if (slotID >= 0 && slotID <= Inventory.ITEMS_END) {
            serverIsCurrentlyProcessingThesePredictions.put(slotID, player.lastTransactionSent.get());
        }
    }

    // This is more meant for pre-1.17 clients, but mojang fucked up netcode AGAIN in 1.17, so
    // we must use this for 1.17 clients as well... at least you tried Mojang.
    @Override
    public void setItem(int item, ItemStack stack) {
        // If there is a more recent change to this one, don't override it
        int finalTransaction = serverIsCurrentlyProcessingThesePredictions.getOrDefault(item, -1);

        // If the server is currently sending a packet to the player AND it is the final change to the slot
        // OR, the client was in control of setting this slot
        if (finalTransaction == -1 || player.lastTransactionReceived.get() >= finalTransaction) {
            // This is the last change for this slot, try to resync this slot if possible
            pendingFinalizedSlot.put(item, GrimAPI.INSTANCE.getTickManager().currentTick + 5);
            serverIsCurrentlyProcessingThesePredictions.remove(item);
        }

        super.setItem(item, stack);
    }

    /**
     * Checks that the specified slot is in sync with the server's and resyncs if needed.
     * @param slot the slot to check
     */
    private void checkThatBukkitIsSynced(int slot) {
        // The player isn't fully logged in yet, don't bother checking
        if (player.platformPlayer == null) return;
        // We aren't tracking the player's inventory, so don't bother
        if (!player.inventory.isPacketInventoryActive) return;

        // Bukkit uses different slot ID's to vanilla
        int bukkitSlot = player.inventory.getBukkitSlot(slot); // 8 -> 39, should be 36

        if (bukkitSlot != -1) {
            ItemStack existing = getItem(slot);
            ItemStack toPE = player.platformPlayer.getInventory().getStack(bukkitSlot, slot);

            if (existing.getType() != toPE.getType() || existing.getAmount() != toPE.getAmount()) {
                GrimAPI.INSTANCE.getScheduler().getEntityScheduler().execute(player.platformPlayer, GrimAPI.INSTANCE.getGrimPlugin(),
                        () -> player.platformPlayer.updateInventory(), null, 0);
                setItem(slot, toPE);
            }
        }
    }

    public void tickWithBukkit() {
        if (player.platformPlayer == null) return;

        // Loop all slot changes the client has predicted and check that the server has accepted them
        int tickID = GrimAPI.INSTANCE.getTickManager().currentTick;
        for (Iterator<Map.Entry<Integer, Integer>> it = pendingFinalizedSlot.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> entry = it.next();
            // If x ticks have passed, check the slot is equal to the server slot and remove
            if (entry.getValue() <= tickID) {
                checkThatBukkitIsSynced(entry.getKey());
                it.remove();
            }
        }

        // If the player's inventory needs to be resent so that Grim can enable the player's packet inventory again
        // Then resend once the player has a supported inventory to activate that.
        if (player.inventory.needResend) {
            GrimAPI.INSTANCE.getScheduler().getEntityScheduler().execute(player.platformPlayer, GrimAPI.INSTANCE.getGrimPlugin(), () -> {
                // Potential race condition doing this multiple times
                if (!player.inventory.needResend) return;

                if (SUPPORTED_INVENTORIES.contains(player.platformPlayer.getInventory().getOpenInventoryKey().toUpperCase(Locale.ROOT))) {
                    player.inventory.needResend = false;
                    player.platformPlayer.updateInventory();
                }
            }, null, 0);
        }

        // Every five ticks, we pull a new item for the player
        // This means no desync will last longer than 10 seconds
        // (Required as mojang has screwed up some things with inventories that we can't easily fix)
        // Don't spam this as it could cause lag (I was getting 0.3 ms to query this, this is done async though)
        if (tickID % 5 == 0) {
            int slotToCheck = (tickID / 5) % getSize();
            // If both these things are true, there is nothing that should be broken.
            if (!pendingFinalizedSlot.containsKey(slotToCheck) && !serverIsCurrentlyProcessingThesePredictions.containsKey(slotToCheck)) {
                checkThatBukkitIsSynced(slotToCheck);
            }
        }
    }
}
