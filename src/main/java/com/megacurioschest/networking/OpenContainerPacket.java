package com.megacurioschest.networking;

import com.megacurioschest.common.MegaMenuProvider;
import com.megacurioschest.items.MegaCuriosItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** 快捷键打开普通饰品箱:服务端查找玩家身上的 MegaCuriosItem 并打开菜单。 */
public class OpenContainerPacket {
    public OpenContainerPacket() {}
    public OpenContainerPacket(@NotNull FriendlyByteBuf buf) {}
    public void encode(@NotNull FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ItemStack found = ItemStack.EMPTY;
            int slotIndex = -1;
            if (player.getMainHandItem().getItem() instanceof MegaCuriosItem) {
                found = player.getMainHandItem();
                slotIndex = player.getInventory().selected;
            } else if (player.getOffhandItem().getItem() instanceof MegaCuriosItem) {
                found = player.getOffhandItem();
                slotIndex = 40;
            } else {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack s = player.getInventory().getItem(i);
                    if (s.getItem() instanceof MegaCuriosItem) {
                        found = s; slotIndex = i; break;
                    }
                }
            }
            if (found.isEmpty() || slotIndex < 0) return;
            final int idx = slotIndex;
            NetworkHooks.openScreen(player, MegaMenuProvider.provider(found, idx), buf -> buf.writeInt(idx));
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
