package com.megacurioschest.networking;

import com.megacurioschest.MegaCuriosChest;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Network {
    private static SimpleChannel INSTANCE;
    private static int id = 0;
    private static int nextID() { return id++; }

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MegaCuriosChest.MOD_ID, "main"),
                () -> "1.0",
                s -> true,
                s -> true);

        INSTANCE.messageBuilder(OpenContainerPacket.class, nextID())
                .encoder(OpenContainerPacket::encode)
                .decoder(OpenContainerPacket::new)
                .consumerNetworkThread(OpenContainerPacket::handle)
                .add();

        INSTANCE.messageBuilder(PageChangePacket.class, nextID())
                .encoder(PageChangePacket::encode)
                .decoder(PageChangePacket::new)
                .consumerNetworkThread(PageChangePacket::handle)
                .add();

        INSTANCE.messageBuilder(EnderChestSyncPacket.class, nextID())
                .encoder(EnderChestSyncPacket::encode)
                .decoder(EnderChestSyncPacket::new)
                .consumerNetworkThread(EnderChestSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(SlotChangedPacket.class, nextID())
                .encoder(SlotChangedPacket::encode)
                .decoder(SlotChangedPacket::new)
                .consumerNetworkThread(SlotChangedPacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenEnderContainerPacket.class, nextID())
                .encoder(OpenEnderContainerPacket::encode)
                .decoder(OpenEnderContainerPacket::new)
                .consumerNetworkThread(OpenEnderContainerPacket::handle)
                .add();
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToPlayer(Object packet, net.minecraft.server.level.ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAllClients(Object packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
}
