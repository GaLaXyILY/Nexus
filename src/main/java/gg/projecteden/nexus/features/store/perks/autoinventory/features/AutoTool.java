package gg.projecteden.nexus.features.store.perks.autoinventory.features;

import gg.projecteden.nexus.Nexus;
import gg.projecteden.nexus.features.customblocks.customblockbreaking.BrokenBlock;
import gg.projecteden.nexus.features.customblocks.models.CustomBlock;
import gg.projecteden.nexus.features.listeners.events.PlayerBlockDigEvent;
import gg.projecteden.nexus.features.store.perks.autoinventory.AutoInventory;
import gg.projecteden.nexus.features.store.perks.autoinventory.AutoInventoryFeature;
import gg.projecteden.nexus.models.autoinventory.AutoInventoryUser;
import gg.projecteden.nexus.utils.Enchant;
import gg.projecteden.nexus.utils.MaterialTag;
import gg.projecteden.nexus.utils.PlayerUtils;
import gg.projecteden.nexus.utils.ToolType.ToolGrade;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static gg.projecteden.nexus.utils.Nullables.isNullOrAir;
import static gg.projecteden.nexus.utils.PlayerUtils.getHotbarContents;
import static gg.projecteden.nexus.utils.StringUtils.camelCase;
import static gg.projecteden.nexus.utils.StringUtils.pretty;

@NoArgsConstructor
public class AutoTool implements Listener {
	public static final String PERMISSION = "autotool.use";

	@EventHandler
	public void on(PlayerBlockDigEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (player.getTargetEntity(5) != null)
			return;
		if (block == null)
			return;
		if (!(player.hasPermission(AutoInventory.PERMISSION) || player.hasPermission(PERMISSION)))
			return;

		final AutoInventoryUser user = AutoInventoryUser.of(player);

		if (!user.hasFeatureEnabled(AutoInventoryFeature.AUTOTOOL))
			return;

		final ItemStack mainHand = player.getInventory().getItemInMainHand();

		if (!MaterialTag.TOOLS.isTagged(mainHand)) {
			if (!MaterialTag.SWORDS.isTagged(mainHand))
				return;

			if (!user.isAutoToolIncludeSword())
				return;
		}

		List<ItemStack> contents = Arrays.stream(getHotbarContents(player)).toList();
		PlayerUtils.selectHotbarItem(player, getBestTool(player, contents, block));
	}

	@Nullable
	public static ItemStack getBestTool(Player player, List<ItemStack> items, Block block) {
		List<ItemStack> hotbar = new ArrayList<>(items);
		hotbar.add(null);

		Consumer<String> debug = message -> {
			if (Nexus.isDebug())
				PlayerUtils.send(player, message);
		};

		final Function<ItemStack, Integer> getBreakTime = tool ->
			new BrokenBlock(block, CustomBlock.fromBlock(block) != null, player, tool, Bukkit.getCurrentTick()).getBreakTicks();

		final ItemStack currentItem = player.getInventory().getItemInMainHand();
		final Integer currentToolBreakTime = getBreakTime.apply(currentItem);

		debug.accept("Block: " + camelCase(block.getType()));

		final ItemStack bestTool = Collections.min(hotbar, Comparator.comparingDouble(item -> {
			debug.accept("");
			debug.accept("Item: " + (item == null ? "&cnull" : "&e" + camelCase(item.getType())));
			if (isNullOrAir(item)) {
				debug.accept("  MAX_VALUE - 1 (is null or air)");
				return Integer.MAX_VALUE - 1;
			}

			if (!block.isPreferredTool(item)) {
				if (!MaterialTag.INFESTED_STONE.isTagged(block.getType()) || !MaterialTag.PICKAXES.isTagged(item)) {
					debug.accept("  MAX_VALUE (unpreferred tool)");
					return Integer.MAX_VALUE;
				}
			}

			if (MaterialTag.ALL_GLASS.isTagged(block.getType())) {
				if (item.containsEnchantment(Enchant.SILK_TOUCH)) {
					final ToolGrade toolGrade = ToolGrade.of(item);
					if (toolGrade != null) {
						debug.accept("  %d (silk touch)".formatted(toolGrade.ordinal()));
						return toolGrade.ordinal();
					} else {
						debug.accept("  MAX_VALUE - 1 (Unknown tool with silk touch)");
						return Integer.MAX_VALUE - 1;
					}
				}

				debug.accept("  MAX_VALUE (glass)");
				return Integer.MAX_VALUE;
			}

			if (MaterialTag.TOOLS_GOLD.isTagged(item.getType())) {
				debug.accept("  MAX_VALUE (golden tool)");
				return Integer.MAX_VALUE;
			}

			if (MaterialTag.CROPS.isTagged(block.getType()))
				if (MaterialTag.HOES.isNotTagged(item.getType()))
					return Integer.MAX_VALUE;

			final int breakTime = getBreakTime.apply(item);
			if (breakTime >= 1) {
				if (!item.equals(currentItem) && breakTime == currentToolBreakTime) {
					debug.accept("  MAX_VALUE (break time same as current tool)");
					return Integer.MAX_VALUE;
				}

				debug.accept("  %s (breakTime)".formatted(breakTime));
				return breakTime;
			}

			debug.accept("  MAX_VALUE (default)");
			return Integer.MAX_VALUE;
		}));

		debug.accept("");
		debug.accept("");
		debug.accept("Best tool: " + (bestTool == null ? "null" : pretty(bestTool)));
		return bestTool;
	}

}
