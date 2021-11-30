package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.in.blockdig.WrappedPacketInBlockDig;
import io.github.retrooper.packetevents.packetwrappers.play.in.closewindow.WrappedPacketInCloseWindow;
import io.github.retrooper.packetevents.packetwrappers.play.in.helditemslot.WrappedPacketInHeldItemSlot;
import io.github.retrooper.packetevents.packetwrappers.play.in.windowclick.WrappedPacketInWindowClick;
import io.github.retrooper.packetevents.packetwrappers.play.out.openwindow.WrappedPacketOutOpenWindow;
import io.github.retrooper.packetevents.packetwrappers.play.out.setslot.WrappedPacketOutSetSlot;
import io.github.retrooper.packetevents.packetwrappers.play.out.windowitems.WrappedPacketOutWindowItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

// TODO: We must handle the player clicking on their inventory, dragging, shift clicking...
// This is for proxy support and mojang fucked up this again... WHY DID THEY REMOVE ITEMSTACK FROM THE PACKET!
// Stop removing essential information from the packet!  Damn it mojang.
public class CompensatedInventory extends PacketCheck {
    // Here are the mappings from the geniuses at Mojang
    // 1, 2, 3, 4 and 0 are the crafting table
    // 5, 6, 7, 8 are the armor slots from helmet to boots
    // 45 is the offhand, only existing on 1.9+ servers
    // 36-44 is the hotbar
    // 9 is top left, through 35 being the bottom right.
    ItemStack[] playerInventory = new ItemStack[45];
    ItemStack carriedItem = null;
    List<ItemStack> openedInventory = new ArrayList<>();
    // ALL OPERATIONS MUST WORK WITH AN OFFSET BECAUSE MOJANG ARE IDIOTS
    // Without an active inventory open, the offset is 0
    int offset = 0;
    int openWindowID = 0;
    int heldSlot = 0;

    public CompensatedInventory(GrimPlayer playerData) {
        super(playerData);
    }

    private void setItemWithOffset(int slot, ItemStack item) {
        int withOffset = slot - offset;

        if (withOffset < 0) { // Not in player inventory
            openedInventory.set(slot, item);
        } else if (withOffset < 45) { //
            playerInventory[withOffset] = item;
        }
    }

    private ItemStack getItem(int slot) {
        int withOffset = slot - offset;

        if (withOffset < 0) { // Not in player inventory
            return openedInventory.get(slot);
        } else if (withOffset < 45) {
            return playerInventory[withOffset];
        }

        return null;
    }

    public ItemStack getHeldItem() {
        return playerInventory[heldSlot + 36];
    }

    public void setHeldItem(ItemStack item) {
        playerInventory[heldSlot + 36] = item;
    }

    public void onPacketReceive(final PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.BLOCK_DIG) {
            WrappedPacketInBlockDig dig = new WrappedPacketInBlockDig(event.getNMSPacket());

            if (dig.getDigType() == WrappedPacketInBlockDig.PlayerDigType.DROP_ITEM) {
                ItemStack heldItem = getHeldItem();
                if (heldItem != null) {
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    if (heldItem.getAmount() <= 0) {
                        heldItem = null;
                    }
                }
                setHeldItem(heldItem);
            }

            if (dig.getDigType() == WrappedPacketInBlockDig.PlayerDigType.DROP_ALL_ITEMS) {
                setHeldItem(null);
            }
        }

        if (event.getPacketId() == PacketType.Play.Client.HELD_ITEM_SLOT) {
            WrappedPacketInHeldItemSlot slot = new WrappedPacketInHeldItemSlot(event.getNMSPacket());

            // Stop people from spamming the server with an out-of-bounds exception
            if (slot.getCurrentSelectedSlot() > 8) return;

            heldSlot = slot.getCurrentSelectedSlot();
        }

