package com.megacurioschest;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue ROWS;
    public static final ForgeConfigSpec.IntValue COLS;
    public static final ForgeConfigSpec.BooleanValue AVOID_GENERIC_SLOT;
    public static final ForgeConfigSpec.BooleanValue PREVENT_SWAP_BINDING_CURSE;
    public static final ForgeConfigSpec.BooleanValue PREVENT_SWAP_VANISHING_CURSE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SWAP_BLACKLIST;

    static {
        BUILDER.comment("Mega Curios Chest 配置 / Mega Curios Chest Config")
               .push("general");
        ROWS = BUILDER.comment("容器行数 (1-12,默认8,容量=行数*列数).")
                      .defineInRange("rows", 8, 1, 12);
        COLS = BUILDER.comment("容器列数 (9-12,默认9).")
                      .defineInRange("cols", 9, 9, 12);
        AVOID_GENERIC_SLOT = BUILDER.comment("快捷装备时,是否不优先装备到通用饰品栏(curio identifier).true=优先具体槽,通用槽最后考虑.")
                                    .define("avoid_generic_slot", true);

        BUILDER.push("swap_blacklist");
        PREVENT_SWAP_BINDING_CURSE = BUILDER.comment("快捷装备时,有绑定诅咒的饰品是否不能被替换(默认true).")
                                             .define("prevent_swap_binding_curse", true);
        PREVENT_SWAP_VANISHING_CURSE = BUILDER.comment("快捷装备时,有消失诅咒的饰品是否不能被替换(默认false).")
                                               .define("prevent_swap_vanishing_curse", false);
        SWAP_BLACKLIST = BUILDER.comment("不能被替换的饰品物品ID列表(如\"apotheosis:mythic_ring\").匹配模式:namespace:path.")
                                .defineListAllowEmpty(Collections.singletonList("swap_blacklist_items"),
                                        () -> Arrays.asList("apotheosis:mythic_ring"),
                                        o -> o instanceof String);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static int rows() { return ROWS.get(); }
    public static int cols() { return COLS.get(); }
    public static boolean avoidGeneric() { return AVOID_GENERIC_SLOT.get(); }
    public static boolean preventSwapBindingCurse() { return PREVENT_SWAP_BINDING_CURSE.get(); }
    public static boolean preventSwapVanishingCurse() { return PREVENT_SWAP_VANISHING_CURSE.get(); }
    public static List<? extends String> swapBlacklist() { return SWAP_BLACKLIST.get(); }
}
