package com.megacurioschest.networking;

import com.megacurioschest.common.MegaContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** 翻页:客户端 -> 服务端 */
public class PageChangePacket {
    private final boolean next;
    public PageChangePacket(boolean next) { this.next = next; }
    public PageChangePacket(@NotNull FriendlyByteBuf buf) { this.next = buf.readBoolean(); }
    public void encode(@NotNull FriendlyByteBuf buf) { buf.writeBoolean(next); }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() != null && ctx.get().getSender().containerMenu instanceof MegaContainer menu) {
                menu.changePage(next);
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
