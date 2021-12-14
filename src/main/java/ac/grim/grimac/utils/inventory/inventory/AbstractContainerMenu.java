package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.ClickAction;
import ac.grim.grimac.utils.inventory.ClickType;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.WrappedStack;
import ac.grim.grimac.utils.inventory.slot.Slot;
import ac.grim.grimac.utils.math.GrimMath;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractContainerMenu {
    @Setter
    protected GrimPlayer player;
    // Quick crafting/dragging
    int quickcraftStatus = 0;
    int quickcraftType = -1;
    Set<Slot> quickcraftSlots = Sets.newHashSet();
    @Setter
    Inventory playerInventory;
    @Getter
    List<Slot> slots = new ArrayList<>();
    @Getter
    @NotNull
    WrappedStack carriedItem;

    public AbstractContainerMenu(GrimPlayer player, Inventory playerInventory) {
        this.player = player;
        this.playerInventory = playerInventory;
        this.carriedItem = WrappedStack.empty();
    }

    public AbstractContainerMenu() {
        this.carriedItem = WrappedStack.empty();
    }

    public Slot addSlot(Slot slot) {
        slot.slotListIndex = this.slots.size();
        this.slots.add(slot);
        return slot;
    }

    public void addFourRowPlayerInventory() {
        for (int slot = Inventory.ITEMS_START; slot <= Inventory.ITEMS_END; slot++) {
            addSlot(new Slot(playerInventory.getPlayerInventory(), slot));
        }
    }

    public static int getQuickcraftHeader(int p_38948_) {
        return p_38948_ & 3;
    }

    public static int getQuickcraftMask(int p_38931_, int p_38932_) {
        return p_38931_ & 3 | (p_38932_ & 3) << 2;
    }

    public static int getQuickcraftType(int p_38929_) {
        return p_38929_ >> 2 & 3;
    }

    public static boolean canItemQuickReplace(@Nullable Slot p_38900_, WrappedStack p_38901_, boolean p_38902_) {
        boolean flag = p_38900_ == null || !p_38900_.hasItem();
        if (!flag && WrappedStack.isSameItemSameTags(p_38901_, p_38900_.getItem())) {
            return p_38900_.getItem().getCount() + (p_38902_ ? 0 : p_38901_.getCount()) <= p_38901_.getMaxStackSize();
        } else {
            return flag;
        }
    }

    public static void getQuickCraftSlotCount(Set<Slot> p_38923_, int p_38924_, WrappedStack p_38925_, int p_38926_) {
        switch (p_38924_) {
            case 0:
                p_38925_.setCount(GrimMath.floor((float) p_38925_.getCount() / (float) p_38923_.size()));
                break;
            case 1:
                p_38925_.setCount(1);
                break;
            case 2:
                p_38925_.setCount(p_38925_.getStack().getType().getMaxStackSize());
        }

        p_38925_.grow(p_38926_);
    }

    protected void resetQuickCraft() {
        this.quickcraftStatus = 0;
        this.quickcraftSlots.clear();
    }

    public boolean isValidQuickcraftType(int p_38863_) {
        if (p_38863_ == 0) {
            return true;
        } else if (p_38863_ == 1) {
            return true;
        } else {
            return p_38863_ == 2 && player.gamemode == GameMode.CREATIVE;
        }
    }

    public WrappedStack getCarried() {
        return getCarriedItem();
    }

    public void setCarried(WrappedStack stack) {
        // Cannot be null
        carriedItem = stack == null ? WrappedStack.empty() : stack;
    }

    public WrappedStack getPlayerInventoryItem(int slot) {
        return playerInventory.getPlayerInventory().getItem(slot);
    }

    public void setPlayerInventoryItem(int slot, ItemStack stack) {
        playerInventory.getPlayerInventory().setItem(slot, new WrappedStack(stack));
    }

    public void doClick(int button, int slotID, ClickType clickType) {
        if (clickType == ClickType.QUICK_CRAFT) {
            int i = this.quickcraftStatus;
            this.quickcraftStatus = getQuickcraftHeader(button);
            if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
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
                Slot slot = slots.get(slotID);
                WrappedStack itemstack = this.getCarried();
                if (canItemQuickReplace(slot, itemstack, true) && slot.mayPlace(itemstack) && (this.quickcraftType == 2 || itemstack.getCount() > this.quickcraftSlots.size()) && this.canDragTo(slot)) {
                    this.quickcraftSlots.add(slot);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (this.quickcraftSlots.size() == 1) {
                        int l = (this.quickcraftSlots.iterator().next()).slotListIndex;
                        this.resetQuickCraft();
                        this.doClick(l, this.quickcraftType, ClickType.PICKUP);
                        return;
                    }

                    WrappedStack itemstack3 = this.getCarried().copy();
                    int j1 = this.getCarried().getCount();

                    for (Slot slot1 : this.quickcraftSlots) {
                        WrappedStack itemstack1 = this.getCarried();
                        if (slot1 != null && canItemQuickReplace(slot1, itemstack1, true) && slot1.mayPlace(itemstack1) && (this.quickcraftType == 2 || itemstack1.getCount() >= this.quickcraftSlots.size()) && this.canDragTo(slot1)) {
                            WrappedStack itemstack2 = itemstack3.copy();
                            int j = slot1.hasItem() ? slot1.getItem().getCount() : 0;
                            getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, itemstack2, j);
                            int k = Math.min(itemstack2.getMaxStackSize(), slot1.getMaxStackSize(itemstack2));
                            if (itemstack2.getCount() > k) {
                                itemstack2.setCount(k);
                            }

                            j1 -= itemstack2.getCount() - j;
                            slot1.set(itemstack2);
                        }
                    }

                    itemstack3.setCount(j1);
                    this.setCarried(itemstack3);
                }

                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (button == 0 || button == 1)) {
            ClickAction clickAction = ClickAction.values()[button];
            if (slotID == -999) { // Drop item
                if (!getCarried().isEmpty()) {
                    if (clickAction == ClickAction.PRIMARY) { // Drops the entire stack
                        setCarried(WrappedStack.empty());
                    } else { // Drops a single item
                        getCarried().split(1);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                if (slotID < 0) return;

                Slot stack = getSlot(slotID);
                if (!stack.mayPickup()) {
                    return;
                }

                for (WrappedStack itemstack9 = this.quickMoveStack(slotID); !itemstack9.isEmpty() && WrappedStack.isSame(stack.getItem(), itemstack9); itemstack9 = this.quickMoveStack(slotID)) {
                }
            } else {
                if (slotID < 0) return;

                Slot slot = getSlot(slotID);
                WrappedStack slotItem = slot.getItem();
                WrappedStack carriedItem = getCarried();

                if (!carriedItem.overrideStackedOnOther(slotItem, clickAction) && !slotItem.overrideOtherStackedOnMe(slotItem, clickAction, carriedItem)) {
                    if (slotItem.isEmpty()) {
                        if (!carriedItem.isEmpty()) {
                            int l2 = clickAction == ClickAction.PRIMARY ? carriedItem.getCount() : 1;
                            this.setCarried(slot.safeInsert(carriedItem, l2));
                        }
                    } else if (slot.mayPickup()) {
                        if (carriedItem.isEmpty()) {
                            int i3 = clickAction == ClickAction.PRIMARY ? slotItem.getCount() : (slotItem.getCount() + 1) / 2;
                            Optional<WrappedStack> optional1 = slot.tryRemove(i3, Integer.MAX_VALUE, player);
                            optional1.ifPresent((p_150421_) -> {
                                this.setCarried(p_150421_);
                                slot.onTake(player, p_150421_);
                            });
                        } else if (slotItem.mayPlace(carriedItem)) {
                            if (slotItem.isSameItemSameTags(carriedItem)) {
                                int j3 = clickAction == ClickAction.PRIMARY ? carriedItem.getCount() : 1;
                                this.setCarried(slot.safeInsert(carriedItem, j3));
                            } else if (carriedItem.getCount() <= slot.getMaxStackSize(carriedItem)) {
                                slot.set(carriedItem);
                                this.setCarried(slotItem);
                            }
                        } else if (slotItem.isSameItemSameTags(carriedItem)) {
                            Optional<WrappedStack> optional = slot.tryRemove(slotItem.getCount(), carriedItem.getMaxStackSize() - carriedItem.getCount(), player);
                            optional.ifPresent((p_150428_) -> {
                                carriedItem.grow(p_150428_.getCount());
                                slot.onTake(player, p_150428_);
                            });
                        }
                    }
                }
            }
        } else if (clickType == ClickType.SWAP) {
            Slot hoveringSlot = slots.get(slotID);

            WrappedStack hotbarKeyStack = getPlayerInventoryItem(button + Inventory.HOTBAR_OFFSET);
            WrappedStack hoveringItem2 = hoveringSlot.getItem();

            if (!hotbarKeyStack.isEmpty() || !hoveringItem2.isEmpty()) {
                if (hotbarKeyStack.isEmpty()) {
                    if (hoveringSlot.mayPickup(player)) {
                        setPlayerInventoryItem(button + Inventory.HOTBAR_OFFSET, hoveringItem2.getStack());
                        hoveringSlot.set(WrappedStack.empty());
                        hoveringSlot.onTake(player, hoveringItem2);
                    }
                } else if (hoveringItem2.isEmpty()) {
                    if (hoveringSlot.mayPlace(hotbarKeyStack)) {
                        int l1 = hoveringSlot.getMaxStackSize(hotbarKeyStack);
                        if (hotbarKeyStack.getCount() > l1) {
                            hoveringSlot.set(hotbarKeyStack.split(l1));
                        } else {
                            hoveringSlot.set(hotbarKeyStack);
                            setPlayerInventoryItem(button + Inventory.HOTBAR_OFFSET, WrappedStack.empty().getStack());
                        }
                    }
                } else if (hoveringSlot.mayPickup(player) && hoveringSlot.mayPlace(hotbarKeyStack)) {
                    int i2 = hoveringSlot.getMaxStackSize(hotbarKeyStack);
                    if (hotbarKeyStack.getCount() > i2) {
                        hoveringSlot.set(hotbarKeyStack.split(i2));
                        hoveringSlot.onTake(player, hoveringItem2);
                        playerInventory.add(hoveringItem2);
                    } else {
                        hoveringSlot.set(hotbarKeyStack);
                        setPlayerInventoryItem(button + Inventory.HOTBAR_OFFSET, hoveringItem2.getStack());
                        hoveringSlot.onTake(player, hoveringItem2);
                    }
                }
            }
        } else if (clickType == ClickType.CLONE && player.gamemode == GameMode.CREATIVE && slotID >= 0 && carriedItem.isEmpty()) {
            Slot slot5 = getSlot(slotID);
            if (slot5.hasItem()) {
                WrappedStack itemstack6 = slot5.getItem().copy();
                itemstack6.setCount(itemstack6.getMaxStackSize());
                this.setCarried(itemstack6);
            }
        } else if (clickType == ClickType.THROW && getCarried().isEmpty() && slotID >= 0) {
            Slot slot4 = getSlot(slotID);
            int i1 = button == 0 ? 1 : slot4.getItem().getCount();
            WrappedStack itemstack8 = slot4.safeTake(i1, Integer.MAX_VALUE, player);
        } else if (clickType == ClickType.PICKUP_ALL && slotID >= 0) {
            Slot slot3 = getSlot(slotID);

            if (!getCarried().isEmpty() && (!slot3.hasItem() || !slot3.mayPickup(player))) {
                int k1 = button == 0 ? 0 : this.slots.size() - 1;
                int j2 = button == 0 ? 1 : -1;

                for (int k2 = 0; k2 < 2; ++k2) {
                    for (int k3 = k1; k3 >= 0 && k3 < this.slots.size() && getCarried().getCount() < getCarried().getMaxStackSize(); k3 += j2) {
                        Slot slot8 = this.slots.get(k3);
                        if (slot8.hasItem() && canItemQuickReplace(slot8, getCarried(), true) && slot8.mayPickup(player) && this.canTakeItemForPickAll(getCarried(), slot8)) {
                            WrappedStack itemstack12 = slot8.getItem();
                            if (k2 != 0 || itemstack12.getCount() != itemstack12.getMaxStackSize()) {
                                WrappedStack itemstack13 = slot8.safeTake(itemstack12.getCount(), getCarried().getMaxStackSize() - getCarried().getCount(), player);
                                getCarried().grow(itemstack13.getCount());
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean moveItemStackTo(WrappedStack toMove, int min, int max, boolean reverse) {
        boolean flag = false;
        int i = min;
        if (reverse) {
            i = max - 1;
        }

        if (toMove.getItem().getMaxStackSize() > 1) {
            while (!toMove.isEmpty()) {
                if (reverse) {
                    if (i < min) {
                        break;
                    }
                } else if (i >= max) {
                    break;
                }

                Slot slot = this.slots.get(i);
                WrappedStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && WrappedStack.isSameItemSameTags(toMove, itemstack)) {
                    int j = itemstack.getCount() + toMove.getCount();
                    if (j <= toMove.getMaxStackSize()) {
                        toMove.setCount(0);
                        itemstack.setCount(j);
                        flag = true;
                    } else if (itemstack.getCount() < toMove.getMaxStackSize()) {
                        toMove.shrink(toMove.getMaxStackSize() - itemstack.getCount());
                        itemstack.setCount(toMove.getMaxStackSize());
                        flag = true;
                    }
                }

                if (reverse) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!toMove.isEmpty()) {
            if (reverse) {
                i = max - 1;
            } else {
                i = min;
            }

            while (true) {
                if (reverse) {
                    if (i < min) {
                        break;
                    }
                } else if (i >= max) {
                    break;
                }

                Slot slot1 = this.slots.get(i);
                WrappedStack itemstack1 = slot1.getItem();
                if (itemstack1.isEmpty() && slot1.mayPlace(toMove)) {
                    if (toMove.getCount() > slot1.getMaxStackSize()) {
                        slot1.set(toMove.split(slot1.getMaxStackSize()));
                    } else {
                        slot1.set(toMove.split(toMove.getCount()));
                    }

                    flag = true;
                    break;
                }

                if (reverse) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return flag;
    }

    public boolean canTakeItemForPickAll(WrappedStack p_38908_, Slot p_38909_) {
        return true;
    }

    public WrappedStack quickMoveStack(int p_38942_) {
        return this.slots.get(p_38942_).getItem();
    }

    public Slot getSlot(int slotID) {
        return this.slots.get(slotID);
    }

    public boolean canDragTo(Slot slot) {
        return true;
    }

    public int getMaxStackSize() {
        return 64;
    }
}