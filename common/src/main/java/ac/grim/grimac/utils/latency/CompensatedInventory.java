package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.inventory.EquipmentType;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.inventory.AbstractContainerMenu;
import ac.grim.grimac.utils.inventory.inventory.MenuType;
import ac.grim.grimac.utils.inventory.inventory.NotImplementedMenu;
import ac.grim.grimac.utils.lists.CorrectingPlayerInventoryStorage;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenHorseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

// Updated to support modern 1.17 protocol
public class CompensatedInventory extends Check implements PacketCheck {
    private static final int PLAYER_INVENTORY_CASE = -1;
    private static final int UNSUPPORTED_INVENTORY_CASE = -2;
    // "Temporarily" public for debugging
    public final Inventory inventory;
    // "Temporarily" public for debugging
    public AbstractContainerMenu menu;
    // Not all inventories are supported due to complexity and version differences
    public boolean isPacketInventoryActive = true;
    public boolean needResend = false;
    public int stateID = 0; // Don't mess up the last sent state ID by changing it
    private int openWindowID = 0;
    // Special values:
    // Player inventory is -1
    // Unsupported inventory is -2
    private int packetSendingInventorySize = PLAYER_INVENTORY_CASE;

    public CompensatedInventory(GrimPlayer playerData) {
        super(playerData);

        CorrectingPlayerInventoryStorage storage = new CorrectingPlayerInventoryStorage(player, 46);
        inventory = new Inventory(playerData, storage);

        menu = inventory;
    }

