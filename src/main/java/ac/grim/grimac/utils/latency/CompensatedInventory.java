package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.inventory.AbstractContainerMenu;
import ac.grim.grimac.utils.inventory.inventory.HorseMenu;
import ac.grim.grimac.utils.inventory.inventory.MenuTypes;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenHorseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import org.bukkit.Bukkit;

import java.util.List;

// hmmm... 1.17 added some interesting stuff to the packet...
// It seems to actually add essentials information to the packet...
//
// the client sends the itemstacks that it changes... which is very nice.
// although let's do it the multi-version way anyways as we have proper lag compensated so it has the same result
//
// for the first time... thanks mojang!
public class CompensatedInventory extends PacketCheck {
    // Temporarily public for debugging
    public Inventory inventory;
    // Temporarily public for debugging
    public AbstractContainerMenu menu;
    // Here are the mappings from the geniuses at Mojang
    // 1, 2, 3, 4 and 0 are the crafting table
    // 5, 6, 7, 8 are the armor slots from helmet to boots
    // 45 is the offhand, only existing on 1.9+ servers
    // 36-44 is the hotbar
    // 9 is top left, through 35 being the bottom right.
    int openWindowID = 0;

    public CompensatedInventory(GrimPlayer playerData) {
        super(playerData);

        InventoryStorage storage = new InventoryStorage(46);
        inventory = new Inventory(playerData, storage);

        menu = inventory;
    }

    public ItemStack getHeldItem() {
        return inventory.getHeldItem();
    }

    public ItemStack getOffHand() {
        return inventory.getOffhand();
    }

    public ItemStack getHelmet() {
        return inventory.getHelmet();
    }

    public ItemStack getChestplate() {
        return inventory.getChestplate();
    }

    public ItemStack getLeggings() {
        return inventory.getLeggings();
    }

    public ItemStack getBoots() {
        return inventory.getBoots();
    }

    public boolean hasItemType(ItemType type) {
        return inventory.hasItemType(type);
    }

    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            if (dig.getAction() == WrapperPlayClientPlayerDigging.Action.DROP_ITEM) {
                ItemStack heldItem = inventory.getHeldItem();
                if (heldItem != null) {
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    if (heldItem.getAmount() <= 0) {
                        heldItem = null;
                    }
                }
                inventory.setHeldItem(heldItem);
            }

            if (dig.getAction() == WrapperPlayClientPlayerDigging.Action.DROP_ITEM_STACK) {
                inventory.setHeldItem(null);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            WrapperPlayClientHeldItemChange slot = new WrapperPlayClientHeldItemChange(event);

            // Stop people from spamming the server with an out-of-bounds exception
            if (slot.getSlot() > 8) return;

            inventory.selected = slot.getSlot();
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);

            // 0 for left click
            // 1 for right click
            int button = click.getButton();
            // Offset by the number of slots in the inventory actively open
            // Is -999 when clicking off the screen
            int slot = click.getSlot();
            // Self-explanatory, look at the enum's values
            WrapperPlayClientClickWindow.WindowClickType clickType = click.getWindowClickType();

            Bukkit.broadcastMessage("Clicked " + button + " " + slot + " " + clickType);

            menu.doClick(button, slot, clickType);
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            menu = inventory;
            menu.setCarried(ItemStack.EMPTY); // Reset carried item
        }
    }

    public boolean isEmpty(ItemStack stack) {
        if (stack == null) return true;
        if (stack.getType() == ItemTypes.AIR) return true;
        return stack.getAmount() <= 0;
    }

    public void onPacketSend(final PacketSendEvent event) {
        // Not 1:1 MCP, based on Wiki.VG to be simpler as we need less logic...
        // For example, we don't need permanent storage, only storing data until the client closes the window
        // We also don't need a lot of server-sided only logic
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow open = new WrapperPlayServerOpenWindow(event);

            // There doesn't seem to be a check against using 0 as the window ID - let's consider that an invalid packet
            // It will probably mess up a TON of logic both client and server sided, so don't do that!
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                openWindowID = open.getContainerId();

                AbstractContainerMenu newMenu = MenuTypes.getMenuFromID(player, inventory, open.getType());
                if (newMenu != null) {
                    menu = newMenu;
                }
            });
        }

        // Supports plugins sending stupid packets for stupid reasons that point to an invalid horse
        // Should be correct? Unsure. Not 1:1 MCP.
        if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            WrapperPlayServerOpenHorseWindow packet = new WrapperPlayServerOpenHorseWindow(event);
            int windowID = packet.getWindowId();
            int slotCount = packet.getSlotCount();
            int entityID = packet.getEntityId();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                openWindowID = windowID;
                menu = new HorseMenu(player, inventory, slotCount, entityID);
            });
        }

        // Is this mapped wrong?  Should it be ClientboundMerchantOffersPacket?  What is this packet?
        if (event.getPacketType() == PacketType.Play.Server.TRADE_LIST) {

        }

        // 1:1 MCP
        if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            // Disregard provided window ID, client doesn't care...
            // We need to do this because the client doesn't send a packet when closing the window
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                openWindowID = 0;
                menu = inventory;
                menu.setCarried(ItemStack.EMPTY); // Reset carried item
            });
        }

        // Should be 1:1 MCP
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems items = new WrapperPlayServerWindowItems(event);

            // State ID is how the game tries to handle latency compensation.
            // Unsure if we need to know about this.
            if (items.getWindowId() == 0) { // Player inventory
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    List<ItemStack> slots = items.getItems();
                    for (int i = 0; i < slots.size(); i++) {
                        inventory.getSlot(i).set(slots.get(i));
                    }
                });
            } else {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    if (items.getWindowId() == openWindowID) {
                        List<ItemStack> slots = items.getItems();
                        for (int i = 0; i < slots.size(); i++) {
                            menu.getSlot(i).set(slots.get(i));
                        }
                    }
                });
            }
        }

        // Also 1:1 MCP
        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            // Only edit hotbar (36 to 44) if window ID is 0
            // Set cursor by putting -1 as window ID and as slot
            // Window ID -2 means any slot can be used
            WrapperPlayServerSetSlot slot = new WrapperPlayServerSetSlot(event);

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                if (slot.getWindowId() == -1) { // Carried item
                    inventory.setCarried(slot.getItem());
                } else if (slot.getWindowId() == -2) { // Any slot is allowed to change in inventory
                    inventory.getSlot(slot.getSlot()).set(slot.getItem());
                } else if (slot.getWindowId() == 0) { // Player hotbar
                    if (slot.getSlot() >= 36 && slot.getSlot() <= 44) { // Client ignored if not in range
                        inventory.getSlot(slot.getSlot()).set(slot.getItem());
                    }
                } else if (slot.getWindowId() == openWindowID) { // Opened inventory
                    menu.getSlot(slot.getSlot()).set(slot.getItem());
                }
            });
        }
    }
}
