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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 普通饰品箱的物品栏数据。
 *
 * TPS 优化:
 * - 不在每次 getStackInSlot 时都解析 NBT (旧实现),而是构造时一次加载进 ItemStackHandler 自带的 stacks 缓存。
 * - 修改操作直接在 stacks 上进行,完成后同步写回 NBT。
 * - 避免了容器每 tick 数百次 NBT 反序列化/ListTag 补齐循环。
 *
 * 相同饰品可堆叠到 int 最大值。
 */
public class MegaStackHandler extends ItemStackHandler implements ICapabilityProvider {

    public static final String NBT_KEY = "mega_inventory";

    private final ItemStack host;
    private final LazyOptional<IItemHandler> holder = LazyOptional.of(() -> this);

    public MegaStackHandler(ItemStack host, int size) {
        super(size);
        this.host = host;
        loadFromNbt();
    }

    private void loadFromNbt() {
        final ListTag list;
        if (!host.isEmpty() && host.hasTag() && host.getOrCreateTag().contains(NBT_KEY)) {
            list = host.getOrCreateTag().getList(NBT_KEY, Tag.TAG_COMPOUND);
        } else {
            list = new ListTag();
        }
        while (list.size() < stacks.size()) {
            list.add(new CompoundTag());
        }
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.of(list.getCompound(i)));
        }
    }

    private void saveToNbt() {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            list.add(stack.save(new CompoundTag()));
        }
        host.getOrCreateTag().put(NBT_KEY, list);
    }

    @Override
    protected void onContentsChanged(int slot) {
        saveToNbt();
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
