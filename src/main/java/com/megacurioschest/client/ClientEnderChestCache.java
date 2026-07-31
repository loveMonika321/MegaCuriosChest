package com.megacurioschest.client;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端缓存末影饰品箱的物品列表，供 TransferHelperMixin 在 JEI 配方填充时使用。
 *
 * 数据来源：
 * - 玩家登录时由服务端通过 EnderChestSyncPacket 同步。
 * - 玩家关闭末影饰品箱界面时由服务端再次同步最新内容。
 *
 * 仅存储非空物品堆叠的副本，JEI 只需知道有哪些物品可用及其数量。
 */
public final class ClientEnderChestCache {

    private static volatile List<ItemStack> cached = Collections.emptyList();

    private ClientEnderChestCache() {}

    /** 替换缓存内容（调用方负责传入不可变或安全的副本列表）。 */
    public static void setItems(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            cached = Collections.emptyList();
        } else {
            List<ItemStack> copy = new ArrayList<>(items.size());
            for (ItemStack s : items) {
                if (s != null && !s.isEmpty()) {
                    copy.add(s.copy());
                }
            }
            cached = copy;
        }
    }

    /** 返回当前缓存（可能为空列表，不会返回 null）。 */
    public static List<ItemStack> getItems() {
        return cached;
    }

    /** 玩家退出时清理。 */
    public static void clear() {
        cached = Collections.emptyList();
    }
}
