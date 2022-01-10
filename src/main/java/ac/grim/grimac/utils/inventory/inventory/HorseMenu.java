package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public class HorseMenu extends AbstractContainerMenu {
    InventoryStorage storage;

    public HorseMenu(GrimPlayer player, Inventory playerInventory, int size, int entityID) {
        super(player, playerInventory);

        PacketEntity hopefullyAHorse = player.compensatedEntities.getEntity(entityID);
        if (!(hopefullyAHorse instanceof PacketEntityHorse)) {
            return;
        }

        PacketEntityHorse horse = (PacketEntityHorse) hopefullyAHorse;

        storage = new InventoryStorage(size);
        addSlot(new Slot(storage, 0));

        this.addSlot(new Slot(storage, 0) {
            public boolean mayPlace(ItemStack p_39677_) {
                return p_39677_.is(ItemTypes.SADDLE) && !this.hasItem() && horse.type != EntityTypes.LLAMA &&
                        !horse.isDead && !horse.isBaby && horse.isTame;
            }
        });
        this.addSlot(new Slot(storage, 1) {
            public boolean mayPlace(ItemStack stack) {
                return stack.getType() == ItemTypes.DIAMOND_HORSE_ARMOR || stack.getType() == ItemTypes.GOLDEN_HORSE_ARMOR ||
                        stack.getType() == ItemTypes.IRON_HORSE_ARMOR || stack.getType() == ItemTypes.LEATHER_HORSE_ARMOR;
            }

            public int getMaxStackSize() {
                return 1;
            }
        });

        if (horse.hasChest) {
            int columns = horse.type == EntityTypes.LLAMA ? horse.llamaStrength : 5;

            for (int k = 0; k < 3; ++k) {
                for (int l = 0; l < columns; ++l) {
                    this.addSlot(new Slot(storage, 2 + l + k * columns));
                }
            }
        }

        addFourRowPlayerInventory();
    }

    @Override
    public ItemStack quickMoveStack(int p_39666_) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(p_39666_);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int i = this.storage.getSize();
            if (p_39666_ < i) {
                if (!this.moveItemStackTo(itemstack1, i, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(itemstack1)) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i <= 2 || !this.moveItemStackTo(itemstack1, 2, i, false)) {
                int j = i + 27;
                int k = j + 9;
                if (p_39666_ >= j && p_39666_ < k) {
                    if (!this.moveItemStackTo(itemstack1, i, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (p_39666_ >= i && p_39666_ < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
        }

        return itemstack;
    }

}
