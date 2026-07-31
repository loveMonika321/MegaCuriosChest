package com.megacurioschest;

import com.megacurioschest.gui.MegaScreen;
import com.megacurioschest.items.EnderMegaCuriosItem;
import com.megacurioschest.items.MegaCuriosItem;
import com.megacurioschest.common.MegaContainer;
import com.megacurioschest.networking.Network;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Mod(MegaCuriosChest.MOD_ID)
public class MegaCuriosChest {
    public static final String MOD_ID = "megacurioschest";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 物品注册
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> MEGA_CHEST = ITEMS.register("mega_curios_chest",
            () -> new MegaCuriosItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ENDER_MEGA_CHEST = ITEMS.register("ender_mega_curios_chest",
            () -> new EnderMegaCuriosItem(new Item.Properties().stacksTo(1)));

    // 菜单类型注册
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final RegistryObject<MenuType<MegaContainer>> MEGA_CONTAINER =
            MENUS.register("mega_container", () -> IForgeMenuType.create(MegaContainer::fromNetwork));
    public static final RegistryObject<MenuType<MegaContainer>> ENDER_MEGA_CONTAINER =
            MENUS.register("ender_mega_container", () -> IForgeMenuType.create(MegaContainer::fromNetworkEnder));

    // 快捷键:打开普通饰品箱
    public static final KeyMapping OPEN_CHEST_KEY = new KeyMapping(
            "key.megacurioschest.open_chest",
            GLFW.GLFW_KEY_K,
            "key.categories.misc");

    // 快捷键:打开末影饰品箱
    public static final KeyMapping OPEN_ENDER_CHEST_KEY = new KeyMapping(
            "key.megacurioschest.open_ender_chest",
            GLFW.GLFW_KEY_N,
            "key.categories.misc");

    public MegaCuriosChest() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.register(this);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "megacurioschest-common.toml");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent e) {
            e.enqueueWork(Network::register);
            LOGGER.info("[MegaCuriosChest] 已加载。");
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent e) {
            e.enqueueWork(() -> {
                MenuScreens.register(MEGA_CONTAINER.get(), MegaScreen::new);
                MenuScreens.register(ENDER_MEGA_CONTAINER.get(), MegaScreen::new);
            });
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
            e.register(OPEN_CHEST_KEY);
            e.register(OPEN_ENDER_CHEST_KEY);
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent e) {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                e.accept(MEGA_CHEST.get());
                e.accept(ENDER_MEGA_CHEST.get());
            }
        }
    }
}
