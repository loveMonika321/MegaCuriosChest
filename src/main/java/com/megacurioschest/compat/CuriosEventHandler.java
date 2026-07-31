package com.megacurioschest.compat;

import com.megacurioschest.MegaCuriosChest;
import com.megacurioschest.common.MegaContainer;
import com.megacurioschest.networking.Network;
import com.megacurioschest.networking.SlotChangedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.SlotModifiersUpdatedEvent;

/**
 * 监听 curios 槽位数量变化(如装备了增加槽位的饰品),重建容器槽位。
 */
@Mod.EventBusSubscriber(modid = MegaCuriosChest.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CuriosEventHandler {

    @SubscribeEvent
    public static void onCuriosSlotsModified(SlotModifiersUpdatedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.containerMenu instanceof MegaContainer menu)) return;
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            Network.sendToPlayer(new SlotChangedPacket(), serverPlayer);
        }
        menu.rebuildSlots();
    }
}
