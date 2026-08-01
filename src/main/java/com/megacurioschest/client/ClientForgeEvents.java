package com.megacurioschest.client;

import com.megacurioschest.MegaCuriosChest;
import com.megacurioschest.networking.Network;
import com.megacurioschest.networking.OpenContainerPacket;
import com.megacurioschest.networking.OpenEnderContainerPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MegaCuriosChest.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    /** 快捷键打开饰品箱 */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (MegaCuriosChest.OPEN_CHEST_KEY.consumeClick()) {
            Network.sendToServer(new OpenContainerPacket());
        }
        if (MegaCuriosChest.OPEN_ENDER_CHEST_KEY.consumeClick()) {
            Network.sendToServer(new OpenEnderContainerPacket());
        }
    }
}