        // Not 1:1 MCP as I couldn't figure out what it did, I observed the packet values in-game
        // and then tried replicating the behavior...
        if (event.getPacketId() == PacketType.Play.Client.WINDOW_CLICK) {
            WrappedPacketInWindowClick click = new WrappedPacketInWindowClick(event.getNMSPacket());
            short button = click.getActionNumber().get();
            int slot = click.getWindowSlot();
            int mode = click.getMode();

            if ((mode == 0 || mode == 1) && (button == 0 || button == 1)) {
                if (slot == -999) {
                    if (button == 0) { // Left click
                        carriedItem = null;
                    } else { // Right click
                        carriedItem.setAmount(carriedItem.getAmount() - 1);
                        if (carriedItem.getAmount() <= 0) {
                            carriedItem = null;
                        }
                    }
                } else if (mode == 1) { // Quick move
                    if (slot < 0) return;

                    //for(ItemStack itemstack9 = this.quickMoveStack(p_150434_, slotID); !itemstack9.isEmpty() && ItemStack.isSame(slot6.getItem(), itemstack9); itemstack9 = this.quickMoveStack(p_150434_, slotID)) {
                    //}
                } else { // Pickup mode
                    if (slot < 0) return;

                }
            } else if (mode == 1) { // Quick move

            } else { //

            }


            if (click.getMode() == 0) { // Left or right click
                if (click.getWindowSlot() == -999) { // Clicking outside of inventory
                    carriedItem = null; // Client predicts throwing the item
                } else { // Store this click onto the player cursor
                    carriedItem = getItem(click.getWindowSlot());
                    setItemWithOffset(click.getWindowSlot(), null);
                }
            } else if (click.getMode() == 1) { // Shift click

            } else if (click.getMode() == 2) { // Using number keys from 1-9, plus offhand is 40

            } else if (click.getMode() == 4) { // Drop item
                if (click.getWindowButton() == 0) { // Drop key
                    ItemStack droppedItem = getItem(click.getWindowSlot()); // Subtract one from itemstack

                    if (droppedItem != null) {
                        droppedItem.setAmount(droppedItem.getAmount() - 1);
                        if (droppedItem.getAmount() <= 0) {
                            droppedItem = null;
                        }
                    }

                    setItemWithOffset(click.getWindowSlot(), droppedItem);
                } else if (click.getWindowButton() == 1) { // Control drop key
                    setItemWithOffset(click.getWindowSlot(), null); // Client predicts dropping the item
                }
            } else if (click.getMode() == 5) { // Dragging
                // If a player sends packets out of order, then it resets their drag status
                if (click.getWindowSlot() == 0) { // Start left mouse drag

                } else if (click.getWindowButton() == 4) { // Start right mouse drag

                } else if (click.getWindowSlot() == 2) { // End left mouse drag

                } else if (click.getWindowButton() == 6) { // End right mouse drag

                } else if (click.getWindowSlot() == 9 || click.getWindowSlot() == 10) { // Middle mouse, resets drag

                }
            } else if (click.getMode() == 6) { // Double click

            }
        }

