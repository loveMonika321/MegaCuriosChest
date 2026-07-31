package com.megacurioschest.networking;

import com.megacurioschest.common.EnderMegaMenuProvider;
import com.megacurioschest.items.EnderMegaCuriosItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** 快捷键打开末影饰品箱:服务端查找玩家身上的 EnderMegaCuriosItem 并打开菜单。 */
public class OpenEnderContainerPacket {
    public OpenEnderContainerPacket() {}
    public OpenEnderContainerPacket(@NotNull FriendlyByteBuf buf) {}
    public void encode(@NotNull FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            // 检查玩家身上是否有末影饰品箱
            boolean hasEnderChest = player.getMainHandItem().getItem() instanceof EnderMegaCuriosItem
                    || player.getOffhandItem().getItem() instanceof EnderMegaCuriosItem;
            if (!hasEnderChest) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i).getItem() instanceof EnderMegaCuriosItem) {
                        hasEnderChest = true;
                        break;
                    }
                }
            }
            if (!hasEnderChest) return;
            NetworkHooks.openScreen(player, EnderMegaMenuProvider.provider(player.getUUID()),
                    buf -> buf.writeUUID(player.getUUID()));
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
