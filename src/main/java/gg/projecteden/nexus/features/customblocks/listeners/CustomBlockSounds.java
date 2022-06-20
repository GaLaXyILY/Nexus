package gg.projecteden.nexus.features.customblocks.listeners;

import gg.projecteden.api.common.utils.TimeUtils.TickTime;
import gg.projecteden.api.common.utils.UUIDUtils;
import gg.projecteden.nexus.Nexus;
import gg.projecteden.nexus.features.customblocks.CustomBlocks.BlockAction;
import gg.projecteden.nexus.features.customblocks.CustomBlocks.ReplacedSoundType;
import gg.projecteden.nexus.features.customblocks.CustomBlocks.SoundAction;
import gg.projecteden.nexus.features.customblocks.NoteBlockUtils;
import gg.projecteden.nexus.features.customblocks.models.CustomBlock;
import gg.projecteden.nexus.models.cooldown.CooldownService;
import gg.projecteden.nexus.utils.BlockUtils;
import gg.projecteden.nexus.utils.NMSUtils;
import gg.projecteden.nexus.utils.Nullables;
import gg.projecteden.nexus.utils.SoundBuilder;
import gg.projecteden.parchment.event.sound.SoundEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerAnimationEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomBlockSounds implements Listener {
	private static final Map<Player, BlockAction> playerActionMap = new ConcurrentHashMap<>();

	public CustomBlockSounds() {
		Nexus.registerListener(this);
	}

	public static void updateAction(Player player, BlockAction action) {
		playerActionMap.put(player, action);
	}

	@EventHandler
	public void on(BlockDamageEvent event) {
		if (event.isCancelled()) return;

		updateAction(event.getPlayer(), BlockAction.HIT);
	}

	@EventHandler
	public void on(BlockPlaceEvent event) {
		if (event.isCancelled()) return;

		updateAction(event.getPlayer(), BlockAction.PLACE);
	}

	@EventHandler
	public void on(NotePlayEvent event) {
		event.setCancelled(true);

		CustomBlock customBlock = CustomBlock.fromBlock(event.getBlock());
		if (CustomBlock.NOTE_BLOCK != customBlock)
			return;

		NoteBlock noteBlock = (NoteBlock) event.getBlock().getBlockData();
		NoteBlockUtils.play(noteBlock, event.getBlock().getLocation(), true);
	}

	// Handles Sound: FALL
	@EventHandler
	public void on(EntityDamageEvent event) {
		if (event.isCancelled())
			return;

		if (!(event.getEntity() instanceof Player player))
			return;

		if (!event.getCause().equals(DamageCause.FALL))
			return;

		Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		if (Nullables.isNullOrAir(block))
			return;

		updateAction(player, BlockAction.FALL);
		tryPlaySound(SoundAction.FALL, block);
	}

	// Handles Sound: HIT
	@EventHandler
	public void on(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		Block block = player.getTargetBlockExact(5);
		if (block == null)
			return;

		if (!playerActionMap.containsKey(player))
			return;

		if (playerActionMap.get(player) == BlockAction.HIT) {
			tryPlaySound(SoundAction.HIT, block);
		}
	}

	// Handles Sound: BREAK
	@EventHandler
	public void on(BlockBreakEvent event) {
		if (event.isCancelled())
			return;

		updateAction(event.getPlayer(), BlockAction.BREAK);

		Block brokenBlock = event.getBlock();
		CustomBlock brokenCustomBlock = CustomBlock.fromBlock(brokenBlock);
		if (brokenCustomBlock == null) {
			tryPlaySound(SoundAction.BREAK, brokenBlock);
		}
	}

	// Handles Sound: STEP
	@EventHandler
	public void on(SoundEvent event) {
		try {
			Block block = event.getEmitter().location().getBlock();
			Block below = block.getRelative(BlockFace.DOWN);
			Block source = null;

			CustomBlock _customBlock = CustomBlock.fromBlock(block);
			if (_customBlock != null)
				source = block;
			else {
				_customBlock = CustomBlock.fromBlock(below);
				if (_customBlock != null)
					source = below;
			}

			net.kyori.adventure.sound.Sound sound = event.getSound();
			SoundAction soundAction = SoundAction.fromSound(sound);
			if (soundAction == null)
				return;

			if (!Nullables.isNullOrAir(source)) {
				if (soundAction != SoundAction.STEP)
					return;

				event.setCancelled(true);
				_customBlock.playSound(null, soundAction, source.getLocation());
				return;
			}

			ReplacedSoundType soundType = ReplacedSoundType.fromSound(sound.name().value());
			if (soundType == null)
				return;

			if (tryPlaySound(soundAction, soundType, event.getEmitter().location()))
				event.setCancelled(true);


		} catch (Exception ignored) {}
	}

	public static void tryPlaySound(SoundAction soundAction, Block block) {
		Sound sound = NMSUtils.getSound(soundAction, block);
		if (sound == null)
			return;

		ReplacedSoundType soundType = ReplacedSoundType.fromSound(sound);
		if (soundType == null)
			return; // handled by minecraft

		String customSound = "custom." + sound.getKey().getKey();
		String defaultSound = soundAction.getCustomSound(soundType);
		if (!customSound.equalsIgnoreCase(defaultSound)) {
			return;
		}

		tryPlaySound(soundAction, soundType, block.getLocation());
	}

	public static boolean tryPlaySound(SoundAction soundAction, ReplacedSoundType soundType, Location location) {
		String soundKey = soundAction.getCustomSound(soundType);
		SoundBuilder soundBuilder = new SoundBuilder(soundKey).location(location).volume(soundAction.getVolume());

		String locationStr = location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
		String cooldownType = "CustomDefaultSound_" + soundAction + "_" + locationStr;
		if (!(new CooldownService().check(UUIDUtils.UUID0, cooldownType, TickTime.TICK.x(3)))) {
			return false;
		}

//		debug("&6CustomDefaultSound:&f " + soundAction + " - " + soundKey);
		BlockUtils.playSound(soundBuilder);
		return true;
	}

}
