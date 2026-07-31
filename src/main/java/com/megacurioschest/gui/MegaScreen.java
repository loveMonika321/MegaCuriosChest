package com.megacurioschest.gui;

import com.megacurioschest.common.MegaContainer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import top.theillusivec4.curios.common.inventory.CurioSlot;

public class MegaScreen extends AbstractContainerScreen<MegaContainer> {

    private static final int SLOT_BG = 0xFF2B2B2B;
    private static final int SLOT_BORDER = 0xFF8B8B8B;
    private static final int SLOT_SEPARATOR = 0xFF5A5A5A;
    private static final int PANEL_BG = 0xFF3C3C3C;
    private static final int PANEL_BORDER = 0xFF6E6E6E;

    private Button btnNext;
    private Button btnPrev;
    private int curioPanelLeft;
    private int curioPanelTop;
    private int curioPanelW;
    private int curioPanelH;

    // 容器槽区域批量绘制缓存
    private int chestL, chestT, chestR, chestB; // 饰品箱容器区（相对屏幕leftPos/topPos的偏移直接用slot计算）
    private int playerL, playerT, playerR, playerB;
    private int hotbarL, hotbarT, hotbarR, hotbarB;
    private int firstCurioL = -1, firstCurioT = -1, lastCurioL = -1, lastCurioT = -1;

    public MegaScreen(MegaContainer menu, Inventory inv, Component title) {
        super(menu, inv, title);
        int rows = menu.getRows();
        int cols = menu.getCols();
        this.curioPanelLeft = cols * 18 + 8 + 8;
        this.curioPanelTop = 18;
        this.curioPanelW = menu.getCurioCols() * 18 + 16;
        int totalH = rows * 18 + 18 + 14 + 76 + 8;
        this.curioPanelH = totalH - curioPanelTop - 8;
        this.imageWidth = curioPanelLeft + curioPanelW + 8;
        this.imageHeight = totalH;
        this.inventoryLabelY = rows * 18 + 18 + 12;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        int px = leftPos + curioPanelLeft + 6;
        int py = topPos + curioPanelTop + 18;
        int halfW = (curioPanelW - 16) / 2 - 2;
        btnPrev = Button.builder(Component.translatable("megacurioschest.gui.page.prev"),
                        b -> getMenu().changePage(false))
                .pos(px, py).size(halfW, 14).build();
        btnNext = Button.builder(Component.translatable("megacurioschest.gui.page.next"),
                        b -> getMenu().changePage(true))
                .pos(px + halfW + 4, py).size(halfW, 14).build();
        addRenderableWidget(btnPrev);
        addRenderableWidget(btnNext);
        cacheSlotRegions();
    }

    /** 一次性计算各区域边界,避免每帧遍历全部 slots */
    private void cacheSlotRegions() {
        // 饰品箱容器槽:0 ~ rows*cols-1, 第一个槽x=8,y=18
        int cols = menu.getCols();
        int rows = menu.getRows();
        chestL = 8 - 1;
        chestT = 18 - 1;
        chestR = 8 + cols * 18 + 1;
        chestB = 18 + rows * 18 + 1;

        // 玩家背包 3x9 和热栏 1x9: 它们接在容器下方 (MegaContainer addPlayerSlots 中计算)
        int playerY = rows * 18 + 18 + 14;
        int playerXOffset = (cols * 18 - 9 * 18) / 2;
        if (playerXOffset < 0) playerXOffset = 0;

        playerL = 8 + playerXOffset - 1;
        playerT = playerY - 1;
        playerR = 8 + playerXOffset + 9 * 18 + 1;
        playerB = playerY + 3 * 18 + 1;

        hotbarL = playerL;
        hotbarT = playerY + 58 - 1;
        hotbarR = playerR;
        hotbarB = playerY + 58 + 18 + 1;

        // Curio slot 边界
        firstCurioL = -1; lastCurioL = -1; firstCurioT = -1; lastCurioT = -1;
        for (Slot s : menu.slots) {
            if (s instanceof CurioSlot) {
                if (firstCurioL < 0) { firstCurioL = s.x; firstCurioT = s.y; }
                lastCurioL = s.x; lastCurioT = s.y;
            }
        }
    }

    /** 当翻页导致 curios slot 变化时重算边界 */
    public void refreshCurioBounds() {
        cacheSlotRegions();
    }

