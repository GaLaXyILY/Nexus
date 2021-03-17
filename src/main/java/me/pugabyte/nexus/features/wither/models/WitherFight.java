package me.pugabyte.nexus.features.wither.models;

import com.destroystokyo.paper.ParticleBuilder;
import com.destroystokyo.paper.Title;
import com.sk89q.worldedit.world.block.BlockTypes;
import lombok.Data;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Chat;
import me.pugabyte.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import me.pugabyte.nexus.features.warps.Warps;
import me.pugabyte.nexus.features.wither.BeginningCutscene;
import me.pugabyte.nexus.features.wither.WitherChallenge;
import me.pugabyte.nexus.utils.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;

import static me.pugabyte.nexus.utils.StringUtils.colorize;

@Data
public abstract class WitherFight implements Listener {

	public UUID host;
	public List<UUID> party;
	public List<UUID> alivePlayers;
	public List<UUID> spectators = new ArrayList<>();
	public boolean started = false;
	public Wither wither;
	public boolean shouldRegen = true;
	public List<Location> playerPlacedBlocks = new ArrayList<>();
	public List<Blaze> blazes = new ArrayList<>();
	public boolean gotStar = false;
	public List<Integer> tasks = new ArrayList<>();
	public Map<UUID, Team> scoreboardTeams = new HashMap<>();

	public abstract WitherChallenge.Difficulty getDifficulty();

	public abstract void spawnWither(Location location);

	public abstract boolean shouldGiveStar();

	public abstract List<ItemStack> getAlternateDrops();

