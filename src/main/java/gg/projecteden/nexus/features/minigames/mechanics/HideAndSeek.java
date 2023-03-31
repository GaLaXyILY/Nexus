package gg.projecteden.nexus.features.minigames.mechanics;

import gg.projecteden.api.common.utils.TimeUtils.TickTime;
import gg.projecteden.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import gg.projecteden.nexus.features.menus.MenuUtils;
import gg.projecteden.nexus.features.menus.api.ClickableItem;
import gg.projecteden.nexus.features.menus.api.annotations.Title;
import gg.projecteden.nexus.features.menus.api.content.InventoryProvider;
import gg.projecteden.nexus.features.minigames.Minigames;
import gg.projecteden.nexus.features.minigames.models.Match;
import gg.projecteden.nexus.features.minigames.models.Minigamer;
import gg.projecteden.nexus.features.minigames.models.events.matches.MatchEndEvent;
import gg.projecteden.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import gg.projecteden.nexus.features.minigames.models.events.matches.MatchQuitEvent;
import gg.projecteden.nexus.features.minigames.models.events.matches.MatchStartEvent;
import gg.projecteden.nexus.features.minigames.models.events.matches.minigamers.MinigamerDeathEvent;
import gg.projecteden.nexus.features.minigames.models.matchdata.HideAndSeekMatchData;
import gg.projecteden.nexus.features.minigames.models.perks.Perk;
import gg.projecteden.nexus.models.cooldown.CooldownService;
import gg.projecteden.nexus.utils.ItemBuilder;
import gg.projecteden.nexus.utils.JsonBuilder;
import gg.projecteden.nexus.utils.MaterialTag;
import gg.projecteden.nexus.utils.PlayerUtils;
import gg.projecteden.nexus.utils.PotionEffectBuilder;
import gg.projecteden.nexus.utils.SoundBuilder;
import gg.projecteden.nexus.utils.Tasks;
import gg.projecteden.nexus.utils.Utils;
import lombok.RequiredArgsConstructor;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static gg.projecteden.nexus.utils.Distance.distance;
import static gg.projecteden.nexus.utils.LocationUtils.blockLocationsEqual;
import static gg.projecteden.nexus.utils.LocationUtils.getCenteredLocation;
import static gg.projecteden.nexus.utils.Nullables.isNullOrAir;
import static gg.projecteden.nexus.utils.StringUtils.camelCase;
import static gg.projecteden.nexus.utils.StringUtils.plural;

public class HideAndSeek extends Infection {

	public static final ItemStack SELECTOR_ITEM = new ItemBuilder(Material.NETHER_STAR).name("&3&lSelect your Block").build();
	public static final ItemStack STUN_GRENADE = new ItemBuilder(Material.FIREWORK_STAR).name("&3&lStun Grenade").build();
	public static final ItemStack RADAR = new ItemBuilder(Material.RECOVERY_COMPASS).name("&3&lRadar").build();
	private static final long SOLIDIFY_PLAYER_AT = TickTime.SECOND.x(5);

	private static final long SELECTOR_COOLDOWN = TickTime.MINUTE.x(2.5);
	private static final CooldownService COOLDOWN_SERVICE = new CooldownService();

	@Override
	public @NotNull String getName() {
		return "Hide and Seek";
	}

	@Override
	public @NotNull String getDescription() {
		return "Disguise as a block and hide from the hunters";
	}

	@Override
	public @NotNull ItemStack getMenuItem() {
		return new ItemStack(Material.GRASS_BLOCK);
	}

	@Override
	public @NotNull GameMode getGameMode() {
		return GameMode.SURVIVAL;
	}

	@Override
	public boolean usesPerk(@NotNull Class<? extends Perk> perk, @NotNull Minigamer minigamer) {
		return isZombie(minigamer);
	}

	@Override
	public boolean canMoveArmor() {
		return false;
	}

	@Override
	public void onJoin(@NotNull MatchJoinEvent event) {
		super.onJoin(event);
		Minigamer minigamer = event.getMinigamer();
		Player player = minigamer.getOnlinePlayer();
		player.getInventory().setItem(0, SELECTOR_ITEM);
	}

