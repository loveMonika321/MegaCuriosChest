package com.megacurioschest.common;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 末影饰品箱菜单提供者。携带玩家 UUID，数据从 SavedData 读取。
 */
public class EnderMegaMenuProvider implements MenuProvider {

    private final UUID ownerUUID;

    public EnderMegaMenuProvider(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public static EnderMegaMenuProvider provider(UUID ownerUUID) {
        return new EnderMegaMenuProvider(ownerUUID);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("megacurioschest.container.ender_title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player) {
        return new MegaContainer(windowId, playerInv, ownerUUID);
    }
}