    /**
     * 绘制一个槽区域:
     *  1. 外围一条大边框 (1 次 fill)
     *  2. 内部槽背景整块填充 (1 次 fill)
     *  3. 横向分隔线 (rows-1 次)
     *  4. 纵向分隔线 (cols-1 次)
     *
     *  单次调用总 fill 次数: rows+cols+2, 而不是 slots × 2
     *  例如 8x9=72 槽:旧实现 144 次 fill → 优化后 19 次 fill
     */
    private void drawSlotRegion(GuiGraphics gg, int x, int y, int rows, int cols) {
        int w = cols * 18;
        int h = rows * 18;
        // 1. 外边框 (整块矩形)
        gg.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_BORDER);
        // 2. 内部背景
        gg.fill(x, y, x + w, y + h, SLOT_BG);
        // 3. 纵向分隔线 (每两个槽之间)
        for (int c = 1; c < cols; c++) {
            int lx = x + c * 18 - 1;
            gg.fill(lx, y, lx + 1, y + h, SLOT_SEPARATOR);
        }
        // 4. 横向分隔线
        for (int r = 1; r < rows; r++) {
            int ty = y + r * 18 - 1;
            gg.fill(x, ty, x + w, ty + 1, SLOT_SEPARATOR);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partial, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        gg.fill(x - 2, y - 2, x + w + 2, y + h + 2, PANEL_BORDER);
        gg.fill(x, y, x + w, y + h, PANEL_BG);
        gg.fill(x, y, x + w, y + 12, 0xFF4A4A4A);

        // === 饰品箱容器槽 (批量) ===
        int rowsM = menu.getRows();
        int colsM = menu.getCols();
        drawSlotRegion(gg, x + 8, y + 18, rowsM, colsM);

        // === 玩家背包 3 行 (批量) ===
        int playerY = rowsM * 18 + 18 + 14;
        int playerXOffset = (colsM * 18 - 9 * 18) / 2;
        if (playerXOffset < 0) playerXOffset = 0;
        drawSlotRegion(gg, x + 8 + playerXOffset, y + playerY, 3, 9);

        // === 热栏 1 行 (批量) ===
        drawSlotRegion(gg, x + 8 + playerXOffset, y + playerY + 58, 1, 9);

        // === 饰品面板 ===
        int cpx = x + curioPanelLeft;
        int cpy = y + curioPanelTop;
        gg.fill(cpx - 2, cpy - 2, cpx + curioPanelW + 2, cpy + curioPanelH + 2, PANEL_BORDER);
        gg.fill(cpx, cpy, cpx + curioPanelW, cpy + curioPanelH, PANEL_BG);
        gg.fill(cpx, cpy, cpx + curioPanelW, cpy + 14, 0xFF4A4A4A);
        gg.drawString(this.font, Component.translatable("megacurioschest.gui.curios"),
                cpx + 6, cpy + 3, 0xFFFFFF);

        // === CurioSlot 批量区域 (如果有) ===
        if (firstCurioL >= 0) {
            // 计算 curios 槽的行列数: MegaContainer 用 getCurioCols() 列按行排
            int curioCols = menu.getCurioCols();
            int slotStartX = colsM * 18 + 8 + 8 + 8 + 8; // curioStartX + 8 + 8 内边距
            int slotStartY = curioPanelTop + 14 + 4 + 14 + 4; // 面板标题高度
            // 计算当前页实际 curio 数量与行列
            int count = 0;
            for (Slot s : menu.slots) if (s instanceof CurioSlot) count++;
            if (count > 0) {
                int r = (count + curioCols - 1) / curioCols;
                int c = Math.min(curioCols, count);
                drawSlotRegion(gg, x + slotStartX, y + slotStartY, r, c);
            }
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        this.renderBackground(gg);
        super.render(gg, mouseX, mouseY, partial);

        gg.drawString(this.font, this.title, leftPos + titleLabelX, topPos + titleLabelY, 0xFFFFFF);
        gg.drawString(this.font, this.playerInventoryTitle,
                leftPos + (curioPanelLeft - this.font.width(this.playerInventoryTitle)) / 2,
                topPos + inventoryLabelY, 0xFFFFFF);

        btnPrev.active = getMenu().getCurrentPage() > 1;
        btnNext.active = getMenu().getCurrentPage() < getMenu().getMaxPage();

        if (getMenu().getMaxPage() > 1) {
            String page = getMenu().getCurrentPage() + "/" + getMenu().getMaxPage();
            int px = leftPos + curioPanelLeft + curioPanelW / 2 - this.font.width(page) / 2;
            int py = topPos + curioPanelTop + curioPanelH - 14;
            gg.drawString(this.font, page, px, py, 0xFFFFFF);
        }

        this.renderTooltip(gg, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gg, int x, int y) {
        super.renderTooltip(gg, x, y);
        if (this.hoveredSlot != null && !(this.hoveredSlot instanceof CurioSlot)) {
            if (this.hoveredSlot.hasItem()) {
                gg.renderTooltip(this.font,
                        Component.translatable("megacurioschest.gui.rmb_equip"),
                        x, y - 12);
            }
        }
    }
}
