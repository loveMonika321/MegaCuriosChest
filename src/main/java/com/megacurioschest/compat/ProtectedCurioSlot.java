package com.megacurioschest.compat;

import com.megacurioschest.Config;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.inventory.CurioSlot;

import net.minecraft.core.NonNullList;

public class ProtectedCurioSlot extends CurioSlot {

    public ProtectedCurioSlot(Player player, IDynamicStackHandler handler, int index,
                              String identifier, int xPosition, int yPosition,
                              NonNullList<Boolean> renders, boolean canToggleRendering) {
        super(player, handler, index, identifier, xPosition, yPosition, renders, canToggleRendering);
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack stack = getItem();
        if (!stack.isEmpty()) {
            if (Config.preventSwapBindingCurse() && stack.getEnchantmentLevel(Enchantments.BINDING_CURSE) > 0) {
                return false;
            }
            if (Config.preventSwapVanishingCurse() && stack.getEnchantmentLevel(Enchantments.VANISHING_CURSE) > 0) {
                return false;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) {
                String idStr = id.toString();
                for (String entry : Config.swapBlacklist()) {
                    if (idStr.equals(entry)) return false;
                }
            }
        }
        return super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return super.mayPlace(stack);
    }
}