	public void start() {
		Nexus.registerListener(this);
		new BeginningCutscene().run().thenAccept(location -> {
			Chat.broadcastIngame(new JsonBuilder(WitherChallenge.PREFIX + "The fight has started! &e&lClick here to spectate")
					.command("/wither spectate").hover("&eYou will be teleported to the wither arena"), MuteMenuItem.EVENTS);
			spawnWither(location);
			new WorldEditUtils("events").set("witherarena-door", BlockTypes.NETHER_BRICKS);
			new WorldEditUtils("events").set("witherarena-lobby", BlockTypes.NETHERRACK);
			tasks.add(Tasks.repeat(Time.SECOND.x(5), Time.SECOND.x(5), () -> {
				if (!shouldRegen) return;
				if (wither.getTarget() == null)
					wither.setTarget(PlayerUtils.getPlayer(RandomUtils.randomElement(alivePlayers)).getPlayer());
				if (wither.hasLineOfSight(wither.getTarget())) return;
				List<Block> blocks = BlockUtils.getBlocksInRadius(wither.getLocation(), 4);
				for (Block block : blocks) {
					if (block.getType() == Material.BEDROCK) continue;
					Location loc = block.getLocation();
					for (Block face : BlockUtils.getAdjacentBlocks(block))
						if (MaterialTag.NEEDS_SUPPORT.isTagged(face.getType()))
							face.setType(Material.AIR);
					new ParticleBuilder(Particle.BLOCK_DUST)
							.location(loc)
							.data(block.getBlockData())
							.count(10)
							.extra(.1)
							.spawn();
					loc.getWorld().playSound(loc, Sound.BLOCK_NETHER_BRICKS_BREAK, 1f, 1f);
					block.setType(Material.AIR);
				}
			}));

			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			for (UUID uuid : alivePlayers) {
				try {
					OfflinePlayer player = PlayerUtils.getPlayer(uuid).getPlayer();
					Nexus.debug("Registering team");
					if (!player.isOnline()) continue;
					Team team = scoreboard.registerNewTeam("wither-" + uuid.toString().split("-")[0]);
					Nexus.debug("Registered new Team: " + team.getName());
					team.addEntry(player.getName());
					player.getPlayer().setScoreboard(scoreboard);
					scoreboardTeams.put(uuid, team);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			tasks.add(Tasks.repeat(0, Time.SECOND.x(5), () -> {
				for (UUID uuid : alivePlayers) {
					OfflinePlayer player = PlayerUtils.getPlayer(uuid).getPlayer();
					if (!player.isOnline()) continue;
					Nexus.debug("Setting team " + scoreboardTeams.get(uuid).getName() + "'s prefix to " + ((int) player.getPlayer().getHealth()) + " &c❤ &r");
					scoreboardTeams.get(uuid).setPrefix(colorize(((int) player.getPlayer().getHealth()) + " &c❤ &r"));
				}
			}));

		});
	}

	public OfflinePlayer getHostOfflinePlayer() {
		return PlayerUtils.getPlayer(host);
	}

	public Player getHostPlayer() {
		return getHostOfflinePlayer().getPlayer();
	}

	public void broadcastToParty(String message) {
		for (UUID player : party)
			PlayerUtils.send(player, WitherChallenge.PREFIX + message);
		for (UUID player : spectators)
			PlayerUtils.send(player, WitherChallenge.PREFIX + message);
	}

	public void teleportPartyToArena() {
		started = true;
		alivePlayers = party;
		for (UUID uuid : alivePlayers) {
			Player player = PlayerUtils.getPlayer(uuid).getPlayer();
			if (player != null)
				player.teleport(new Location(Bukkit.getWorld("events"), -150.50, 69.00, -114.50, .00F, .00F));
		}
	}

	public void giveItems() {
		OfflinePlayer itemReceiver = getHostOfflinePlayer();
		if (getDifficulty() == WitherChallenge.Difficulty.CORRUPTED)
			if (getAlternateDrops() != null)
				PlayerUtils.giveItemsAndDeliverExcess(itemReceiver, getAlternateDrops(), null, WorldGroup.SURVIVAL);
		if (shouldGiveStar()) {
			gotStar = true;
			PlayerUtils.giveItemsAndDeliverExcess(itemReceiver, Collections.singleton(new ItemStack(Material.NETHER_STAR)), null, WorldGroup.SURVIVAL);
			broadcastToParty("&3Congratulations! You have gotten a wither star for this fight!");
		} else {
			broadcastToParty("&cUnfortunately, you did not get a star this time. You can try a harder difficulty for a higher chance");
			if (getAlternateDrops() != null)
				PlayerUtils.giveItemsAndDeliverExcess(itemReceiver, getAlternateDrops(), null, WorldGroup.SURVIVAL);
		}
	}

	public void spawnPiglins(int amount) {
		WorldGuardUtils utils = new WorldGuardUtils("events");
		for (int i = 0; i < amount; i++) {
			Location location;
			do {
				location = utils.getRandomBlock("witherarena-pigmen").getLocation();
			} while (location.getBlock().getType() != Material.AIR);
			PigZombie piglin = location.getWorld().spawn(location, PigZombie.class);
			piglin.setAdult();
			piglin.setCanPickupItems(false);
			piglin.setTarget(PlayerUtils.getPlayer(RandomUtils.randomElement(alivePlayers)).getPlayer());
		}
	}

	public void spawnHoglins(int amount) {
		WorldGuardUtils utils = new WorldGuardUtils("events");
		for (int i = 0; i < amount; i++) {
			Location location;
			do {
				location = utils.getRandomBlock("witherarena-pigmen").getLocation();
			} while (location.getBlock().getType() != Material.AIR);
			Hoglin hoglin = location.getWorld().spawn(location, Hoglin.class);
			hoglin.setAdult();
			hoglin.setCanPickupItems(false);
			hoglin.setImmuneToZombification(true);
			hoglin.setTarget(PlayerUtils.getPlayer(RandomUtils.randomElement(alivePlayers)).getPlayer());
		}
	}

	public void spawnBrutes(int amount) {
		WorldGuardUtils utils = new WorldGuardUtils("events");
		for (int i = 0; i < amount; i++) {
			Location location;
			do {
				location = utils.getRandomBlock("witherarena-pigmen").getLocation();
			} while (location.getBlock().getType() != Material.AIR);
			PiglinBrute brute = location.getWorld().spawn(location, PiglinBrute.class);
			brute.setCanPickupItems(false);
			brute.setImmuneToZombification(true);
			brute.setTarget(PlayerUtils.getPlayer(RandomUtils.randomElement(alivePlayers)).getPlayer());
		}
	}

	public List<Blaze> spawnBlazes(int amount, int radius) {
		List<Location> locations = new ArrayList<>();
		for (int iteration = 0; iteration < amount; iteration++) {
			double angle = 360.0 / amount * iteration;
			angle = Math.toRadians(angle);
			double x = Math.cos(angle) * radius;
			double z = Math.sin(angle) * radius;
			locations.add(wither.getLocation().clone().add(x, 0, z));
		}

		for (Location location : locations) {
			for (int i = 0; i < 5; i++)
				new ParticleBuilder(Particle.LAVA)
						.count(25)
						.location(location)
						.spawn();
		}

		List<Blaze> blazes = new ArrayList<>();
		for (Location location : locations) {
			Item item = location.getWorld().spawn(location, Item.class);
			item.setGravity(false);
			item.setInvulnerable(true);
			item.setItemStack(new ItemStack(Material.STONE_BUTTON));
			item.setCanPlayerPickup(false);
			item.setCanMobPickup(false);
			item.setWillAge(false);
			item.setVelocity(new Vector(0, 0, 0));
			Blaze blaze = location.getWorld().spawn(location, Blaze.class);
			blaze.setGravity(false);
			blazes.add(blaze);
			item.addPassenger(blaze);
		}
		announceBlazeShield();
		return blazes;
	}

	public void announceBlazeShield() {
		broadcastToParty("&cThe wither has spawned a blaze shield. &eKill them to continue the fight!");
	}

	@EventHandler
	public void onEntityTarget(EntityTargetLivingEntityEvent event) {
		if (!isInRegion(event.getEntity().getLocation())) return;
		if (event.getTarget() instanceof Player && alivePlayers.contains(event.getTarget().getUniqueId())) return;
		event.setTarget(PlayerUtils.getPlayer(RandomUtils.randomElement(alivePlayers)).getPlayer());
	}

	@EventHandler
	public void onKillEntity(EntityDeathEvent event) {
		if (!isInRegion(event.getEntity().getLocation())) return;
		if (event.getEntityType() == EntityType.PLAYER) return;
		event.getDrops().clear();
		event.setDroppedExp(0);
		if (event.getEntityType() != EntityType.BLAZE) return;
		Blaze blaze = (Blaze) event.getEntity();
		if (!blazes.contains(blaze)) return;

		if (blaze.getVehicle() != null)
			blaze.getVehicle().remove();

		blazes.remove(blaze);
		if (blazes.size() > 0) return;
		wither.setAI(true);
		wither.setGravity(true);
		wither.setInvulnerable(false);
		shouldRegen = true;
	}

	@EventHandler
	public void onWitherRegen(EntityRegainHealthEvent event) {
		if (event.getEntity() != wither) return;
		if (shouldRegen) return;
		if (event.getEntity() != wither) return;
		event.setCancelled(true);
	}

	public void processPlayerQuit(Player player, String reason) {
		player.setGameMode(GameMode.SURVIVAL);
		Warps.spawn(player);
		if (alivePlayers.size() == 1) {
			tasks.forEach(Tasks::cancel);
			int partySize = party.size();
			Chat.broadcastIngame(WitherChallenge.PREFIX + "&e" + getHostOfflinePlayer().getName() +
					(partySize > 1 ? " and " + (partySize - 1) + " other" + ((partySize - 1 > 1) ? "s" : "") + " &3have" : " &3has") +
					" lost to the Wither in " + getDifficulty().getTitle() + " &3mode", MuteMenuItem.EVENTS);
			Chat.broadcastDiscord("**[Wither]** " + getHostOfflinePlayer().getName() +
					(partySize > 1 ? " and " + (partySize - 1) + " other" + ((partySize - 1 > 1) ? "s" : "") + " have" : " has") +
					" lost to the Wither in " + StringUtils.camelCase(getDifficulty().name()) + " mode");
			WitherChallenge.reset();
		} else {
			WitherChallenge.currentFight.broadcastToParty("&e" + player.getName() + " &chas " + reason + " and is out of the fight!");
			wither.setTarget(PlayerUtils.getPlayer(RandomUtils.randomElement(alivePlayers)).getPlayer());
		}
		alivePlayers.remove(player.getUniqueId());
		player.removePotionEffect(PotionEffectType.WITHER);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onDeath(PlayerDeathEvent event) {
		if (WitherChallenge.currentFight == null) return;
		Player player = event.getEntity();
		if (!alivePlayers.contains(player.getUniqueId())) return;
		scoreboardTeams.get(player.getUniqueId()).unregister();
		scoreboardTeams.remove(player.getUniqueId());
		event.setCancelled(true);
		event.setDeathMessage(null);
		Warps.spawn(player);
		processPlayerQuit(player, "died");
	}

	@EventHandler
	public void onKillWither(EntityDeathEvent event) {
		if (event.getEntityType() != EntityType.WITHER) return;
		Wither wither = (Wither) event.getEntity();
		if (wither != this.wither) return;
		tasks.forEach(Tasks::cancel);
		giveItems();

		int partySize = party.size();
		Chat.broadcastIngame(WitherChallenge.PREFIX + "&e" + getHostOfflinePlayer().getName() +
				(partySize > 1 ? " and " + (partySize - 1) + " other" + ((partySize - 1 > 1) ? "s" : "") + " &3have" : " &3has") +
				" successfully beaten the Wither in " +
				getDifficulty().getTitle() + " &3mode " + (gotStar ? "and got the star" : "but did not get the star"), MuteMenuItem.EVENTS);
		Chat.broadcastDiscord("**[Wither]** " + getHostOfflinePlayer().getName() +
				(partySize > 1 ? " and " + (partySize - 1) + " other" + ((partySize - 1 > 1) ? "s" : "") + " have" : " has") +
				" successfully beaten the Wither in " + StringUtils.camelCase(getDifficulty().name()) + " mode. " + (gotStar ? "and got the star" : "but did not get the star"));

		new WorldGuardUtils("events").getEntitiesInRegion("witherarena").forEach(e -> {
			if (e.getType() != EntityType.PLAYER)
				e.remove();
		});

		Tasks.wait(Time.SECOND.x(10), () -> {
			started = false;
			WitherChallenge.currentFight.getAlivePlayers().forEach(uuid -> {
				OfflinePlayer offlinePlayer = PlayerUtils.getPlayer(uuid);
				if (offlinePlayer.getPlayer() != null)
					Warps.spawn(offlinePlayer.getPlayer());
			});
			WitherChallenge.currentFight.getSpectators().forEach(uuid -> {
				OfflinePlayer offlinePlayer = PlayerUtils.getPlayer(uuid);
				if (offlinePlayer.getPlayer() != null) {
					Warps.spawn(offlinePlayer.getPlayer());
					offlinePlayer.getPlayer().setGameMode(GameMode.SURVIVAL);
				}
			});
			WitherChallenge.reset();
		});
	}

	public boolean isInRegion(Location location) {
		return new WorldGuardUtils(location.getWorld()).isInRegion(location, "witherarena");
	}

	@EventHandler
	public void onPlaceWater(PlayerBucketEmptyEvent event) {
		if (!isInRegion(event.getBlock().getLocation())) return;
		if (event.getBucket() != Material.WATER_BUCKET) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onWitherSkeletonDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof WitherSkeleton)) return;
		if (!(event.getDamager() instanceof Player) && !(event.getDamager() instanceof Arrow)) return;
		if (!isInRegion(event.getEntity().getLocation())) return;
	}

	@EventHandler
	public void onFallingBlockLand(EntityChangeBlockEvent event) {
		if (!(event.getEntity() instanceof FallingBlock)) return;
		if (!isInRegion(event.getEntity().getLocation())) return;
		event.setCancelled(true);
		event.getEntity().remove();
	}

	@EventHandler
	public void onPlaceBlock(BlockPlaceEvent event) {
		if (!isInRegion(event.getBlock().getLocation())) return;
		if (!alivePlayers.contains(event.getPlayer().getUniqueId())) return;
		playerPlacedBlocks.add(event.getBlock().getLocation());
	}

	@EventHandler
	public void onBreakBlock(BlockBreakEvent event) {
		if (!isInRegion(event.getBlock().getLocation())) return;
		if (!alivePlayers.contains(event.getPlayer().getUniqueId())) return;
		if (playerPlacedBlocks.contains(event.getBlock().getLocation())) return;
		if (event.getBlock().getType() == Material.FIRE) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		if (!isInRegion(event.getBlock().getLocation())) return;
		if (playerPlacedBlocks.contains(event.getBlock().getLocation())) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onWitherExplodeBlock(EntityChangeBlockEvent event) {
		if (!isInRegion(event.getBlock().getLocation())) return;
		if (playerPlacedBlocks.contains(event.getBlock().getLocation())) return;
		if (event.getTo() != Material.AIR) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onRemoveMapFromFrame(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof ItemFrame)) return;
		if (!isInRegion(event.getEntity().getLocation())) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onItemFrameBreak(HangingBreakEvent event) {
		if (!(event.getEntity() instanceof ItemFrame)) return;
		if (!isInRegion(event.getEntity().getLocation())) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onItemFrameBreakByEntity(HangingBreakByEntityEvent event) {
		if (!(event.getEntity() instanceof ItemFrame)) return;
		if (!isInRegion(event.getEntity().getLocation())) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onEntityExplodeEvent(EntityExplodeEvent event) {
		if (!isInRegion(event.getEntity().getLocation())) return;
		event.blockList().removeIf(block -> !playerPlacedBlocks.contains(block.getLocation()));
	}

	@EventHandler
	public void onClickEntityWitherBlock(PlayerInteractAtEntityEvent event) {
		if (!(event.getRightClicked() instanceof ItemFrame)) return;
		if (!isInRegion(event.getRightClicked().getLocation())) return;
		event.setCancelled(true);
		if (!event.getPlayer().getInventory().getItemInMainHand().getType().isBlock()) return;
		ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
		event.getRightClicked().getLocation().getBlock().setType(item.getType());
		item.setAmount(item.getAmount() - 1);
		event.getPlayer().updateInventory();
	}

	public enum CounterAttack {
		KNOCKBACK {
			@Override
			public void execute(List<UUID> uuids) {
				Location witherLoc = WitherChallenge.currentFight.wither.getLocation();
				for (UUID uuid : uuids) {
					if (PlayerUtils.getPlayer(uuid).getPlayer() == null) continue;
					Player player = PlayerUtils.getPlayer(uuid).getPlayer();
					Location playerLoc = player.getLocation();
					int x = (int) (playerLoc.getX() - witherLoc.getX());
					int z = (int) (playerLoc.getZ() - witherLoc.getZ());
					player.setVelocity(new Vector(x, .5, z).multiply(1.25));
				}
			}
		},
		BLINDNESS {
			@Override
			public void execute(List<UUID> uuids) {
				for (UUID uuid : uuids)
					if (PlayerUtils.getPlayer(uuid).getPlayer() != null)
						PlayerUtils.getPlayer(uuid).getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Time.SECOND.x(5), 0, true));
			}
		},
		CONFUSION {
			@Override
			public void execute(List<UUID> uuids) {
				for (UUID uuid : uuids) {
					if (PlayerUtils.getPlayer(uuid).getPlayer() != null)
						PlayerUtils.getPlayer(uuid).getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, Time.SECOND.x(10), 0, true));
				}
			}
		},
		TAKE_POTIONS {
			@Override
			public void execute(List<UUID> uuids) {
				for (UUID uuid : uuids) {
					OfflinePlayer player = PlayerUtils.getPlayer(uuid);
					if (player.getPlayer() == null) continue;
					for (PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
						if (effect.getType() == PotionEffectType.WITHER) continue;
						player.getPlayer().removePotionEffect(effect.getType());
						player.getPlayer().sendTitle(new Title("", colorize("&8&kbbb &4&lStipped Potion Effects &8&kbbb"), 10, 40, 10));
					}

				}
			}
		},
		HUNGER {
			@Override
			public void execute(List<UUID> uuids) {
				for (UUID uuid : uuids)
					if (PlayerUtils.getPlayer(uuid).getPlayer() != null)
						PlayerUtils.getPlayer(uuid).getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Time.SECOND.x(10), 1, true));
			}
		},
		LEVITATION {
			@Override
			public void execute(List<UUID> uuids) {
				for (UUID uuid : uuids)
					if (PlayerUtils.getPlayer(uuid).getPlayer() != null)
						PlayerUtils.getPlayer(uuid).getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, Time.SECOND.x(5), 0, true));
			}
		},
		WITHER_EFFECT {
			@Override
			public void execute(List<UUID> uuids) {
				for (UUID uuid : uuids)
					if (PlayerUtils.getPlayer(uuid).getPlayer() != null)
						PlayerUtils.getPlayer(uuid).getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WITHER, Time.SECOND.x(10), 0, true));
			}
		},
		DUPLICATE {
			@Override
			public void execute(List<UUID> uuids) {
				Location witherLoc = WitherChallenge.currentFight.wither.getLocation();
				Wither wither1 = spawnMinion(witherLoc.clone().add(3, 0, 0));
				wither1.setTarget(PlayerUtils.getPlayer(RandomUtils.randomElement(uuids)).getPlayer());

				Wither wither2 = spawnMinion(witherLoc.clone().add(-3, 0, 0));
				wither2.setTarget(PlayerUtils.getPlayer(RandomUtils.randomElement(uuids)).getPlayer());
			}

			public Wither spawnMinion(Location location) {
				Wither wither = location.getWorld().spawn(location, Wither.class);
				wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1);
				wither.setHealth(1);
				wither.setCustomName("Minion");
				wither.setCustomNameVisible(true);
				wither.getBossBar().setVisible(false);
				return wither;
			}
		},
		TNT {
			@Override
			public void execute(List<UUID> uuids) {
				Location witherLoc = WitherChallenge.currentFight.wither.getLocation();
				TNTPrimed tnt = witherLoc.getWorld().spawn(witherLoc, TNTPrimed.class);
				tnt.setFuseTicks(100);
			}
		};

		public abstract void execute(List<UUID> uuids);
	}

}
