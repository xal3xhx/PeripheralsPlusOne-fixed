package com.austinv11.peripheralsplusplus.recipe;

import com.austinv11.peripheralsplusplus.init.ModItems;
import com.austinv11.peripheralsplusplus.utils.rfid.RfidTag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RecipeRfidChip implements IRecipe {
    private ResourceLocation group;
    private ResourceLocation name;

    public RecipeRfidChip(ResourceLocation group) {
        super();
        this.group = group;
    }

    public RecipeRfidChip() {
        super();
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return matchesAdd(inv) || matchesRemove(inv);

    }

    /**
     * Checks if the crafting contents matches a recipe to remove a RFID chip from an item
     * @param inv crafting inventory
     * @return recipe matches
     */
    private boolean matchesRemove(InventoryCrafting inv) {
        ItemStack rfidItem = getRfidItem(inv);
        if (rfidItem.isEmpty())
            return false;
        ItemStack fork = getFork(inv);
        return !fork.isEmpty() && getInventorySize(inv) == 2;
    }

    /**
     * Gets the amount of items in the inventory.
     * @param inv inventory to check
     * @return amount of items in inventory
     */
    private int getInventorySize(InventoryCrafting inv) {
        int size = 0;
        for (int itemIndex = 0; itemIndex < inv.getSizeInventory(); itemIndex++)
            if (!inv.getStackInSlot(itemIndex).isEmpty())
                size++;
        return size;
    }

    /**
     * Checks if the crafting contents matches a recipe to add a RFID chip to an item
     * @param inv crafting inventory
     * @return recipe matches
     */
    private boolean matchesAdd(InventoryCrafting inv) {
        ItemStack blankItem = getBlankItem(inv);
        if (blankItem.isEmpty())
            return false;
        ItemStack rfidChip = getRfidChip(inv);
        return !rfidChip.isEmpty() && getInventorySize(inv) == 2;
    }

    /**
     * @see RecipeRfidChip#getItemFromInventory(InventoryCrafting, ItemStack)
     */
    @Nonnull
    private ItemStack getRfidChip(InventoryCrafting inv) {
        return getItemFromInventory(inv, new ItemStack(ModItems.RFID_CHIP));
    }

    /**
     * @see RecipeRfidChip#getItemFromInventory(InventoryCrafting, ItemStack)
     */
    @Nonnull
    private ItemStack getFork(InventoryCrafting inv) {
        return getItemFromInventory(inv, new ItemStack(ModItems.FORK));
    }

    /**
     * Searches an inventory for an item. An empty item is returned if the item could not be found or there were
     * duplicates.
     * @param inv inventory to search
     * @param match item to match
     * @return matching item or empty item
     */
    @Nonnull
    private ItemStack getItemFromInventory(InventoryCrafting inv, ItemStack match) {
        ItemStack found = ItemStack.EMPTY;
        for (int itemIndex = 0; itemIndex < inv.getSizeInventory(); itemIndex++) {
            ItemStack itemStack = inv.getStackInSlot(itemIndex).copy();
            if (itemStack.isEmpty())
                continue;
            if (itemStack.isItemEqual(match)) {
                if (!found.isEmpty())
                    return ItemStack.EMPTY;
                found = itemStack;
            }
        }
        return found;
    }

    /**
     * Searches an inventory for an item that does not have an RFID tag. Forks and RFID chips are ignored.
     * An empty item is returned if there is more than one matching item.
     * @see RecipeRfidChip#getRfidItem(InventoryCrafting, boolean)
     * @param inv inventory to search
     * @return matching item or empty item
     */
    @Nonnull
    private ItemStack getBlankItem(InventoryCrafting inv) {
        return getRfidItem(inv, true);
    }

    /**
     * Searches an inventory for an item that has an RFID tag. Forks and RFID chips are ignored.
     * An empty item is returned if there is more than one matching item.
     * @param inv inventory to search
     * @param blankItem search for an item without an RFID tag instead
     * @return matching item or empty item
     */
    @Nonnull
    private ItemStack getRfidItem(InventoryCrafting inv, boolean blankItem) {
        ItemStack rfidItem = ItemStack.EMPTY;
        for (int itemIndex = 0; itemIndex < inv.getSizeInventory(); itemIndex++) {
            ItemStack itemStack = inv.getStackInSlot(itemIndex).copy();
            if (itemStack.isEmpty() || itemStack.isItemEqual(new ItemStack(ModItems.RFID_CHIP)) ||
                    itemStack.isItemEqual(new ItemStack(ModItems.FORK)))
                continue;
            if ((!blankItem && RfidTag.hasTag(itemStack)) || (blankItem && !RfidTag.hasTag(itemStack))) {
                if (!rfidItem.isEmpty())
                    return ItemStack.EMPTY;
                rfidItem = itemStack;
            }
        }
        return rfidItem;
    }

    /**
     * @see RecipeRfidChip#getRfidItem(InventoryCrafting, boolean)
     */
    @Nonnull
    private ItemStack getRfidItem(InventoryCrafting inv) {
        return getRfidItem(inv, false);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        // Add
        if (matchesAdd(inv)) {
            ItemStack rfidChip = getRfidChip(inv);
            ItemStack blankItem = getBlankItem(inv);
            RfidTag rfidTag = new RfidTag(rfidChip);
            RfidTag.addTag(blankItem, rfidTag);
            if (blankItem.isItemEqual(new ItemStack(ModItems.PLASTIC_CARD)))
                blankItem.setTranslatableName(blankItem.getUnlocalizedName() +
                        (Math.random() < .5 ? ".name_rfid" : ".name_nfc"));
            return blankItem;
        }
        // Remove
        else {
            ItemStack rfidItem = getRfidItem(inv).copy();
            return RfidTag.createChip(new RfidTag(rfidItem));
        }
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        // Add
        if (matchesAdd(inv))
            return IRecipe.super.getRemainingItems(inv);
        // Remove
        NonNullList<ItemStack> list = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int itemIndex = 0; itemIndex < inv.getSizeInventory(); itemIndex++) {
            ItemStack item = inv.getStackInSlot(itemIndex).copy();
            if (RfidTag.hasTag(item))
                RfidTag.removeTag(item);
            list.set(itemIndex, item);
        }
        return list;
    }

    @Override
    public boolean canFit(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(ModItems.RFID_CHIP);
    }

    @Override
    public IRecipe setRegistryName(ResourceLocation name) {
        this.name = name;
        return this;
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName() {
        return name;
    }

    @Override
    public Class<IRecipe> getRegistryType() {
        return IRecipe.class;
    }

    @Override
    public String getGroup() {
        return group.toString();
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.PlaceEvent event) {
        // Drop the RFID chip when a tagged item block is placed
        if (event.getPlayer().isCreative())
            return;
        ItemStack item = event.getPlayer().getHeldItem(event.getHand());
        if (RfidTag.hasTag(item)) {
            ItemStack chip = RfidTag.createChip(new RfidTag(item));
            EntityItem chipEntity = new EntityItem(
                    event.getWorld(),
                    event.getBlockSnapshot().getPos().getX(),
                    event.getBlockSnapshot().getPos().getY(),
                    event.getBlockSnapshot().getPos().getZ(),
                    chip
            );
            event.getWorld().spawnEntity(chipEntity);
        }
    }
}
