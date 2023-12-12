package gg.projecteden.nexus.features.customenchants.enchants;

import com.gmail.nossr50.events.fake.FakeBlockBreakEvent;
import gg.projecteden.api.common.utils.TimeUtils.TickTime;
import gg.projecteden.nexus.Nexus;
import gg.projecteden.nexus.features.customblocks.customblockbreaking.BrokenBlock;
import gg.projecteden.nexus.features.customenchants.CustomEnchant;
import gg.projecteden.nexus.models.nickname.Nickname;
import gg.projecteden.nexus.utils.BlockUtils;
import gg.projecteden.nexus.utils.Enchant;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static gg.projecteden.api.common.utils.RandomUtils.chanceOf;
import static java.util.Collections.emptyList;
import static org.bukkit.block.BlockFace.EAST;
import static org.bukkit.block.BlockFace.NORTH;
import static org.bukkit.block.BlockFace.SOUTH;
import static org.bukkit.block.BlockFace.WEST;

public class TunnelingEnchant extends CustomEnchant implements Listener {

	public TunnelingEnchant(@NotNull NamespacedKey key) {
		super(key);
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	private static final long BREAK_TICKS_THRESHOLD = TickTime.SECOND.x(2);

	@EventHandler(ignoreCancelled = true)
	public void on(BlockBreakEvent event) {
		if (event instanceof FakeBlockBreakEvent)
			return;

		var original = event.getBlock();

		var player = event.getPlayer();
		var tool = player.getInventory().getItemInMainHand();

		if (!tool.getItemMeta().hasEnchant(Enchant.TUNNELING))
			return;

		var blocks = tunnel(player, original);
		var durability = (Damageable) tool.getItemMeta();
		var unbreaking = tool.getEnchantmentLevel(Enchant.UNBREAKING);

		for (Block block : blocks) {
			if (block == original)
				continue;

			if (!block.isPreferredTool(tool))
				continue;

			final int breakTicks = new BrokenBlock(block, player, tool).getBreakTicks();
//			Dev.GRIFFIN.send("Break ticks for %s: %d".formatted(StringUtils.camelCase(block.getType()), breakTicks));
			if (breakTicks > BREAK_TICKS_THRESHOLD)
				continue;

			if (!new FakeBlockBreakEvent(block, player).callEvent())
				continue;

			block.breakNaturally(tool, true, true);
			if (chanceOf(100 / (unbreaking + 1)))
				durability.setDamage(durability.getDamage() + 1);
		}

		tool.setItemMeta(durability);
	}

	private List<Block> tunnel(Player player, Block block) {
		var pitch = player.getPitch();
		var facing = player.getFacing();

		if (pitch < -45 || pitch > 45) {
			return BlockUtils.getBlocksInRadius(block, 1, 0, 1);
		} else if (facing == EAST || facing == WEST) {
			return BlockUtils.getBlocksInRadius(block, 0, 1, 1);
		} else if (facing == NORTH || facing == SOUTH) {
			return BlockUtils.getBlocksInRadius(block, 1, 1, 0);
		} else {
			Nexus.severe("Unhandled tunneling direction (Player: %s, Pitch: %s, Facing: %s)".formatted(Nickname.of(player), pitch, facing));
			return emptyList();
		}
	}

}
