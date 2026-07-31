package com.megacurioschest.items;

import com.megacurioschest.common.EnderMegaMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * 末影饰品箱物品。右键打开容器。
 * 数据绑定到玩家 UUID，存储在世界数据中，物品本身不存储数据。
 */
public class EnderMegaCuriosItem extends Item {

    public EnderMegaCuriosItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ServerPlayer sender = (ServerPlayer) player;
            NetworkHooks.openScreen(sender, EnderMegaMenuProvider.provider(player.getUUID()),
                    buf -> buf.writeUUID(player.getUUID()));
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
