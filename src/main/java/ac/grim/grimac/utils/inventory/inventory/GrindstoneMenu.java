package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.EnchantmentHelper;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;

import java.util.List;
import java.util.stream.Collectors;

public class GrindstoneMenu extends AbstractContainerMenu {

    public GrindstoneMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage storage = new InventoryStorage(3);

        addSlot(new Slot(storage, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Is damageable, is enchanted book, or is enchanted
                return (stack.isDamageableItem() || stack.getType() == ItemTypes.ENCHANTED_BOOK || !stack.isEnchanted(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()));
            }
        });
        addSlot(new Slot(storage, 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Is damageable, is enchanted book, or is enchanted
                return (stack.isDamageableItem() || stack.getType() == ItemTypes.ENCHANTED_BOOK || !stack.isEnchanted(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()));
            }
        });
        addSlot(new Slot(storage, 2) {
            @Override
            public boolean mayPlace(ItemStack p_39630_) {
                return false;
            }

            @Override
            public void onTake(GrimPlayer p_150574_, ItemStack p_150575_) {
                storage.setItem(0, ItemStack.EMPTY);
                storage.setItem(1, ItemStack.EMPTY);
            }
        });

        addFourRowPlayerInventory();
    }

    private static int calculateIncreasedRepairCost(int p_39026_) {
        return p_39026_ * 2 + 1;
    }

    private void createResult() {
        ItemStack itemstack = getSlot(0).getItem();
        ItemStack itemstack1 = getSlot(1).getItem();
        boolean flag = !itemstack.isEmpty() || !itemstack1.isEmpty();
        boolean flag1 = !itemstack.isEmpty() && !itemstack1.isEmpty();
        if (!flag) {
            getSlot(0).set(ItemStack.EMPTY);
        } else {
            boolean flag2 = !itemstack.isEmpty() && !itemstack.is(ItemTypes.ENCHANTED_BOOK) && !itemstack.isEnchanted(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) || !itemstack1.isEmpty() && !itemstack1.is(ItemTypes.ENCHANTED_BOOK) && !itemstack1.isEnchanted(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
            if (itemstack.getAmount() > 1 || itemstack1.getAmount() > 1 || !flag1 && flag2) {
                getSlot(2).set(ItemStack.EMPTY);
                return;
            }

            int j = 1;
            int i;
            ItemStack itemstack2;
            if (flag1) {
                if (!itemstack.is(itemstack1.getType())) {
                    getSlot(2).set(ItemStack.EMPTY);
                    return;
                }

                ItemType item = itemstack.getType();
                int k = item.getMaxDurability() - itemstack.getDamageValue();
                int l = item.getMaxDurability() - itemstack1.getDamageValue();
                int i1 = k + l + item.getMaxDurability() * 5 / 100;
                i = Math.max(item.getMaxDurability() - i1, 0);
                itemstack2 = this.mergeEnchants(itemstack, itemstack1);
                if (!itemstack2.isDamageableItem()) {
                    if (!ItemStack.isSameItemSameTags(itemstack, itemstack1)) {
                        getSlot(2).set(ItemStack.EMPTY);
                        return;
                    }

                    j = 2;
                }
            } else {
                boolean flag3 = !itemstack.isEmpty();
                i = flag3 ? itemstack.getDamageValue() : itemstack1.getDamageValue();
                itemstack2 = flag3 ? itemstack : itemstack1;
            }

            getSlot(2).set(this.removeNonCurses(itemstack2, i, j));
        }
    }

    private ItemStack mergeEnchants(ItemStack first, ItemStack second) {
        ItemStack copyFirst = first.copy();
        List<Enchantment> enchants = second.getEnchantments(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());

        for (Enchantment entry : enchants) {
            if (!EnchantmentHelper.isCurse(entry.getType()) || copyFirst.getEnchantmentLevel(entry.getType(), PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) == 0) {
                Enchantment enchant = Enchantment.builder().type(entry.getType()).level(entry.getLevel()).build();
                List<Enchantment> enchantmentList = copyFirst.getEnchantments(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
                enchantmentList.add(enchant);
                copyFirst.setEnchantments(enchantmentList, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
            }
        }

        return copyFirst;
    }

    private ItemStack removeNonCurses(ItemStack itemOne, int p_39581_, int p_39582_) {
        ItemStack itemstack = itemOne.copy();
        itemstack.getNBT().removeTag("Enchantments");
        itemstack.getNBT().removeTag("StoredEnchantments");
        if (p_39581_ > 0) {
            itemstack.setDamageValue(p_39581_);
        } else {
            itemstack.getNBT().removeTag("Damage");
        }

        itemstack.setAmount(p_39582_);

        List<Enchantment> filteredCurses = itemOne.getEnchantments(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()).stream().filter(enchantment -> !EnchantmentHelper.isCurse(enchantment.getType())).collect(Collectors.toList());

        itemstack.setEnchantments(filteredCurses, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());

        if (itemstack.is(ItemTypes.ENCHANTED_BOOK) && filteredCurses.size() == 0) {
            itemstack = new ItemStack.Builder().type(ItemTypes.BOOK).amount(1).build();

            // Set display name
            if (itemOne.getNBT().getCompoundTagOrNull("display") != null
                    && itemOne.getNBT().getCompoundTagOrNull("display").getTagOrNull("Name") != null) {

                NBTCompound compoundTag = itemstack.getOrCreateTag().getCompoundTagOrNull("display");
                if (compoundTag == null) {
                    itemstack.getNBT().setTag("display", new NBTCompound());
                    compoundTag = itemstack.getNBT().getCompoundTagOrNull("display");
                }

                compoundTag.setTag("Name", itemOne.getNBT().getCompoundTagOrNull("display").getTagOrNull("Name"));
            }
        }

        itemstack.getNBT().setTag("RepairCost", new NBTInt(0));

        for (int i = 0; i < filteredCurses.size(); ++i) {
            itemstack.getNBT().setTag("RepairCost", new NBTInt(calculateIncreasedRepairCost(itemstack.getNBT().getNumberTagOrNull("RepairCost").getAsInt())));
        }

        return itemstack;
    }

    @Override
    public ItemStack quickMoveStack(int p_39589_) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(p_39589_);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            ItemStack itemstack2 = getSlot(0).getItem();
            ItemStack itemstack3 = getSlot(1).getItem();
            if (p_39589_ == 2) {
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                //slot.onQuickCraft(itemstack1, itemstack);
            } else if (p_39589_ != 0 && p_39589_ != 1) {
                if (!itemstack2.isEmpty() && !itemstack3.isEmpty()) {
                    if (p_39589_ >= 3 && p_39589_ < 30) {
                        if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (p_39589_ >= 30 && p_39589_ < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }

            if (itemstack1.getAmount() == itemstack.getAmount()) {
                return ItemStack.EMPTY;
            }

            //slot.onTake(p_39588_, itemstack1);
        }

        return itemstack;
    }
}
