package kr.glound.townrise;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TownRiseAreaTool {
    private static final String TOOL_MARKER = "townrise_area_tool";
    private static final Map<UUID, TownRiseAreaSelection> SELECTIONS = new ConcurrentHashMap<>();

    private TownRiseAreaTool() {
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("town")
                .then(Commands.literal("area")
                        .then(Commands.literal("create")
                                .executes(context -> startAreaCreation(context.getSource().getPlayerOrException())))));
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isAreaTool(event.getItemStack())) {
            return;
        }
        BlockPos pos = event.getPos();
        selection(player).setFirst(player.serverLevel().dimension(), pos);
        player.displayClientMessage(message("첫 번째 지점을 선택했습니다: " + format(pos), ChatFormatting.GREEN), false);
        cancelLeftClick(event);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isAreaTool(event.getItemStack())) {
            return;
        }
        BlockPos pos = event.getPos();
        selection(player).setSecond(player.serverLevel().dimension(), pos);
        player.displayClientMessage(message("두 번째 지점을 선택했습니다: " + format(pos) + " — 도구를 Q로 버리면 확정됩니다.", ChatFormatting.GREEN), false);
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static void onItemToss(ItemTossEvent event) {
        Player rawPlayer = event.getPlayer();
        if (!(rawPlayer instanceof ServerPlayer player)) {
            return;
        }
        ItemEntity itemEntity = event.getEntity();
        ItemStack tossed = itemEntity.getItem();
        if (!isAreaTool(tossed)) {
            return;
        }

        event.setCanceled(true);
        itemEntity.discard();
        confirmSelection(player);
    }

    public static boolean isAreaTool(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.GOLDEN_AXE)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(TOOL_MARKER);
    }

    public static ItemStack createTool() {
        ItemStack stack = new ItemStack(Items.GOLDEN_AXE);
        stack.set(DataComponents.ITEM_NAME, Component.literal("TownRise 구역 지정 도구").withStyle(ChatFormatting.GOLD));
        stack.set(DataComponents.RARITY, Rarity.RARE);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("좌클릭: 첫 번째 지점").withStyle(ChatFormatting.YELLOW),
                Component.literal("우클릭: 두 번째 지점").withStyle(ChatFormatting.YELLOW),
                Component.literal("Q로 버리기: 구역 확정").withStyle(ChatFormatting.AQUA),
                Component.literal("TownRise 전용 도구").withStyle(ChatFormatting.DARK_GREEN)
        )));
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TOOL_MARKER, true);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        return stack;
    }

    private static int startAreaCreation(ServerPlayer player) {
        SELECTIONS.put(player.getUUID(), new TownRiseAreaSelection());
        ItemStack tool = createTool();
        boolean added = player.getInventory().add(tool);
        if (!added) {
            player.drop(tool, false);
        }
        player.displayClientMessage(message("구역 지정을 시작합니다. 좌클릭/우클릭으로 두 지점을 선택한 뒤 도구를 Q로 버리세요.", ChatFormatting.AQUA), false);
        return 1;
    }

    private static void confirmSelection(ServerPlayer player) {
        TownRiseAreaSelection selection = SELECTIONS.get(player.getUUID());
        if (selection == null || !selection.hasAnyPoint()) {
            player.displayClientMessage(message("선택된 지점이 없습니다. /town area create 로 다시 시작하세요.", ChatFormatting.RED), false);
            return;
        }
        selection.completeArea().ifPresentOrElse(area -> {
            SELECTIONS.remove(player.getUUID());
            player.displayClientMessage(message("구역이 확정되었습니다: " + area.compactDescription(), ChatFormatting.GOLD), false);
            player.displayClientMessage(message("크기: " + area.blockVolume() + " blocks", ChatFormatting.GRAY), false);
        }, () -> player.displayClientMessage(message("두 지점이 모두 필요합니다. 좌클릭/우클릭 후 다시 Q로 확정하세요.", ChatFormatting.RED), false));
    }

    private static TownRiseAreaSelection selection(ServerPlayer player) {
        return SELECTIONS.computeIfAbsent(player.getUUID(), ignored -> new TownRiseAreaSelection());
    }

    private static void cancelLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCanceled(true);
    }

    private static String format(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static Component message(String text, ChatFormatting color) {
        return Component.literal("[TownRise] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(color));
    }
}
