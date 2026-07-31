package com.megacurioschest.common;

import com.megacurioschest.MegaCuriosChest;
import com.megacurioschest.networking.EnderChestSyncPacket;
import com.megacurioschest.networking.Network;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服务端 Forge 事件处理：
 * - 玩家登录时同步末影饰品箱内容到客户端，使 JEI 在玩家尚未打开饰品箱时也能识别其中的材料。
 */
@Mod.EventBusSubscriber(modid = MegaCuriosChest.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonForgeEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            List<ItemStack> items = collectEnderChestItems(serverPlayer.getUUID());
            Network.sendToPlayer(new EnderChestSyncPacket(items), serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            List<ItemStack> items = collectEnderChestItems(serverPlayer.getUUID());
            Network.sendToPlayer(new EnderChestSyncPacket(items), serverPlayer);
        }
    }

    /**
     * 从 SavedData 收集末影饰品箱中的非空物品列表。
     * 供登录同步与菜单关闭同步复用。
     */
    public static List<ItemStack> collectEnderChestItems(UUID uuid) {
        List<ItemStack> items = new ArrayList<>();
        MegaChestSavedData data = MegaChestSavedData.get();
        ListTag list = data.getInventory(uuid, com.megacurioschest.items.MegaCuriosItem.DEFAULT_CAPACITY);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ItemStack stack = ItemStack.of(tag);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }
}
