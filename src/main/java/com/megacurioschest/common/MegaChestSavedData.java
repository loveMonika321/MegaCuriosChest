package com.megacurioschest.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 按玩家 UUID 存储饰品箱数据的世界级 SavedData。
 * 每个玩家拥有独立的饰品箱内容，与物品实例无关。
 */
public class MegaChestSavedData extends SavedData {

    private static final String DATA_NAME = "megacurioschest_chest_data";

    // UUID -> 物品栏 NBT 列表
    private final Map<UUID, ListTag> playerInventories = new HashMap<>();

    public MegaChestSavedData() {
    }

    public static MegaChestSavedData get() {
        ServerLevel level = ServerLifecycleHooks.getCurrentServer().overworld();
        return level.getDataStorage().computeIfAbsent(MegaChestSavedData::load, MegaChestSavedData::new, DATA_NAME);
    }

    private static MegaChestSavedData load(CompoundTag nbt) {
        MegaChestSavedData data = new MegaChestSavedData();
        for (String key : nbt.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
                data.playerInventories.put(uuid, list);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        for (Map.Entry<UUID, ListTag> entry : playerInventories.entrySet()) {
            nbt.put(entry.getKey().toString(), entry.getValue());
        }
        return nbt;
    }

    /**
     * 获取指定玩家的物品栏列表（不存在则创建空列表）。
     */
    public ListTag getInventory(UUID uuid, int size) {
        ListTag list = playerInventories.get(uuid);
        if (list == null) {
            list = new ListTag();
            while (list.size() < size) {
                list.add(new CompoundTag());
            }
            playerInventories.put(uuid, list);
            setDirty();
        }
        // 确保列表大小足够
        while (list.size() < size) {
            list.add(new CompoundTag());
        }
        return list;
    }

    /**
     * 保存指定玩家的物品栏列表。
     */
    public void setInventory(UUID uuid, ListTag list) {
        playerInventories.put(uuid, list);
        setDirty();
    }
}
