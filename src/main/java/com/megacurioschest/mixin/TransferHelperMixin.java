package com.megacurioschest.mixin;

import com.megacurioschest.client.ClientEnderChestCache;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * 让 Beyond Dimensions 的 JEI 配方填充(TransferHelper)在构建可用物品池时，
 * 把末影饰品箱内的物品一并纳入，使 "+" 能识别并存放在饰品箱中的材料。
 *
 * 仅修改 transferRecipe 的 playerInv 参数(将其替换为 玩家背包 + 饰品箱物品 的合并列表)，
 * 不触碰存储/合成槽来源，也不依赖 Beyond Dimensions 的内部类型(如私有类 Avail)。
 *
 * 使用 targets 字符串声明目标类，避免编译期硬依赖；饰品箱数据由客户端缓存提供
 * (ClientEnderChestCache)，在登录与关闭饰品箱界面时同步。
 */
@Mixin(targets = "com.wintercogs.beyonddimensions.integration.module.jei.transfer.TransferHelper")
public class TransferHelperMixin {

    /**
     * transferRecipe 为静态方法，其第 3 个参数(index=2)即 playerInv(List&lt;ItemStack&gt;)。
     * 在方法入口将其替换为合并了饰品箱物品的新列表；原列表(NonNullList)不会被修改。
     */
    @ModifyVariable(
            method = "transferRecipe",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static List<ItemStack> megacurioschest$mergeEnderChestItems(List<ItemStack> playerInv) {
        List<ItemStack> enderItems = ClientEnderChestCache.getItems();
        if (enderItems.isEmpty()) {
            return playerInv;
        }
        List<ItemStack> combined = new ArrayList<>(playerInv.size() + enderItems.size());
        combined.addAll(playerInv);
        combined.addAll(enderItems);
        return combined;
    }
}