        if (event.getPacketId() == PacketType.Play.Client.CLOSE_WINDOW) {
            WrappedPacketInCloseWindow close = new WrappedPacketInCloseWindow(event.getNMSPacket());
            // Check for currently open inventory, close if the ID matches.
        }
    }

    // TODO: Implement bundle support...
    public boolean overrideStackedOnOther(ItemStack stack, int clickAction) {
        return false;
    }

    // TODO: Implement bundle support...
    public boolean overrideOtherStackedOnMe(ItemStack stack, int clickAction) {
        return false;
    }

    public boolean isEmpty(ItemStack stack) {
        if (stack == null) return true;
        if (stack.getType() == Material.AIR) return true;
        return stack.getAmount() <= 0;
    }

    public void onPacketSend(final PacketPlaySendEvent event) {
        // Not 1:1 MCP, based on Wiki.VG to be simpler as we need less logic...
        // We don't care if it's a chest or a dispenser, we just need to know it's size because of
        // how mojang stupidly implemented inventories.
        if (event.getPacketId() == PacketType.Play.Server.OPEN_WINDOW) {
            WrappedPacketOutOpenWindow open = new WrappedPacketOutOpenWindow(event.getNMSPacket());

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                openWindowID = open.getWindowId();
                // TODO: Pre-1.14 support, which uses strings for some reason.
                offset = getOffset(open.getInventoryTypeId().get());
                openedInventory = new ArrayList<>(offset);
            });
        }

        // 1:1 MCP - supports plugins sending stupid packets for stupid reasons that point to an invalid horse
        if (event.getPacketId() == PacketType.Play.Server.OPEN_WINDOW_HORSE) {
            WrappedPacket packet = new WrappedPacket(event.getNMSPacket());
            int windowID = packet.readInt(0);
            int slotCount = packet.readInt(1);
            int entityID = packet.readInt(2);

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                PacketEntity hopefullyAHorse = player.compensatedEntities.getEntity(entityID);

                if (hopefullyAHorse instanceof PacketEntityHorse) {
                    openWindowID = windowID;
                    offset = slotCount;
                    openedInventory = new ArrayList<>(offset);
                }
            });
        }

        // Is this mapped wrong?  Should it be ClientboundMerchantOffersPacket?  What is this packet?
        if (event.getPacketId() == PacketType.Play.Server.OPEN_WINDOW_MERCHANT) {

        }

        // 1:1 MCP
        if (event.getPacketId() == PacketType.Play.Server.CLOSE_WINDOW) {
            // Disregard provided window ID, client doesn't care...
            openWindowID = 0;
            offset = 0;
            openedInventory = new ArrayList<>();
        }

        // Should be 1:1 MCP
        if (event.getPacketId() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrappedPacketOutWindowItems items = new WrappedPacketOutWindowItems(event.getNMSPacket());

            // State ID is how the game tries to handle latency compensation.
            // Unsure if we need to know about this.
            if (items.getWindowId() == 0) { // Player inventory
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    List<ItemStack> slots = items.getSlots();
                    for (int i = 0; i < slots.size(); i++) {
                        playerInventory[i] = slots.get(i);
                    }
                });
            } else {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    if (items.getWindowId() == openWindowID) {
                        List<ItemStack> slots = items.getSlots();
                        for (int i = 0; i < slots.size(); i++) {
                            openedInventory.set(i, slots.get(i));
                        }
                    }
                });
            }
        }

        // Also 1:1 MCP
        if (event.getPacketId() == PacketType.Play.Server.SET_SLOT) {
            // Only edit hotbar (36 to 44) if window ID is 0
            // Set cursor by putting -1 as window ID and as slot
            // Window ID -2 means any slot can be used
            WrappedPacketOutSetSlot slot = new WrappedPacketOutSetSlot(event.getNMSPacket());

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                if (slot.getWindowId() == -1) { // Carried item
                    carriedItem = slot.getItemStack();
                } else if (slot.getWindowId() == -2) { // Any slot is allowed to change in inventory
                    playerInventory[slot.getSlot()] = slot.getItemStack();
                } else if (slot.getWindowId() == 0) { // Player hotbar
                    if (slot.getSlot() >= 36 && slot.getSlot() <= 44) { // Client ignored if not in range
                        playerInventory[slot.getSlot()] = slot.getItemStack();
                    }
                } else if (slot.getWindowId() == openWindowID) { // Opened inventory
                    openedInventory.set(slot.getSlot(), slot.getItemStack());
                }
            });
        }
    }

    // From protocol wiki:
    //0 	minecraft:generic_9x1 	A 1-row inventory, not used by the notchian server.
    // 9 offset
    //1 	minecraft:generic_9x2 	A 2-row inventory, not used by the notchian server.
    // 18 offset
    //2 	minecraft:generic_9x3 	General-purpose 3-row inventory. Used by Chest, minecart with chest, ender chest, and barrel
    // 27 offset
    //3 	minecraft:generic_9x4 	A 4-row inventory, not used by the notchian server.
    // 36 offset
    //4 	minecraft:generic_9x5 	A 5-row inventory, not used by the notchian server.
    // 45 offset
    //5 	minecraft:generic_9x6 	General-purpose 6-row inventory, used by large chests.
    // 54 offset
    //6 	minecraft:generic_3x3 	General-purpose 3-by-3 square inventory, used by Dispenser and Dropper
    // 9 offset
    //7 	minecraft:anvil 	Anvil
    // 3 offset
    //8 	minecraft:beacon 	Beacon
    // 1 offset
    //9 	minecraft:blast_furnace 	Blast Furnace
    // 3 offset
    //10 	minecraft:brewing_stand 	Brewing stand
    // 5 offset
    //11 	minecraft:crafting 	Crafting table
    // 10 offset
    //12 	minecraft:enchantment 	Enchantment table
    // 2 offset
    //13 	minecraft:furnace 	Furnace
    // 3 offset
    //14 	minecraft:grindstone 	Grindstone
    // 3 offset
    //15 	minecraft:hopper 	Hopper or minecart with hopper
    // 5 offset
    //16 	minecraft:lectern 	Lectern
    // No player inventory.
    //17 	minecraft:loom 	Loom
    // 4 offset
    //18 	minecraft:merchant 	Villager, Wandering Trader
    // 3 offset
    //19 	minecraft:shulker_box 	Shulker box
    // 27 offset
    //20 	minecraft:smithing 	Smithing Table
    // 3 offset
    //21 	minecraft:smoker 	Smoker
    // 3 offset
    //22 	minecraft:cartography 	Cartography Table
    // 3 offset
    //23 	minecraft:stonecutter 	Stonecutter
    // 2 offset
    private int getOffset(int containerType) {
        switch (containerType) {
            case 0:
                return 9;
            case 1:
                return 18;
            case 2:
                return 27;
            case 3:
                return 36;
            case 4:
                return 45;
            case 5:
                return 54;
            case 6:
                return 9;
            case 7:
                return 3;
            case 8:
                return 1;
            case 9:
                return 3;
            case 10:
                return 5;
            case 11:
                return 10;
            case 12:
                return 2;
            case 13:
                return 3;
            case 14:
                return 3;
            case 15:
                return 5;
            case 16:
                return 1;
            case 17:
                return 4;
            case 18:
                return 3;
            case 19:
                return 27;
            case 20:
                return 3;
            case 21:
                return 3;
            case 22:
                return 3;
            case 23:
                return 2;
        }
        return 0;
    }
}
