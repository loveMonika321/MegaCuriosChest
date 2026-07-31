package com.megacurioschest.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 末影饰品箱的物品栏数据。
 *
 * TPS 优化:
 * - 构造时一次性从 SavedData 加载到 ItemStackHandler 的 stacks 缓存。
 * - 读操作直接访问内存 stacks(旧实现每次都去 SavedData.get().getInventory() 新建 ListTag 引用)。
 * - 修改时通过 onContentsChanged 懒写回 SavedData。
 * - 客户端不再通过 serializeNBT/deserializeNBT 往返,直接用内存 stacks。
 *
 * 相同饰品可堆叠到 int 最大值。
 */
public class EnderMegaStackHandler extends ItemStackHandler implements ICapabilityProvider {

    private final UUID ownerUUID;
    private final boolean clientSide;
    private final LazyOptional<IItemHandler> holder = LazyOptional.of(() -> this);

    public EnderMegaStackHandler(UUID ownerUUID, int size) {
        super(size);
        this.ownerUUID = ownerUUID;
        this.clientSide = ServerLifecycleHooks.getCurrentServer() == null;
        if (!clientSide) {
            loadFromSavedData();
        }
    }

    private void loadFromSavedData() {
        MegaChestSavedData data = MegaChestSavedData.get();
        ListTag list = data.getInventory(ownerUUID, stacks.size());
        while (list.size() < stacks.size()) {
            list.add(new CompoundTag());
        }
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.of(list.getCompound(i)));
        }
    }

    private void saveToSavedData() {
        if (clientSide) return;
        MegaChestSavedData data = MegaChestSavedData.get();
        ListTag list = new ListTag();
        for (ItemStack s : stacks) {
            list.add(s.save(new CompoundTag()));
        }
        data.setInventory(ownerUUID, list);
    }

    @Override
    protected void onContentsChanged(int slot) {
        saveToSavedData();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        return stacks.get(slot);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= stacks.size()) return;
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack toInsert, boolean simulate) {
        if (toInsert.isEmpty()) return ItemStack.EMPTY;
        if (slot < 0 || slot >= stacks.size()) return toInsert;
        ItemStack existing = stacks.get(slot);
        int limit = getStackLimit(slot, toInsert);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, toInsert)) return toInsert;
            limit -= existing.getCount();
        }
        if (limit <= 0) return toInsert;
        boolean reachedLimit = toInsert.getCount() <= limit;
        if (!simulate) {
            ItemStack result = existing.isEmpty() ? toInsert.copy() : existing;
            result.setCount(existing.isEmpty() ? toInsert.getCount() : existing.getCount() + toInsert.getCount());
            stacks.set(slot, result);
            onContentsChanged(slot);
        }
        return reachedLimit ? ItemStack.EMPTY : copyWithCount(toInsert, toInsert.getCount() - limit);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, existing.getCount());
        ItemStack result = copyWithCount(existing, toExtract);
        if (!simulate) {
            existing.shrink(toExtract);
            if (existing.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    private static ItemStack copyWithCount(ItemStack src, int count) {
        ItemStack copy = src.copy();
        copy.setCount(count);
        return copy;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, holder);
    }
}
