package com.megacurioschest.common;

import com.megacurioschest.Config;
import com.megacurioschest.compat.CuriosHelper;
import com.megacurioschest.networking.EnderChestSyncPacket;
import com.megacurioschest.networking.Network;
import com.megacurioschest.networking.PageChangePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.common.inventory.CurioSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import java.lang.reflect.Field;

public class MegaContainer extends AbstractContainerMenu {

    private static Field lastSlotsField;
    private static NonNullList<ItemStack> getLastSlots(AbstractContainerMenu menu) {
        try {
            if (lastSlotsField == null) {
                lastSlotsField = AbstractContainerMenu.class.getDeclaredField("lastSlots");
                lastSlotsField.setAccessible(true);
            }
            @SuppressWarnings("unchecked")
            NonNullList<ItemStack> list = (NonNullList<ItemStack>) lastSlotsField.get(menu);
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    private final Player player;
    private final ItemStackHandler handler;
    private final int rows;
    private final int cols;
    private final int containerSize;

    // 所有 curios 槽分组信息(不分页)
    private final List<CuriosHelper.SlotGroup> allCurioGroups = new ArrayList<>();
    // 当前页已加入容器的 curios 槽信息(与 slots 中尾部 CurioSlot 一一对应)
    private final List<CuriosHelper.SlotInfo> currentCurioInfos = new ArrayList<>();
    // 当前页显示的组索引范围
    private int currentGroupStart = 0;
    private int currentGroupEnd = 0;
    private int maxPage = 1;
    private int currentPage = 1;

    /** 普通版构造器（物品NBT存储） */
    public MegaContainer(int windowId, Inventory playerInv, ItemStack chestItem) {
        super(com.megacurioschest.MegaCuriosChest.MEGA_CONTAINER.get(), windowId);
        this.player = playerInv.player;
        this.handler = (MegaStackHandler) chestItem.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve().orElseGet(() -> new MegaStackHandler(chestItem, com.megacurioschest.items.MegaCuriosItem.DEFAULT_CAPACITY));
        this.rows = Config.rows();
        this.cols = Config.cols();
        this.containerSize = rows * cols;
        addSlots();
    }

    /** 末影版构造器（UUID存储） */
    public MegaContainer(int windowId, Inventory playerInv, UUID ownerUUID) {
        super(com.megacurioschest.MegaCuriosChest.ENDER_MEGA_CONTAINER.get(), windowId);
        this.player = playerInv.player;
        this.handler = new EnderMegaStackHandler(ownerUUID, com.megacurioschest.items.MegaCuriosItem.DEFAULT_CAPACITY);
        this.rows = Config.rows();
        this.cols = Config.cols();
        this.containerSize = rows * cols;
        addSlots();
    }

    /** 普通版客户端从网络重建 */
    public static MegaContainer fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf data) {
        int slotIndex = data.readInt();
        ItemStack item = playerInv.getItem(slotIndex);
        if (item.isEmpty() && slotIndex >= 0 && slotIndex < playerInv.getContainerSize()) {
            item = playerInv.getItem(slotIndex);
        }
        return new MegaContainer(windowId, playerInv, item);
    }

    /** 末影版客户端从网络重建 */
    public static MegaContainer fromNetworkEnder(int windowId, Inventory playerInv, FriendlyByteBuf data) {
        UUID ownerUUID = data.readUUID();
        return new MegaContainer(windowId, playerInv, ownerUUID);
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getCurrentPage() { return currentPage; }
    public int getMaxPage() { return maxPage; }
    public int getCuriosCount() {
        int total = 0;
        for (CuriosHelper.SlotGroup group : allCurioGroups) {
            total += group.slots.size();
        }
        return total;
    }
    public int getCurioCols() { return 3; }
    public int getCurioRows() {
        int totalH = rows * 18 + 18 + 14 + 76;
        int panelTop = 18;
        int headerH = 14 + 4 + 14 + 4;
        int footerH = 4 + 12;
        int availH = totalH - panelTop - headerH - footerH;
        return Math.max(3, availH / 18);
    }
    public int getCurioPanelLeft() { return cols * 18 + 8 + 8; }
    public int getCurioPanelTop() { return 18; }
    public List<CuriosHelper.SlotGroup> getCurrentPageGroups() {
        List<CuriosHelper.SlotGroup> result = new ArrayList<>();
        for (int i = currentGroupStart; i < currentGroupEnd && i < allCurioGroups.size(); i++) {
            result.add(allCurioGroups.get(i));
        }
        return result;
    }

    private void addSlots() {
        addContainerSlots();
        addPlayerSlots();
        addCuriosSlots();
    }

    /**
     * 翻页时只重建饰品槽,不清理容器槽和玩家槽。
     * 避免 slots.clear() 导致 AbstractContainerMenu 内部 lastSlots/remoteSlots 不同步。
     */
    private void refreshCurioSlots() {
        // 移除旧的饰品槽 (它们在 slots 列表末尾)
        NonNullList<ItemStack> lastSlots = getLastSlots(this);
        while (!this.slots.isEmpty() && this.slots.get(this.slots.size() - 1) instanceof CurioSlot) {
            int lastIdx = this.slots.size() - 1;
            this.slots.remove(lastIdx);
            if (lastSlots != null && lastIdx < lastSlots.size()) {
                lastSlots.remove(lastIdx);
            }
        }
        currentCurioInfos.clear();
        addCuriosSlots();
    }

    private void addContainerSlots() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                addSlot(new SlotItemHandler(handler, col + row * cols, col * 18 + 8, row * 18 + 18) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return CuriosHelper.isTrinket(stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxStackSize(@NotNull ItemStack stack) {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public @NotNull ItemStack remove(int amount) {
                        return super.remove(Math.min(amount, 1));
                    }
                });
            }
        }
    }

