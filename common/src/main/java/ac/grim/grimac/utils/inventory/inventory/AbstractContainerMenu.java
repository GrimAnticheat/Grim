package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.inventory.ClickAction;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.slot.ResultSlot;
import ac.grim.grimac.utils.inventory.slot.Slot;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractContainerMenu {
    protected final GrimPlayer player;
    // Quick crafting/dragging
    private int quickcraftStatus = 0;
    private int quickcraftType = -1;
    private final Set<Slot> quickcraftSlots = Sets.newHashSet();
    @Setter(AccessLevel.PROTECTED)
    private Inventory playerInventory;
    @Getter
    protected final List<Slot> slots = new ArrayList<>();
    @Getter
    private @NotNull ItemStack carried = ItemStack.EMPTY;

    public AbstractContainerMenu(GrimPlayer player, Inventory playerInventory) {
        this.player = player;
        this.playerInventory = playerInventory;
    }

    public AbstractContainerMenu(GrimPlayer player) {
        this.player = player;
    }

    public static int getQuickcraftType(int mask) {
        return mask >> 2 & 3;
    }

    public static int getQuickcraftHeader(int mask) {
        return mask & 3;
    }

    public static int getQuickcraftMask(int header, int type) {
        return header & 3 | (type & 3) << 2;
    }

    public static boolean canItemQuickReplace(@Nullable Slot slot, ItemStack itemStack, boolean ignoreSize) {
        boolean flag = slot == null || !slot.hasItem();
        if (!flag && ItemStack.isSameItemSameTags(itemStack, slot.getItem())) {
            return slot.getItem().getAmount() + (ignoreSize ? 0 : itemStack.getAmount()) <= itemStack.getMaxStackSize();
        } else {
            return flag;
        }
    }

    public static int getQuickCraftSlotCount(Set<Slot> slots, int type, ItemStack itemStack) {
        return switch (type) {
            case 0 -> GrimMath.floor((float) itemStack.getAmount() / (float) slots.size());
            case 1 -> 1;
            case 2 -> itemStack.getType().getMaxAmount();
            default -> itemStack.getAmount();
        };
    }

    public Slot addSlot(Slot slot) {
        slot.slotListIndex = this.slots.size();
        this.slots.add(slot);
        return slot;
    }

    public void addFourRowPlayerInventory() {
        for (int slot = Inventory.ITEMS_START; slot < Inventory.ITEMS_END; slot++) {
            addSlot(new Slot(playerInventory.getInventoryStorage(), slot));
        }
    }

    protected void resetQuickCraft() {
        this.quickcraftStatus = 0;
        this.quickcraftSlots.clear();
    }

    public boolean isValidQuickcraftType(int quickcraftType) {
        return quickcraftType == 0 || quickcraftType == 1 || quickcraftType == 2 && player.gamemode == GameMode.CREATIVE;
    }

    public void setCarried(ItemStack stack) {
        // Cannot be null
        carried = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack getPlayerInventoryItem(int slot) {
        return playerInventory.getInventoryStorage().getItem(slot);
    }

    public void setPlayerInventoryItem(int slot, ItemStack stack) {
        playerInventory.getInventoryStorage().setItem(slot, stack);
    }

    public void doClick(int button, int slotID, WrapperPlayClientClickWindow.WindowClickType clickType) {
        if (clickType == WrapperPlayClientClickWindow.WindowClickType.QUICK_CRAFT) {
            int status = this.quickcraftStatus;
            this.quickcraftStatus = getQuickcraftHeader(button);
            if ((status != 1 || this.quickcraftStatus != 2) && status != this.quickcraftStatus || this.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.quickcraftStatus == 0) {
                this.quickcraftType = getQuickcraftType(button);
                if (isValidQuickcraftType(this.quickcraftType)) {
                    this.quickcraftStatus = 1;
                    this.quickcraftSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.quickcraftStatus == 1) {
                if (slotID < 0) return;
                Slot slot = slots.get(slotID);
                ItemStack carried = this.getCarried();
                if (canItemQuickReplace(slot, carried, true) && slot.mayPlace(carried) && (this.quickcraftType == 2 || carried.getAmount() > this.quickcraftSlots.size()) && this.canDragTo(slot)) {
                    this.quickcraftSlots.add(slot);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (this.quickcraftSlots.size() == 1) {
                        int slot = (this.quickcraftSlots.iterator().next()).slotListIndex;
                        this.resetQuickCraft();
                        this.doClick(this.quickcraftType, slot, WrapperPlayClientClickWindow.WindowClickType.PICKUP);
                        return;
                    }

                    ItemStack copy = this.getCarried().copy();
                    int amount = this.getCarried().getAmount();

                    for (Slot slot : this.quickcraftSlots) {
                        ItemStack carried = this.getCarried();
                        if (slot != null && canItemQuickReplace(slot, carried, true) && slot.mayPlace(carried) && (this.quickcraftType == 2 || carried.getAmount() >= this.quickcraftSlots.size()) && this.canDragTo(slot)) {
                            ItemStack newStack = copy.copy();
                            int amountInSlot = slot.hasItem() ? slot.getItem().getAmount() : 0;
                            newStack.setAmount(getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, newStack) + amountInSlot);
                            int k = Math.min(newStack.getMaxStackSize(), slot.getMaxStackSize(newStack));
                            if (newStack.getAmount() > k) {
                                newStack.setAmount(k);
                            }

                            amount -= newStack.getAmount() - amountInSlot;
                            slot.set(newStack);
                        }
                    }

                    copy.setAmount(amount);
                    this.setCarried(copy);
                }

                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((clickType == WrapperPlayClientClickWindow.WindowClickType.PICKUP || clickType == WrapperPlayClientClickWindow.WindowClickType.QUICK_MOVE) && (button == 0 || button == 1)) {
            ClickAction clickAction = ClickAction.values()[button];
            if (slotID == -999) { // Drop item
                if (!getCarried().isEmpty()) {
                    if (clickAction == ClickAction.PRIMARY) { // Drops the entire stack
                        setCarried(ItemStack.EMPTY);
                    } else { // Drops a single item
                        getCarried().split(1);
                    }
                }
            } else if (clickType == WrapperPlayClientClickWindow.WindowClickType.QUICK_MOVE) {
                if (slotID < 0) return;

                Slot stack = getSlot(slotID);
                if (!stack.mayPickup(player)) {
                    return;
                }

                ItemStack quickMoveStack = this.quickMoveStack(slotID);
                while (!quickMoveStack.isEmpty() && ItemStack.isSameItemSameTags(stack.getItem(), quickMoveStack)) {
                    quickMoveStack = this.quickMoveStack(slotID);
                }
            } else {
                if (slotID < 0) return;

                Slot slot = getSlot(slotID);
                ItemStack slotItem = slot.getItem();
                ItemStack carriedItem = getCarried();

                // TODO: What do we do with crafting? I think this is overkill and we shouldn't attempt to track crafting, and just resync inventory.
                // 1.17+ clients send changed itemstacks anyways, so just hack around stuff until people stop using decade old versions.
                if (slot instanceof ResultSlot) {
                    player.inventory.isPacketInventoryActive = false;
                }

                // TODO: Bundle support
                //if (!carriedItem.overrideStackedOnOther(slotItem, clickAction) && !slotItem.overrideOtherStackedOnMe(slotItem, clickAction, carriedItem)) {
                if (slotItem.isEmpty()) {
                    if (!carriedItem.isEmpty()) {
                        int amount = clickAction == ClickAction.PRIMARY ? carriedItem.getAmount() : 1;
                        this.setCarried(slot.safeInsert(carriedItem, amount));
                    }
                } else if (slot.mayPickup(player)) {
                    if (carriedItem.isEmpty()) {
                        int amount = clickAction == ClickAction.PRIMARY ? slotItem.getAmount() : (slotItem.getAmount() + 1) / 2;
                        Optional<ItemStack> newCarried = slot.tryRemove(amount, Integer.MAX_VALUE, player);
                        newCarried.ifPresent(itemsTaken -> {
                            this.setCarried(itemsTaken);
                            slot.onTake(player, itemsTaken);
                        });
                    } else if (slot.mayPlace(carriedItem)) {
                        if (ItemStack.isSameItemSameTags(slotItem, carriedItem)) {
                            int amount = clickAction == ClickAction.PRIMARY ? carriedItem.getAmount() : 1;
                            this.setCarried(slot.safeInsert(carriedItem, amount));
                        } else if (carriedItem.getAmount() <= slot.getMaxStackSize(carriedItem)) {
                            slot.set(carriedItem);
                            this.setCarried(slotItem);
                        }
                    } else if (ItemStack.isSameItemSameTags(slotItem, carriedItem)) {
                        Optional<ItemStack> newCarried = slot.tryRemove(slotItem.getAmount(), carriedItem.getMaxStackSize() - carriedItem.getAmount(), player);
                        newCarried.ifPresent(itemsTaken -> {
                            carriedItem.grow(itemsTaken.getAmount());
                            slot.onTake(player, itemsTaken);
                        });
                    }
                }
                //}
            }
        } else if (clickType == WrapperPlayClientClickWindow.WindowClickType.SWAP) {
            Slot hoveringSlot = slots.get(slotID);

            // How the fuck did the player SWAP with true slot 38 (chestplate?)??
            // A vanilla client can't do this... what cheat does this?
            // TODO: What cheat does this?
            if (button != 40 && (button < 0 || button >= 9)) return;

            button = button == 40 ? Inventory.SLOT_OFFHAND : button + Inventory.HOTBAR_OFFSET;

            // 40 is offhand
            ItemStack hotbarKeyStack = getPlayerInventoryItem(button);
            ItemStack hoveringItem2 = hoveringSlot.getItem();

            if (!hotbarKeyStack.isEmpty() || !hoveringItem2.isEmpty()) {
                if (hotbarKeyStack.isEmpty()) {
                    if (hoveringSlot.mayPickup(player)) {
                        setPlayerInventoryItem(button, hoveringItem2);
                        hoveringSlot.set(ItemStack.EMPTY);
                        hoveringSlot.onTake(player, hoveringItem2);
                    }
                } else if (hoveringItem2.isEmpty()) {
                    if (hoveringSlot.mayPlace(hotbarKeyStack)) {
                        int l1 = hoveringSlot.getMaxStackSize(hotbarKeyStack);
                        if (hotbarKeyStack.getAmount() > l1) {
                            hoveringSlot.set(hotbarKeyStack.split(l1));
                        } else {
                            hoveringSlot.set(hotbarKeyStack);
                            setPlayerInventoryItem(button, ItemStack.EMPTY);
                        }
                    }
                } else if (hoveringSlot.mayPickup(player) && hoveringSlot.mayPlace(hotbarKeyStack)) {
                    int maxStackSize = hoveringSlot.getMaxStackSize(hotbarKeyStack);
                    if (hotbarKeyStack.getAmount() > maxStackSize) {
                        hoveringSlot.set(hotbarKeyStack.split(maxStackSize));
                        hoveringSlot.onTake(player, hoveringItem2);
                        playerInventory.add(hoveringItem2);
                    } else {
                        hoveringSlot.set(hotbarKeyStack);
                        setPlayerInventoryItem(button, hoveringItem2);
                        hoveringSlot.onTake(player, hoveringItem2);
                    }
                }
            }
        } else if (clickType == WrapperPlayClientClickWindow.WindowClickType.CLONE && player.gamemode == GameMode.CREATIVE && slotID >= 0 && getCarried().isEmpty()) {
            Slot slot = getSlot(slotID);
            if (slot.hasItem()) {
                ItemStack stack = slot.getItem().copy();
                stack.setAmount(stack.getMaxStackSize());
                this.setCarried(stack);
            }
        } else if (clickType == WrapperPlayClientClickWindow.WindowClickType.THROW && getCarried().isEmpty() && slotID >= 0) {
            Slot slot = getSlot(slotID);
            int amount = button == 0 ? 1 : slot.getItem().getAmount();
            slot.safeTake(amount, Integer.MAX_VALUE, player);
        } else if (clickType == WrapperPlayClientClickWindow.WindowClickType.PICKUP_ALL && slotID >= 0) {
            if (getCarried().isEmpty()) {
                return;
            } else {
                Slot slot = getSlot(slotID);
                if (slot.hasItem() && slot.mayPickup(player)) return;
            }

            int start = button == 0 ? 0 : this.slots.size() - 1;
            int step = button == 0 ? 1 : -1;

            for (int pass = 0; pass < 2; pass++) {
                for (int i = start; i >= 0 && i < this.slots.size() && getCarried().getAmount() < getCarried().getMaxStackSize(); i += step) {
                    Slot slot = this.slots.get(i);
                    if (slot.hasItem() && canItemQuickReplace(slot, getCarried(), true) && slot.mayPickup(player) && this.canTakeItemForPickAll(getCarried(), slot)) {
                        ItemStack itemStack = slot.getItem();
                        if (pass != 0 || itemStack.getAmount() != itemStack.getMaxStackSize()) {
                            ItemStack removed = slot.safeTake(itemStack.getAmount(), getCarried().getMaxStackSize() - getCarried().getAmount(), player);
                            getCarried().grow(removed.getAmount());
                        }
                    }
                }
            }
        }
    }

    protected boolean moveItemStackTo(ItemStack toMove, int start, int end, boolean reverse) {
        boolean changed = false;
        int slotID = start;
        if (reverse) {
            slotID = end - 1;
        }

        if (toMove.getType().getMaxAmount() > 1) {
            while (!toMove.isEmpty()) {
                if (reverse && slotID < start) break;
                else if (!reverse && slotID >= end) break;

                Slot slot = this.slots.get(slotID);
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty() && ItemStack.isSameItemSameTags(toMove, stack)) {
                    int amount = stack.getAmount() + toMove.getAmount();
                    if (amount <= toMove.getMaxStackSize()) {
                        toMove.setAmount(0);
                        stack.setAmount(amount);
                        changed = true;
                    } else if (stack.getAmount() < toMove.getMaxStackSize()) {
                        toMove.shrink(toMove.getMaxStackSize() - stack.getAmount());
                        stack.setAmount(toMove.getMaxStackSize());
                        changed = true;
                    }
                }

                if (reverse) slotID--; else slotID++;
            }
        }

        if (!toMove.isEmpty()) {
            slotID = reverse ? end - 1 : start;

            while (true) {
                if (reverse && slotID < start) break;
                else if (!reverse && slotID >= end) break;

                Slot slot = this.slots.get(slotID);
                ItemStack stack = slot.getItem();
                if (stack.isEmpty() && slot.mayPlace(toMove)) {
                    if (toMove.getAmount() > slot.getMaxStackSize()) {
                        slot.set(toMove.split(slot.getMaxStackSize()));
                    } else {
                        slot.set(toMove.split(toMove.getAmount()));
                    }

                    changed = true;
                    break;
                }

                if (reverse) slotID--; else slotID++;
            }
        }

        return changed;
    }

    public boolean canTakeItemForPickAll(ItemStack p_38908_, Slot p_38909_) {
        return true;
    }

    public ItemStack quickMoveStack(int slotID) {
        return this.slots.get(slotID).getItem();
    }

    public Slot getSlot(int slotID) {
        try {
            return this.slots.get(slotID);
        } catch (IndexOutOfBoundsException e) {
            LogUtil.error("Tried to get slot " + slotID + " in a container with only " + this.slots.size() + " slots, container type: " + this.getClass().getName(), e);
            throw e;
        }
    }

    public boolean canDragTo(Slot slot) {
        return true;
    }

    public int getMaxStackSize() {
        return 64;
    }
}
