package com.megacurioschest.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 普通饰品箱菜单提供者。携带物品在玩家背包中的槽索引。
 */
public class MegaMenuProvider implements MenuProvider {

    private final ItemStack itemStack;
    private final int slotIndex;

    public MegaMenuProvider(ItemStack itemStack, int slotIndex) {
        this.itemStack = itemStack;
        this.slotIndex = slotIndex;
    }

    public static MegaMenuProvider provider(ItemStack itemStack, int slotIndex) {
        return new MegaMenuProvider(itemStack, slotIndex);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("megacurioschest.container.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player) {
        return new MegaContainer(windowId, playerInv, itemStack);
    }

    public void writeExtraData(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
    }
}
