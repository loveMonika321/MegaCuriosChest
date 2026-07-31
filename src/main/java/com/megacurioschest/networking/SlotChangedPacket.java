package com.megacurioschest.networking;

import com.megacurioschest.common.MegaContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** curios 槽位数量变化时,通知客户端重建容器槽位。服务端 -> 客户端 */
public class SlotChangedPacket {
    public SlotChangedPacket() {}
    public SlotChangedPacket(@NotNull FriendlyByteBuf buf) {}
    public void encode(@NotNull FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu instanceof MegaContainer menu) {
                menu.rebuildSlots();
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