    private void addPlayerSlots() {
        int playerY = rows * 18 + 18 + 14;
        // 玩家背包 3 行 9 列,居中于容器
        int playerXOffset = (cols * 18 - 9 * 18) / 2;
        if (playerXOffset < 0) playerXOffset = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(player.getInventory(), col + row * 9 + 9, col * 18 + 8 + playerXOffset, playerY + row * 18));
            }
        }
        // 热栏
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(player.getInventory(), col, col * 18 + 8 + playerXOffset, playerY + 58));
        }
    }

    private void addCuriosSlots() {
        allCurioGroups.clear();
        allCurioGroups.addAll(CuriosHelper.getCurioSlotGroups(player));

        if (allCurioGroups.isEmpty()) {
            maxPage = 1;
            currentPage = 1;
            currentGroupStart = 0;
            currentGroupEnd = 0;
            return;
        }

        calculatePagination();
        if (currentPage > maxPage) currentPage = maxPage;
        if (currentPage < 1) currentPage = 1;
        calculateCurrentPageGroups();

        int curioStartX = cols * 18 + 8 + 8 + 8;
        int curioSlotsStartY = getCurioPanelTop() + 14 + 4 + 14 + 4;
        int curioCols = getCurioCols();

        int slotIndex = 0;
        for (int g = currentGroupStart; g < currentGroupEnd && g < allCurioGroups.size(); g++) {
            CuriosHelper.SlotGroup group = allCurioGroups.get(g);
            for (int i = 0; i < group.slots.size(); i++) {
                CuriosHelper.SlotInfo info = group.slots.get(i);
                int row = slotIndex / curioCols;
                int col = slotIndex % curioCols;
                int sx = curioStartX + col * 18;
                int sy = curioSlotsStartY + row * 18;
                CurioSlot slot = CuriosHelper.createCurioSlot(player, group, i, sx, sy);
                info.slot = slot;
                addSlot(slot);
                currentCurioInfos.add(info);
                slotIndex++;
            }
        }
    }

    private void calculatePagination() {
        int slotsPerPage = getCurioCols() * getCurioRows();
        int totalSlots = 0;
        int pages = 1;
        int pageSlots = 0;
        for (CuriosHelper.SlotGroup group : allCurioGroups) {
            int groupSize = group.slots.size();
            if (pageSlots + groupSize > slotsPerPage && pageSlots > 0) {
                pages++;
                pageSlots = 0;
            }
            pageSlots += groupSize;
            if (pageSlots >= slotsPerPage) {
                pages++;
                pageSlots = 0;
            }
        }
        maxPage = Math.max(1, pages);
    }

    private void calculateCurrentPageGroups() {
        int slotsPerPage = getCurioCols() * getCurioRows();
        int page = 1;
        int pageSlots = 0;
        currentGroupStart = 0;
        currentGroupEnd = 0;
        boolean foundStart = false;

        for (int g = 0; g < allCurioGroups.size(); g++) {
            CuriosHelper.SlotGroup group = allCurioGroups.get(g);
            int groupSize = group.slots.size();

            if (pageSlots + groupSize > slotsPerPage && pageSlots > 0) {
                if (page == currentPage && !foundStart) {
                    currentGroupStart = g;
                    foundStart = true;
                }
                page++;
                pageSlots = 0;
                if (page > currentPage && foundStart) {
                    currentGroupEnd = g;
                    return;
                }
            }

            if (page == currentPage && !foundStart) {
                currentGroupStart = g;
                foundStart = true;
            }

            pageSlots += groupSize;
            currentGroupEnd = g + 1;

            if (pageSlots >= slotsPerPage) {
                page++;
                pageSlots = 0;
                if (page > currentPage && foundStart) {
                    return;
                }
                foundStart = false;
            }
        }
    }

    /** 翻页:客户端先发包再本地翻页,服务端直接翻页 */
    public void changePage(boolean next) {
        if (player.level().isClientSide()) {
            Network.sendToServer(new PageChangePacket(next));
        }
        if (next && currentPage < maxPage) currentPage++;
        if (!next && currentPage > 1) currentPage--;
        refreshCurioSlots();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int containerEnd = containerSize;
        int playerEnd = containerEnd + 36;
        int curioEnd = this.slots.size();

        if (index < containerEnd) {
            // 容器槽 -> 玩家栏
            if (!moveItemStackTo(original, containerEnd, playerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < playerEnd) {
            // 玩家栏 -> 容器槽(饰品),用自定义堆叠逻辑
            if (CuriosHelper.isTrinket(original)) {
                if (!moveToContainerWithStacking(original)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        } else {
            // curios 槽 -> 容器槽,用自定义堆叠逻辑
            if (!moveToContainerWithStacking(original)) {
                return ItemStack.EMPTY;
            }
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    private boolean moveToContainerWithStacking(ItemStack stack) {
        if (stack.isEmpty()) return true;

        // 第一步:尝试合并到已有相同物品的槽
        for (int i = 0; i < containerSize; i++) {
            Slot slot = this.slots.get(i);
            ItemStack slotStack = slot.getItem();
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, stack)) {
                int canAdd = Integer.MAX_VALUE - slotStack.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(stack.getCount(), canAdd);
                    ItemStack newStack = slotStack.copy();
                    newStack.grow(toAdd);
                    slot.set(newStack);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) return true;
                }
            }
        }

        // 第二步:放到空槽
        for (int i = 0; i < containerSize; i++) {
            Slot slot = this.slots.get(i);
            if (!slot.hasItem() && slot.mayPlace(stack)) {
                slot.set(stack.copy());
                stack.setCount(0);
                return true;
            }
        }

        return stack.isEmpty();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clicked(int slotIndex, int button, @NotNull ClickType type, @NotNull Player player) {
        int containerEnd = containerSize;
        // 阻止把饰品箱本身移进容器
        if (slotIndex >= containerEnd && slotIndex < containerEnd + 36 && slotIndex >= 0 && slotIndex < this.slots.size()) {
            Slot s = this.slots.get(slotIndex);
            if (s.hasItem() && s.getItem().getItem() instanceof com.megacurioschest.items.MegaCuriosItem) {
                return;
            }
        }
        // 右键容器槽:快捷装备
        if (button == 1 && type == ClickType.PICKUP && slotIndex >= 0 && slotIndex < containerEnd) {
            Slot s = this.slots.get(slotIndex);
            if (s.hasItem()) {
                swapCurios(s, player, false);
                return;
            }
        }
        // shift+右键:装备到第二个槽
        if (button == 1 && type == ClickType.QUICK_MOVE && slotIndex >= 0 && slotIndex < containerEnd) {
            Slot s = this.slots.get(slotIndex);
            if (s.hasItem()) {
                swapCurios(s, player, true);
                return;
            }
        }
        super.clicked(slotIndex, button, type, player);
    }

    /**
     * 快捷装备核心:把容器槽的物品装备到第一个可用的 curios 槽。
     * 避开通用饰品栏(curio identifier):先找具体槽,找不到才用通用槽。
     * 每次只装备1个。
     */
    public void swapCurios(Slot containerSlot, Player player, boolean skipFirst) {
        ItemStack containerStack = containerSlot.getItem();
        if (containerStack.isEmpty()) return;

        ItemStack toEquip = containerStack.copyWithCount(1);
        CurioSlot target = findTargetSlot(toEquip, skipFirst, false);
        if (target == null) {
            target = findTargetSlot(toEquip, skipFirst, true);
        }
        if (target == null) return;

        ItemStack oldCurio = target.getItem().copy();
        target.set(toEquip);

        ItemStack newContainerStack;
        if (containerStack.getCount() > 1) {
            newContainerStack = containerStack.copy();
            newContainerStack.shrink(1);
        } else {
            newContainerStack = ItemStack.EMPTY;
        }

        if (oldCurio.isEmpty()) {
            containerSlot.set(newContainerStack);
        } else {
            if (newContainerStack.isEmpty()) {
                containerSlot.set(oldCurio);
            } else if (ItemStack.isSameItemSameTags(newContainerStack, oldCurio)) {
                newContainerStack.grow(oldCurio.getCount());
                containerSlot.set(newContainerStack);
            } else {
                containerSlot.set(newContainerStack);
                if (!player.getInventory().add(oldCurio)) {
                    player.drop(oldCurio, false);
                }
            }
        }
        target.setChanged();
        containerSlot.setChanged();
    }

    /**
     * 查找目标饰品槽。
     * @param toEquip 要装备的物品
     * @param skipFirst 是否跳过第一个匹配槽(shift+右键)
     * @param allowGeneric 是否允许通用槽
     */
    private CurioSlot findTargetSlot(ItemStack toEquip, boolean skipFirst, boolean allowGeneric) {
        // 第一步:找空槽(不替换任何物品)
        CurioSlot empty = findEmptySlot(toEquip, skipFirst, allowGeneric);
        if (empty != null) return empty;

        // 第二步:找可替换的槽(跳过受保护的)
        return findReplaceableSlot(toEquip, skipFirst, allowGeneric);
    }

    private CurioSlot findEmptySlot(ItemStack toEquip, boolean skipFirst, boolean allowGeneric) {
        boolean skipped = !skipFirst;
        for (CuriosHelper.SlotInfo info : currentCurioInfos) {
            if (!allowGeneric && Config.avoidGeneric() && info.isGeneric()) continue;
            if (!info.slot.getItem().isEmpty()) continue;
            if (info.slot.mayPlace(toEquip)) {
                if (!skipped) { skipped = true; continue; }
                return info.slot;
            }
        }
        return null;
    }

    private CurioSlot findReplaceableSlot(ItemStack toEquip, boolean skipFirst, boolean allowGeneric) {
        boolean skipped = !skipFirst;
        for (CuriosHelper.SlotInfo info : currentCurioInfos) {
            if (!allowGeneric && Config.avoidGeneric() && info.isGeneric()) continue;
            if (CuriosHelper.isSwapProtected(info.slot.getItem())) continue;
            if (info.slot.mayPlace(toEquip)) {
                if (!skipped) { skipped = true; continue; }
                return info.slot;
            }
        }
        return null;
    }

    /** 供网络包调用:服务端重建槽位 (curios 槽位数量变化时) */
    public void rebuildSlots() {
        refreshCurioSlots();
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        // 末影饰品箱关闭时，将最新内容同步到客户端缓存，保证 JEI 后续可用
        if (!player.level().isClientSide()
                && handler instanceof EnderMegaStackHandler
                && player instanceof ServerPlayer serverPlayer) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty()) {
                    items.add(s.copy());
                }
            }
            Network.sendToPlayer(new EnderChestSyncPacket(items), serverPlayer);
        }
    }
}