	// Select unique concrete blocks
	@EventHandler
	public void setPlayerBlock(PlayerInteractEvent event) {
		if (event.getItem() == null) return;
		if (!Utils.ActionGroup.RIGHT_CLICK.applies(event)) return;

		Player player = event.getPlayer();
		if (!player.getWorld().equals(Minigames.getWorld())) return;

		Minigamer minigamer = Minigamer.of(player);
		if (!minigamer.isIn(this)) return;

		Match match = minigamer.getMatch();
		if (event.getItem().equals(SELECTOR_ITEM)) {
			if (match.isStarted()) {
				if (!COOLDOWN_SERVICE.check(player.getUniqueId(), "hide-and-seek-selector", SELECTOR_COOLDOWN, false)) {
					return;
				}
			}
			new HideAndSeekMenu(match).open(player);
		}
		if (event.getItem().equals(STUN_GRENADE))
			if (match.isStarted())
				StunGrenade.run(minigamer);
		if (event.getItem().equals(RADAR))
			if (match.isStarted())
				Radar.run(minigamer);
	}

	public void disguise(Minigamer minigamer, boolean midgame) {
		final Player player = minigamer.getOnlinePlayer();
		final Match match = minigamer.getMatch();
		final HideAndSeekMatchData matchData = match.getMatchData();
		final MiscDisguise disguise = new MiscDisguise(DisguiseType.FALLING_BLOCK, matchData.getBlockChoice(minigamer));
		disguise.setEntity(player);
		disguise.startDisguise();
		matchData.getDisguises().put(player.getUniqueId(), disguise);
		DisguiseAPI.setActionBarShown(player, false);
		minigamer.setImmobileTicks(0);
		applySelectorCooldown(minigamer);
		match.getScoreboard().update();
		if (midgame) {
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1F, 0.1F);
			player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation(), 50, .5, .5, .5, 0.01F);
		}
	}

	public void applySelectorCooldown(Minigamer minigamer) {
		COOLDOWN_SERVICE.check(minigamer.getOnlinePlayer(), "hide-and-seek-selector", SELECTOR_COOLDOWN);
		minigamer.getOnlinePlayer().setCooldown(SELECTOR_ITEM.getType(), (int) SELECTOR_COOLDOWN);
	}

	@Override
	public void onStart(@NotNull MatchStartEvent event) {
		super.onStart(event);
		Match match = event.getMatch();
		HideAndSeekMatchData matchData = match.getMatchData();
		if (matchData.getMapMaterials().size() == 0) {
			error("Arena has no blocks whitelisted!", match);
			return;
		}

		for (Minigamer minigamer : match.getMinigamers()) {
			if (isZombie(minigamer)) {
				continue;
			}

			disguise(minigamer, false);
		}

		int taskId = match.getTasks().repeat(0, 1, () -> {
			for (Minigamer minigamer : getHumans(match)) {
				Player player = minigamer.getOnlinePlayer();
				UUID userId = player.getUniqueId();
				Map<Minigamer, Location> solidPlayers = matchData.getSolidPlayers();
				int immobileTicks = minigamer.getImmobileTicks();
				Material blockChoice = matchData.getBlockChoice(userId);
				Component blockName = Component.translatable(blockChoice);

				// if player just moved, break their disguise
				if (immobileTicks < SOLIDIFY_PLAYER_AT && solidPlayers.containsKey(minigamer)) {
					blockChange(minigamer, solidPlayers.remove(minigamer), Material.AIR);
					PlayerUtils.showPlayer(player).to(minigamer.getMatch().getOnlinePlayers());
					if (player.hasPotionEffect(PotionEffectType.INVISIBILITY))
						player.removePotionEffect(PotionEffectType.INVISIBILITY);
					FallingBlock fallingBlock = matchData.getSolidBlocks().remove(minigamer.getOnlinePlayer().getUniqueId());
					if (fallingBlock != null)
						fallingBlock.remove();
					matchData.getDisguises().get(player.getUniqueId()).startDisguise();
				}

				Location location = player.getLocation();
				final Block down = location.getBlock().getRelative(BlockFace.DOWN);
				if (isNullOrAir(down) || down.isLiquid()) {
					sendActionBarWithTimer(minigamer, new JsonBuilder("&cYou cannot solidify here"));
				// check how long they've been still
				} else if (immobileTicks < TickTime.SECOND.x(2)) {
					sendActionBarWithTimer(minigamer, new JsonBuilder("&bYou are currently partially disguised as a ").next(blockName));
				} else if (immobileTicks < SOLIDIFY_PLAYER_AT) {
					// countdown until solidification
					int seconds = (int) Math.ceil((SOLIDIFY_PLAYER_AT - immobileTicks) / 20d);
					String display = String.format(plural("&dFully disguising in %d second", seconds) + "...", seconds);
					sendActionBarWithTimer(minigamer, display);
				} else {
					if (!solidPlayers.containsKey(minigamer)) {
						if (immobileTicks == SOLIDIFY_PLAYER_AT && MaterialTag.ALL_AIR.isTagged(location.getBlock().getType())) {
							// save fake block location
							solidPlayers.put(minigamer, location);
							// create a falling block to render on the hider's client
							if (blockChoice.isSolid() && blockChoice.isOccluding()) {
								BlockData blockData = blockChoice.createBlockData();

								// Copy nearby block data if logs
								if (MaterialTag.LOGS.isTagged(blockChoice)) {
									for (BlockFace blockFace : BlockFace.values()) {
										final Block relative = location.getBlock().getRelative(blockFace);
										if (relative.getType() == blockChoice) {
											blockData = relative.getBlockData();
											break;
										}
									}
								}

								FallingBlock fallingBlock = player.getWorld().spawnFallingBlock(getCenteredLocation(location), blockData);
								fallingBlock.setGravity(false);
								fallingBlock.setHurtEntities(false);
								fallingBlock.setDropItem(false);
								fallingBlock.setVelocity(new Vector());
								matchData.getSolidBlocks().put(player.getUniqueId(), fallingBlock);
								// stop their disguise (as otherwise the hider sees 2 of their block)
								matchData.getDisguises().get(player.getUniqueId()).stopDisguise();
							}
							// add invisibility to hide them/their falling block disguise
							player.addPotionEffect(new PotionEffectBuilder(PotionEffectType.INVISIBILITY).infinite().ambient(true).build());
							PlayerUtils.hidePlayer(player).from(minigamer.getMatch().getOnlinePlayers());
							// run usual ticking
							disguisedBlockTick(minigamer);
						} else
							sendActionBarWithTimer(minigamer, "&cYou cannot fully disguise inside non-air blocks!");
					} else {
						disguisedBlockTick(minigamer);
					}
				}
			}
		});
		match.getTasks().register(taskId);

		// separate task so this doesn't run as often
		int hunterTaskId = match.getTasks().repeat(0, 5, () -> getZombies(match).forEach(minigamer -> {
			Block block = minigamer.getOnlinePlayer().getTargetBlockExact(4, FluidCollisionMode.NEVER);
			if (block == null) return;
			Material type = block.getType();
			if (MaterialTag.ALL_AIR.isTagged(type)) return;
			Component name = Component.translatable(type);

			// this will create some grammatically weird messages ("Oak Planks is a possible hider")
			// idk what to do about that though
			JsonBuilder message = new JsonBuilder();
			if (matchData.getMapMaterials().contains(type))
				message.color(NamedTextColor.GREEN).next(name).next(" is a possible hider");
			else
				message.color(NamedTextColor.RED).next(name).next(" is not a possible hider");
			sendActionBarWithTimer(minigamer, message);
		}));
		match.getTasks().register(hunterTaskId);
	}

	@Override
	public void announceRelease(Match match) {
		match.broadcast(new JsonBuilder("&cThe seekers have been released!"));
	}

	private void disguisedBlockTick(Minigamer minigamer) {
		final Player player = minigamer.getOnlinePlayer();
		final HideAndSeekMatchData matchData = minigamer.getMatch().getMatchData();
		final Material blockChoice = matchData.getBlockChoice(minigamer);
		blockChange(minigamer, matchData.getSolidPlayers().get(minigamer), blockChoice);

		JsonBuilder message = new JsonBuilder("&aYou are currently fully disguised as a ").next(Component.translatable(blockChoice));
		if (matchData.getSolidBlocks().containsKey(player.getUniqueId())) {
			matchData.getSolidBlocks().get(player.getUniqueId()).setTicksLived(1);
			if (!MaterialTag.ALL_AIR.isTagged(player.getInventory().getItemInMainHand().getType()))
				message = new JsonBuilder("&cWarning: Your weapon is visible!");
		}
		sendActionBarWithTimer(minigamer, message);
	}

	protected void blockChange(Minigamer origin, Location location, Material block) {
		origin.getMatch().getMinigamers().forEach(minigamer -> {
			if (!minigamer.equals(origin))
				minigamer.getOnlinePlayer().sendBlockChange(location, block.createBlockData());
		});
	}

	@Override
	public @NotNull Map<String, Integer> getScoreboardLines(@NotNull Match match) {
		if (!match.isStarted() || match.getTimer().getTime() > match.getArena().getSeconds()/2)
			return super.getScoreboardLines(match);
		HideAndSeekMatchData matchData = match.getMatchData();
		Map<String, Integer> lines = new HashMap<>();
		List<Minigamer> humans = getHumans(match);
		lines.put("", 0);
		lines.put("&3&lPlayer Count", 0);
		lines.put("- " + getZombieTeam(match).getVanillaColoredName(), -1 * getZombies(match).size());
		lines.put("- " + getHumanTeam(match).getVanillaColoredName(), -1 * humans.size());

		lines.put("&3&lSurviving Blocks", 99);
		Map<Material, Integer> blockCounts = new HashMap<>();
		humans.forEach(minigamer -> {
			Material blockChoice = matchData.getBlockChoice(minigamer);
			blockCounts.compute(blockChoice, ($, integer) -> integer == null ? 1 : integer+1);
		});
		blockCounts.forEach((material, integer) -> lines.put(camelCase(material), integer));
		return lines;
	}

	public void cleanup(Minigamer minigamer) {
		DisguiseAPI.undisguiseToAll(minigamer.getOnlinePlayer());
	}

	public void cleanup(Match match) {
		match.getMinigamers().forEach(minigamer -> {
			cleanup(minigamer);
			minigamer.getOnlinePlayer().setCooldown(SELECTOR_ITEM.getType(), 0);
			minigamer.getOnlinePlayer().setCooldown(RADAR.getType(), 0);
			minigamer.getOnlinePlayer().setCooldown(STUN_GRENADE.getType(), 0);
		});

		if (match.getMatchData() instanceof HideAndSeekMatchData hideAndSeekMatchData) {
			hideAndSeekMatchData.getSolidBlocks().forEach(($, fallingBlock) -> fallingBlock.remove());
			hideAndSeekMatchData.getFlashBangItems().forEach(Entity::remove);
		}
	}

	@Override
	public void onQuit(@NotNull MatchQuitEvent event) {
		super.onQuit(event);
		cleanup(event.getMinigamer());
	}

	@Override
	public void onEnd(@NotNull MatchEndEvent event) {
		super.onEnd(event);
		cleanup(event.getMatch());
	}

	@Override
	public void onDeath(@NotNull MinigamerDeathEvent event) {
		super.onDeath(event);
		cleanup(event.getMinigamer());
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		// this method is basically checking to see if a hunter has swung at a hider's fake block
		Minigamer minigamer = Minigamer.of(event.getPlayer());

		if (
				minigamer.isPlaying(this) &&
						isZombie(minigamer) &&
						event.getAction() == Action.LEFT_CLICK_BLOCK &&
						event.getHand() != null &&
						event.getHand().equals(EquipmentSlot.HAND)
		) {
			HideAndSeekMatchData matchData = minigamer.getMatch().getMatchData();
			Location blockLocation;
			if (event.getClickedBlock() != null)
				blockLocation = event.getClickedBlock().getLocation();
			else {
				return;
			}

			for (Map.Entry<Minigamer, Location> entry : matchData.getSolidPlayers().entrySet()) {
				Minigamer target = entry.getKey();
				Location location = entry.getValue();
				if (blockLocationsEqual(blockLocation, location)) {
					EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(minigamer.getOnlinePlayer(), target.getPlayer(), EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1);
					e.callEvent();
					if (e.isCancelled()) return;

					minigamer.getOnlinePlayer().attack(target.getPlayer());
					target.setImmobileTicks(0);
					new SoundBuilder(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK).receiver(minigamer.getOnlinePlayer()).category(SoundCategory.PLAYERS).play();
					new SoundBuilder(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK).receiver(target.getPlayer()).category(SoundCategory.PLAYERS).play();
					return;
				}
			}
		}
	}

	@Override
	public boolean canUseBlock(@NotNull Minigamer minigamer, @NotNull Block block) {
		return false;
	}

	@RequiredArgsConstructor
	@Title("&3&lSelect your Block")
	public class HideAndSeekMenu extends InventoryProvider {
		private final Match match;

		@Override
		protected int getRows(Integer page) {
			return MenuUtils.calculateRows(match.getArena().getBlockList().size(), 1);
		}

		@Override
		public void init() {
			addCloseItem();
			HideAndSeekMatchData matchData = match.getMatchData();
			List<Material> materials = matchData.getMapMaterials();
			List<ClickableItem> clickableItems = new ArrayList<>();
			materials.forEach(material -> {
				ItemStack itemStack = new ItemStack(material);
				clickableItems.add(ClickableItem.of(itemStack, e -> {
					matchData.getBlockChoices().put(viewer.getUniqueId(), material);
					viewer.closeInventory();
					if (!match.isStarted())
						PlayerUtils.send(viewer, new JsonBuilder("&3You have selected ").next(Component.translatable(material, NamedTextColor.YELLOW)));
					else
						disguise(Minigamer.of(viewer), true);

				}));
			});
			paginator().items(clickableItems).build();
		}

	}

	public static class StunGrenade {

		private static final long COOLDOWN_TIME = TickTime.SECOND.x(60);
		private static final int RANGE = 8;
		private static final PotionEffect STUN_EFFECT = new PotionEffectBuilder(PotionEffectType.BLINDNESS).particles(true).duration(TickTime.SECOND.x(5)).build();

		public static void run(Minigamer minigamer) {
			if (!COOLDOWN_SERVICE.check(minigamer.getUuid(), "hide-and-seek-stun", COOLDOWN_TIME, false))
				return;

			Match match = minigamer.getMatch();
			HideAndSeekMatchData matchData = match.getMatchData();
			HideAndSeek hideAndSeek = match.getMechanic();
			AtomicInteger iteration = new AtomicInteger(0);
			Item item = minigamer.getLocation().getWorld().spawn(minigamer.getLocation(), Item.class, _item -> {
				_item.setItemStack(new ItemStack(Material.FIREWORK_STAR));
				_item.setCanMobPickup(false);
				_item.setPickupDelay((short) 32767);
			});
			matchData.getFlashBangItems().add(item);
			int taskId = Tasks.repeat(0, 1, () -> {
				if (iteration.get() == 0) {
					item.getLocation().getWorld().playSound(item.getLocation(), Sound.ENTITY_WITCH_THROW, 1, 1);
					new SoundBuilder("minecraft:custom.misc.flashbang")
						.location(item.getLocation())
						.receivers(match.getOnlinePlayers().stream().filter(p -> distance(p, item).lte(16)).toList())
						.muteMenuItem(MuteMenuItem.JOKES)
						.play();
					item.setVelocity(minigamer.getLocation().getDirection().normalize());
				}

				if (iteration.get() < 20)
					item.getLocation().getWorld().spawnParticle(Particle.SMOKE_NORMAL, item.getLocation(), 1, 0, 0, 0, 0.1);

				if (iteration.getAndIncrement() == 20) {
					item.getLocation().getWorld().spawnParticle(Particle.FLASH, item.getLocation(), 2, 0, 0, 0, 0.1);
					item.getLocation().getWorld().playSound(item.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
					Tasks.wait(1, () -> item.getLocation().getWorld().playSound(item.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1, 1));

					for (Minigamer minigamer1 : hideAndSeek.getZombies(match)) {
						if (distance(minigamer1, item).lt(RANGE)) {
							minigamer1.addPotionEffect(STUN_EFFECT);
						}
					}

					item.remove();
					matchData.getFlashBangItems().remove(item);
				}
			});
			match.getTasks().register(taskId);

			COOLDOWN_SERVICE.check(minigamer.getOnlinePlayer(), "hide-and-seek-stun", COOLDOWN_TIME);
			minigamer.getOnlinePlayer().setCooldown(STUN_GRENADE.getType(), (int) COOLDOWN_TIME);
		}

	}

	public static class Radar {

		private static final long COOLDOWN_TIME = TickTime.SECOND.x(20);
		private static final int RANGE = 20;

		public static void run(Minigamer minigamer) {
			if (!COOLDOWN_SERVICE.check(minigamer.getUuid(), "hide-and-seek-radar", COOLDOWN_TIME, false))
				return;

			Match match = minigamer.getMatch();
			HideAndSeek hideAndSeek = match.getMechanic();
			if (hideAndSeek.getHumans(match).stream().anyMatch(_minigamer -> distance(minigamer, _minigamer).lt(RANGE))) {
				minigamer.sendMessage(new JsonBuilder("There is a hider nearby!").color(Color.LIME));
				minigamer.getOnlinePlayer().playSound(minigamer.getOnlinePlayer().getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
			} else {
				minigamer.sendMessage(new JsonBuilder("No hiders in range").color(Color.RED));
				new SoundBuilder("minecraft:custom.noteblock.buzz").volume(2).pitch(0.1).receiver(minigamer.getOnlinePlayer()).play();
			}
			COOLDOWN_SERVICE.check(minigamer.getOnlinePlayer(), "hide-and-seek-radar", COOLDOWN_TIME);
			minigamer.getOnlinePlayer().setCooldown(RADAR.getType(), (int) COOLDOWN_TIME);
		}

	}

}