    // Taken from https://www.spigotmc.org/threads/mapping-protocol-to-bukkit-slots.577724/
    public int getBukkitSlot(int packetSlot) {
        // 0 -> 5 are crafting slots, don't exist in bukkit
        if (packetSlot <= 4) {
            return -1;
        }
        // 5 -> 8 are armor slots in protocol, ordered helmets to boots
        if (packetSlot <= 8) {
            // 36 -> 39 are armor slots in bukkit, ordered boots to helmet. tbh I got this from trial and error.
            return (7 - packetSlot) + 36;
        }
        // By a coincidence, non-hotbar inventory slots match.
        if (packetSlot <= 35) {
            return packetSlot;
        }
        // 36 -> 44 are hotbar slots in protocol
        if (packetSlot <= 44) {
            // 0 -> 9 are hotbar slots in bukkit
            return packetSlot - 36;
        }
        // 45 is offhand is packet, it is 40 in bukkit
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) && packetSlot == 45) {
            return 40;
        }
        return -1;
    }

    // Meant for 1.17+ clients who send changed slots, making the server not send the entire inventory
    private void markPlayerSlotAsChanged(int clicked) {
        // Player inventory
        if (openWindowID == 0) {
            inventory.getInventoryStorage().handleClientClaimedSlotSet(clicked);
            return;
        }

        // We don't know size of the inventory, so we can't do anything
        // We will resync later.
        if (menu instanceof NotImplementedMenu) return;

        // 9-45 are the player inventory slots that are used
        // There are 36 player slots in each menu that we care about and track.
        int nonPlayerInvSize = menu.getSlots().size() - 36 + 9;
        int playerInvSlotclicked = clicked - nonPlayerInvSize;
        // Bypass player inventory
        inventory.getInventoryStorage().handleClientClaimedSlotSet(playerInvSlotclicked);
    }

    public ItemStack getItemInHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? getHeldItem() : getOffHand();
    }

    /**
     * Marks that the server has updated a slot by the specified ID in the specified inventory ID.
     * @param clicked the updated slot
     * @param windowID the inventory ID
     */
    private void markServerForChangingSlot(int clicked, int windowID) {
        // Unsupported inventory
        if (packetSendingInventorySize == UNSUPPORTED_INVENTORY_CASE) return;
        // Player inventory
        if (packetSendingInventorySize == PLAYER_INVENTORY_CASE || windowID == 0) {
            // Result slot isn't included in storage, we must ignore it
            inventory.getInventoryStorage().handleServerCorrectSlot(clicked);
            return;
        }
        // See note in above method.
        int nonPlayerInvSize = menu.getSlots().size() - 36 + 9;
        int playerInvSlotclicked = clicked - nonPlayerInvSize;

        inventory.getInventoryStorage().handleServerCorrectSlot(playerInvSlotclicked);
    }

    public ItemStack getHeldItem() {
        ItemStack item = isPacketInventoryActive || player.platformPlayer == null ? inventory.getHeldItem() :
                player.platformPlayer.getInventory().getItemInHand();
        return item == null ? ItemStack.EMPTY : item;
    }

    public ItemStack getOffHand() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9))
            return ItemStack.EMPTY;
        ItemStack item = isPacketInventoryActive || player.platformPlayer == null ? inventory.getOffhand() :
                player.platformPlayer.getInventory().getItemInOffHand();
        return item == null ? ItemStack.EMPTY : item;
    }

    public ItemStack getHelmet() {
        ItemStack item = isPacketInventoryActive || player.platformPlayer == null ? inventory.getHelmet() :
                player.platformPlayer.getInventory().getHelmet();
        return item == null ? ItemStack.EMPTY : item;
    }

    public ItemStack getChestplate() {
        ItemStack item = isPacketInventoryActive || player.platformPlayer == null ? inventory.getChestplate() :
                player.platformPlayer.getInventory().getChestplate();
        return item == null ? ItemStack.EMPTY : item;
    }

    public ItemStack getLeggings() {
        ItemStack item = isPacketInventoryActive || player.platformPlayer == null ? inventory.getLeggings() :
                player.platformPlayer.getInventory().getLeggings();
        return item == null ? ItemStack.EMPTY : item;
    }

    public ItemStack getBoots() {
        ItemStack item = isPacketInventoryActive || player.platformPlayer == null ? inventory.getBoots() :
                player.platformPlayer.getInventory().getBoots();
        return item == null ? ItemStack.EMPTY : item;
    }

    private ItemStack getByEquipmentType(EquipmentType type) {
        return switch (type) {
            case HEAD -> getHelmet();
            case CHEST -> getChestplate();
            case LEGS -> getLeggings();
            case FEET -> getBoots();
            case OFFHAND -> getOffHand();
            case MAINHAND -> getHeldItem();
        };
    }

    public boolean hasItemType(ItemType type) {
        if (isPacketInventoryActive || player.platformPlayer == null)
            return inventory.hasItemType(type);

        // Fall back to platform inventories
        for (ItemStack itemStack : player.platformPlayer.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == type) return true;
        }
        return false;
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem item = new WrapperPlayClientUseItem(event);

            ItemStack use = item.getHand() == InteractionHand.MAIN_HAND ? getHeldItem() : getOffHand();

            EquipmentType equipmentType = EquipmentType.getEquipmentSlotForItem(use);
            if (equipmentType != null) {
                int slot;
                switch (equipmentType) {
                    case HEAD -> slot = Inventory.SLOT_HELMET;
                    case CHEST -> slot = Inventory.SLOT_CHESTPLATE;
                    case LEGS -> slot = Inventory.SLOT_LEGGINGS;
                    case FEET -> slot = Inventory.SLOT_BOOTS;
                    default -> {
                        return; // Not armor, therefore we shouldn't run this code
                    }
                }

                ItemStack currentEquippedItem = getByEquipmentType(equipmentType);
                // Only 1.19.4+ clients support swapping with non-empty items
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_19_4) && !currentEquippedItem.isEmpty())
                    return;

                // 1.19.4+ clients support swapping with non-empty items
                int swapItemSlot = item.getHand() == InteractionHand.MAIN_HAND ? inventory.selected + Inventory.HOTBAR_OFFSET : Inventory.SLOT_OFFHAND;

                // Mojang implemented this stupidly, I rewrote their item swap code to make it somewhat cleaner.
                // Slot in hotbar
                inventory.getInventoryStorage().handleClientClaimedSlotSet(swapItemSlot);
                inventory.getInventoryStorage().setItem(swapItemSlot, currentEquippedItem);

                // Equipment slot
                inventory.getInventoryStorage().handleClientClaimedSlotSet(slot);
                inventory.getInventoryStorage().setItem(slot, use);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            // 1.8 clients don't predict dropping items
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

            if (dig.getAction() == DiggingAction.DROP_ITEM) {
                ItemStack heldItem = getHeldItem();
                if (heldItem != null) {
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    if (heldItem.getAmount() <= 0) {
                        heldItem = null;
                    }
                }
                inventory.setHeldItem(heldItem);
                inventory.getInventoryStorage().handleClientClaimedSlotSet(Inventory.HOTBAR_OFFSET + player.packetStateData.lastSlotSelected);
            }

            if (dig.getAction() == DiggingAction.DROP_ITEM_STACK) {
                inventory.setHeldItem(null);
                inventory.getInventoryStorage().handleClientClaimedSlotSet(Inventory.HOTBAR_OFFSET + player.packetStateData.lastSlotSelected);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            final int slot = new WrapperPlayClientHeldItemChange(event).getSlot();

            // Stop people from spamming the server with an out-of-bounds exception
            if (slot > 8 || slot < 0) return;

            inventory.selected = slot;
        }

        if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            WrapperPlayClientCreativeInventoryAction action = new WrapperPlayClientCreativeInventoryAction(event);
            if (player.gamemode != GameMode.CREATIVE) return;

            boolean valid = action.getSlot() >= 1 &&
                    (PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_8) ?
                            action.getSlot() <= 45 : action.getSlot() < 45);

            if (valid) {
                inventory.getSlot(action.getSlot()).set(action.getItemStack());
                inventory.getInventoryStorage().handleClientClaimedSlotSet(action.getSlot());
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW && !event.isCancelled()) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);

            // How is this possible? Maybe transaction splitting.
            if (click.getWindowId() != openWindowID) {
                return;
            }

            // Don't care about this click since we can't track it.
            if (menu instanceof NotImplementedMenu) {
                return;
            }

            // Mark the slots the player has changed as changed, then continue simulating what they changed
            Optional<Map<Integer, ItemStack>> slots = click.getSlots();
            slots.ifPresent(integerItemStackMap -> integerItemStackMap.keySet().forEach(this::markPlayerSlotAsChanged));

            // 0 for left click
            // 1 for right click
            int button = click.getButton();
            // Offset by the number of slots in the inventory actively open
            // Is -999 when clicking off the screen
            int slot = click.getSlot();
            // Self-explanatory, look at the enum's values
            WrapperPlayClientClickWindow.WindowClickType clickType = click.getWindowClickType();

            if (slot == -1 || slot == -999 || slot < menu.getSlots().size()) {
                menu.doClick(button, slot, clickType);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            this.closeActiveInventory();
        }
    }

    public void markSlotAsResyncing(BlockPlace place) {
        // Update held item tracking
        if (place.hand == InteractionHand.MAIN_HAND) {
            inventory.getInventoryStorage().handleClientClaimedSlotSet(Inventory.HOTBAR_OFFSET + player.packetStateData.lastSlotSelected);
        } else {
            inventory.getInventoryStorage().handleServerCorrectSlot(Inventory.SLOT_OFFHAND);
        }
    }

    public void onBlockPlace(BlockPlace place) {
        if (player.gamemode != GameMode.CREATIVE && place.itemStack.getType() != ItemTypes.POWDER_SNOW_BUCKET) {
            markSlotAsResyncing(place);
            place.itemStack.setAmount(place.itemStack.getAmount() - 1);
        }
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        // Not 1:1 MCP, based on Wiki.VG to be simpler as we need less logic...
        // For example, we don't need permanent storage, only storing data until the client closes the window
        // We also don't need a lot of server-sided only logic
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow open = new WrapperPlayServerOpenWindow(event);

            MenuType menuType = MenuType.getMenuType(open.getType());

            AbstractContainerMenu newMenu;
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) {
                newMenu = MenuType.getMenuFromID(player, inventory, menuType);
            } else {
                newMenu = MenuType.getMenuFromString(player, inventory, open.getLegacyType(), open.getLegacySlots(), open.getHorseId());
            }

            packetSendingInventorySize = newMenu instanceof NotImplementedMenu ? UNSUPPORTED_INVENTORY_CASE : newMenu.getSlots().size();

            // There doesn't seem to be a check against using 0 as the window ID - let's consider that an invalid packet
            // It will probably mess up a TON of logic both client and server sided, so don't do that!
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                openWindowID = open.getContainerId();
                menu = newMenu;
                isPacketInventoryActive = !(newMenu instanceof NotImplementedMenu);
                needResend = newMenu instanceof NotImplementedMenu;
            });
        }

        // I'm not implementing this lol
        if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            WrapperPlayServerOpenHorseWindow open = new WrapperPlayServerOpenHorseWindow(event);

            packetSendingInventorySize = UNSUPPORTED_INVENTORY_CASE;
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                isPacketInventoryActive = false;
                needResend = true;
                openWindowID = open.getWindowId();
            });
        }

        // 1:1 MCP
        if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            packetSendingInventorySize = PLAYER_INVENTORY_CASE;

            // Disregard provided window ID, client doesn't care...
            // We need to do this because the client doesn't send a packet when closing the window
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), this::closeActiveInventory);
        }

        // Should be 1:1 MCP
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems items = new WrapperPlayServerWindowItems(event);
            stateID = items.getStateId();

            List<ItemStack> slots = items.getItems();
            for (int i = 0; i < slots.size(); i++) {
                markServerForChangingSlot(i, items.getWindowId());
            }

            final int cachedPacketInvSize = packetSendingInventorySize;
            final AtomicBoolean updatedValue = new AtomicBoolean();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                // Never true when the inventory is unsupported.
                // Vanilla ALWAYS sends the entire inventory to resync, this is a valid thing to check
                // 01/07/2025: Somehow, the server sends a window id 0 update when the player is not in their inventory?
                // I guess just revert isPacketInventoryActive if the player has a NotImplementedMenu open?
                // Regardless, the client does accept this packet and update its inventory, so we must do the same.
                if (slots.size() == cachedPacketInvSize || items.getWindowId() == 0) {
                    isPacketInventoryActive = true;
                    updatedValue.set(true);
                }
            });

            if (items.getWindowId() == 0) { // Player inventory
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    if (!isPacketInventoryActive) return;
                    for (int i = 0; i < slots.size(); i++) {
                        inventory.getSlot(i).set(slots.get(i));
                    }
                    if (items.getCarriedItem().isPresent()) {
                        inventory.setCarried(items.getCarriedItem().get());
                    }
                });
            } else {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    if (!isPacketInventoryActive) return;
                    if (items.getWindowId() == openWindowID) {
                        for (int i = 0; i < slots.size(); i++) {
                            menu.getSlot(i).set(slots.get(i));
                        }
                    }
                    if (items.getCarriedItem().isPresent()) {
                        inventory.setCarried(items.getCarriedItem().get());
                    }
                });
            }

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                // The server sent a packet for the player's inventory when they had another inventory open - revert.
                if (updatedValue.get() && !menu.equals(inventory)) {
                    isPacketInventoryActive = false;
                }
            });
        }

        // This packet replaces SET_SLOT for player inventory for 1.21.2+
        if (event.getPacketType() == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory slot = new WrapperPlayServerSetPlayerInventory(event);
            final int slotID = slot.getSlot();
            final ItemStack item = slot.getStack();

            inventory.getInventoryStorage().handleServerCorrectSlot(slotID);

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                if (!isPacketInventoryActive) return;
                inventory.getSlot(slotID).set(item);
            });
        }

        // Also 1:1 MCP
        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            // Only edit hotbar (36 to 44) if window ID is 0
            // Set cursor by putting -1 as window ID and as slot
            // Window ID -2 means any slot can be used
            WrapperPlayServerSetSlot slot = new WrapperPlayServerSetSlot(event);
            final int slotID = slot.getSlot();
            final int inventoryID = slot.getWindowId();
            final ItemStack item = slot.getItem();

            if (inventoryID == -2) { // Direct inventory change
                inventory.getInventoryStorage().handleServerCorrectSlot(slotID);
            } else if (inventoryID == 0) { // Inventory change through window ID, no crafting result
                inventory.getInventoryStorage().handleServerCorrectSlot(slotID);
            } else {
                markServerForChangingSlot(slotID, inventoryID);
            }

            stateID = slot.getStateId();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                if (!isPacketInventoryActive) return;
                if (inventoryID == -1) { // Carried item
                    inventory.setCarried(item);
                } else if (inventoryID == -2) { // Direct inventory change (only applied if valid slot)
                    if (inventory.getInventoryStorage().getSize() > slotID && slotID >= 0) {
                        inventory.getInventoryStorage().setItem(slotID, item);
                    }
                } else if (inventoryID == 0) { // Player inventory
                    // This packet can only be used to edit the hotbar and offhand of the player's inventory if
                    // window ID is set to 0 (slots 36 through 45) if the player is in creative, with their inventory open,
                    // and not in their survival inventory tab. Otherwise, when window ID is 0, it can edit any slot in the player's inventory.
                    if (slotID >= 0 && slotID <= 45) {
                        inventory.getSlot(slotID).set(item);
                    }
                } else if (inventoryID == openWindowID) { // Opened inventory (if not valid, client crashes)
                    menu.getSlot(slotID).set(item);
                }
            });
        }
    }

    /**
     * Closes the player's currently open inventory on the client by resetting to the player's inventory.
     */
    private void closeActiveInventory() {
        isPacketInventoryActive = true;
        openWindowID = 0;
        menu = inventory;
        menu.setCarried(ItemStack.EMPTY); // Reset carried item
    }
}
