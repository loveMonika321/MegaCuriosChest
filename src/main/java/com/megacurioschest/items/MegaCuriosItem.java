package com.megacurioschest.items;

import com.megacurioschest.common.MegaMenuProvider;
import com.megacurioschest.common.MegaStackHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * 便携饰品箱物品。右键打开容器。
 * 物品栏数据通过 Capability (MegaStackHandler) 提供,存于物品 NBT。
 */
public class MegaCuriosItem extends Item {

    public static final int DEFAULT_CAPACITY = 72; // 8 行 x 9 列,实际容量由 Config 决定

    public MegaCuriosItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ServerPlayer sender = (ServerPlayer) player;
            int slotIndex = (hand == InteractionHand.MAIN_HAND)
                    ? player.getInventory().selected
                    : 40;
            NetworkHooks.openScreen(sender, MegaMenuProvider.provider(itemStack, slotIndex),
                    buf -> buf.writeInt(slotIndex));
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new MegaStackHandler(stack, DEFAULT_CAPACITY);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
