package com.megacurioschest.compat;

import com.megacurioschest.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.inventory.CurioSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.NonNullList;

/**
 * Curios API 封装。集中处理饰品槽读取、判断与装备。
 */
public class CuriosHelper {

    /** curios 的"通用饰品栏" identifier,默认 "curio" */
    public static final String GENERIC_IDENTIFIER = "curio";

    /** 某个槽位的信息:槽实例 + identifier + 槽内索引 */
    public static class SlotInfo {
        public CurioSlot slot;
        public final String identifier;
        public final int index;
        public SlotInfo(CurioSlot slot, String identifier, int index) {
            this.slot = slot; this.identifier = identifier; this.index = index;
        }
        public boolean isGeneric() {
            return GENERIC_IDENTIFIER.equals(identifier);
        }
    }

    /**
     * 饰品槽分组信息:按 identifier 分组
     */
    public static class SlotGroup {
        public final String identifier;
        public final List<SlotInfo> slots = new ArrayList<>();
        public final boolean canToggleRendering;
        public final IDynamicStackHandler stackHandler;
        public SlotGroup(String identifier, boolean canToggleRendering, IDynamicStackHandler stackHandler) {
            this.identifier = identifier;
            this.canToggleRendering = canToggleRendering;
            this.stackHandler = stackHandler;
        }
    }

    /**
     * 获取玩家所有饰品槽(按 identifier 分组),用于在容器中展示。
     * 只保存创建 CurioSlot 需要的数据,不直接创建 CurioSlot。
     */
    public static List<SlotGroup> getCurioSlotGroups(Player player) {
        List<SlotGroup> groups = new ArrayList<>();
        Optional<ICuriosItemHandler> opt = CuriosApi.getCuriosInventory(player).resolve();
        if (opt.isEmpty()) return groups;
        ICuriosItemHandler handler = opt.get();
        Map<String, ICurioStacksHandler> curioMap = handler.getCurios();
        for (Map.Entry<String, ICurioStacksHandler> e : curioMap.entrySet()) {
            String identifier = e.getKey();
            ICurioStacksHandler stacksHandler = e.getValue();
            IDynamicStackHandler stackHandler = stacksHandler.getStacks();
            SlotGroup group = new SlotGroup(identifier, stacksHandler.canToggleRendering(), stackHandler);
            for (int i = 0; i < stackHandler.getSlots(); i++) {
                group.slots.add(new SlotInfo(null, identifier, i));
            }
            if (!group.slots.isEmpty()) {
                groups.add(group);
            }
        }
        return groups;
    }

    /**
     * 创建一个 CurioSlot 实例
     */
    public static CurioSlot createCurioSlot(Player player, SlotGroup group, int index, int x, int y) {
        NonNullList<Boolean> renders = NonNullList.withSize(group.stackHandler.getSlots(), false);
        return new ProtectedCurioSlot(player, group.stackHandler, index, group.identifier,
                x, y, renders, group.canToggleRendering);
    }

    /**
     * 获取玩家所有饰品槽(带 identifier 元信息),用于在容器中展示。
     * @deprecated 请使用 getCurioSlotGroups
     */
    @Deprecated
    public static List<SlotInfo> getCurioSlots(Player player, int startX, int startY) {
        List<SlotInfo> out = new ArrayList<>();
        for (SlotGroup group : getCurioSlotGroups(player)) {
            out.addAll(group.slots);
        }
        return out;
    }

    /** 是否为 curios 槽 */
    public static boolean isCurioSlot(Object slot) {
        return slot instanceof CurioSlot;
    }

    /**
     * 判断饰品是否受保护,不能被快捷装备替换掉。
     * 检查:绑定诅咒、消失诅咒、配置黑名单。
     */
    public static boolean isSwapProtected(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (Config.preventSwapBindingCurse() && stack.getEnchantmentLevel(Enchantments.BINDING_CURSE) > 0) {
            return true;
        }
        if (Config.preventSwapVanishingCurse() && stack.getEnchantmentLevel(Enchantments.VANISHING_CURSE) > 0) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) {
            String idStr = id.toString();
            for (String entry : Config.swapBlacklist()) {
                if (idStr.equals(entry)) return true;
            }
        }
        return false;
    }

    /**
     * 判断物品是否为饰品:带 curios 标签, 或实现了 ICurio 接口。
     * 用于容器槽的 mayPlace 过滤。
     */
    public static boolean isTrinket(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ICurio) return true;
        // 1.20.1: stack.getTags() 返回 Stream<TagKey<Item>>
        boolean[] found = {false};
        stack.getTags().forEach(tag -> {
            ResourceLocation loc = tag.location();
            if ("curios".equals(loc.getNamespace())) found[0] = true;
        });
        return found[0];
    }

    /**
     * 判断物品能否放入指定 identifier 的槽位。
     * 用 CurioSlot.mayPlace 间接判断:构造临时判断。
     */
    public static boolean canEquipIn(Player player, ItemStack stack, String identifier) {
        Optional<ICuriosItemHandler> opt = CuriosApi.getCuriosInventory(player).resolve();
        if (opt.isEmpty()) return false;
        ICurioStacksHandler stacksHandler = opt.get().getCurios().get(identifier);
        if (stacksHandler == null) return false;
        IDynamicStackHandler stackHandler = stacksHandler.getStacks();
        return canEquipInStatic(stackHandler, identifier, 0, stack);
    }

    /**
     * 静态方法:判断某个槽位能否放入物品。
     */
    public static boolean canEquipInStatic(IDynamicStackHandler stackHandler, String identifier, int index, ItemStack stack) {
        NonNullList<Boolean> renders = NonNullList.withSize(stackHandler.getSlots(), false);
        CurioSlot slot = new CurioSlot(null, stackHandler, index, identifier,
                0, 0, renders, false);
        return slot.mayPlace(stack);
    }
}
