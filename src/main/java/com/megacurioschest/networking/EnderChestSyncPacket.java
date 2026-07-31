package com.megacurioschest.networking;

import com.megacurioschest.client.ClientEnderChestCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 -> 客户端同步末影饰品箱物品列表。
 * 仅发送非空堆叠，客户端收到后写入 ClientEnderChestCache 供 JEI 使用。
 */
public class EnderChestSyncPacket {

    private final List<ItemStack> items;

    public EnderChestSyncPacket(List<ItemStack> items) {
        this.items = items;
    }

    public EnderChestSyncPacket(@NotNull FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ItemStack> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buf.readItem());
        }
        this.items = list;
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeVarInt(items.size());
        for (ItemStack s : items) {
            buf.writeItem(s);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 仅客户端处理；dedicated server 不会收到此 S2C 包
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientEnderChestCache.setItems(items);
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
