package com.megacurioschest.mixin;

import com.megacurioschest.common.EnderMegaStackHandler;
import com.megacurioschest.items.MegaCuriosItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让 Beyond Dimensions 的合成台菜单在执行 JEI 配方填充时，
 * 优先从末影饰品箱提取物品，其次玩家背包，最后维度网络存储。
 *
 * 使用 targets 字符串声明目标类，避免对 Beyond Dimensions 的编译期硬依赖，
 * 仅在 Beyond Dimensions 存在时由 Mixin 应用（目标类未加载则不触发）。
 */
@Mixin(targets = "com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu")
public class DimensionsCraftMenuMixin {

    /** 缓存的末影饰品箱处理器，菜单存活期间复用，避免每次提取都重新加载 SavedData。 */
    @Unique
    private EnderMegaStackHandler megacurioschest$enderHandler;

    /**
     * 拦截 DimensionsCraftMenu.transferRecipe 中对 extractFromInventory 的调用，
     * 在提取玩家背包之前先尝试从未影饰品箱提取。
     *
     * 由于 extractFromInventory 为私有方法，此处不复用原实现，
     * 而是以等价逻辑重新实现玩家背包提取，避免 Redirect 与 Shadow 同时作用于同一调用的限制。
     */
    @Redirect(
            method = "transferRecipe",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wintercogs/beyonddimensions/common/menu/DimensionsCraftMenu;extractFromInventory(Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;I)I"
            )
    )
    private int megacurioschest$extractEnderFirst(Inventory inventory, ItemStack template, int amount) {
        int remaining = amount;
        // 仅服务端执行：末影饰品箱数据保存在服务端 SavedData 中
        if (!inventory.player.level().isClientSide()) {
            remaining = megacurioschest$extractFromEnder(inventory.player, template, remaining);
        }
        if (remaining > 0) {
            remaining = megacurioschest$extractFromInventory(inventory, template, remaining);
        }
        return remaining;
    }

    /**
     * 优先从未影饰品箱提取，返回仍未满足的数量。
     * 复用 EnderMegaStackHandler，提取时通过其 onContentsChanged 自动写回 SavedData。
     */
    @Unique
    private int megacurioschest$extractFromEnder(Player player, ItemStack template, int amount) {
        if (amount <= 0) return amount;
        EnderMegaStackHandler handler = megacurioschest$getEnderHandler(player);
        int remaining = amount;
        for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, template)) {
                int extract = Math.min(remaining, stack.getCount());
                handler.extractItem(i, extract, false);
                remaining -= extract;
            }
        }
        return remaining;
    }

    /** 等价于 Beyond Dimensions 的 extractFromInventory：从玩家背包主槽位(0-35)提取。 */
    @Unique
    private int megacurioschest$extractFromInventory(Inventory inventory, ItemStack template, int amount) {
        int remaining = amount;
        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameTags(stack, template)) {
                int extract = Math.min(remaining, stack.getCount());
                stack.shrink(extract);
                remaining -= extract;
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        return remaining;
    }

    @Unique
    private EnderMegaStackHandler megacurioschest$getEnderHandler(Player player) {
        if (megacurioschest$enderHandler == null) {
            megacurioschest$enderHandler = new EnderMegaStackHandler(player.getUUID(), MegaCuriosItem.DEFAULT_CAPACITY);
        }
        return megacurioschest$enderHandler;
    }
}
