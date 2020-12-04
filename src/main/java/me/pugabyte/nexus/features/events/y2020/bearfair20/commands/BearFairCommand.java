package me.pugabyte.nexus.features.events.y2020.bearfair20.commands;

import com.google.common.base.Strings;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20;
import me.pugabyte.nexus.features.events.y2020.bearfair20.fairgrounds.Interactables;
import me.pugabyte.nexus.features.events.y2020.bearfair20.islands.IslandType;
import me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland;
import me.pugabyte.nexus.features.events.y2020.bearfair20.islands.PugmasIsland;
import me.pugabyte.nexus.features.events.y2020.bearfair20.quests.BFQuests;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.features.warps.commands._WarpCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.Redirects.Redirect;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleteIgnore;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.bearfair.BearFairService;
import me.pugabyte.nexus.models.bearfair.BearFairUser;
import me.pugabyte.nexus.models.bearfair.BearFairUser.BFPointSource;
import me.pugabyte.nexus.models.delivery.Delivery;
import me.pugabyte.nexus.models.delivery.DeliveryService;
import me.pugabyte.nexus.models.warps.Warp;
import me.pugabyte.nexus.models.warps.WarpService;
import me.pugabyte.nexus.models.warps.WarpType;
import me.pugabyte.nexus.utils.BlockUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Time;
import me.pugabyte.nexus.utils.WorldEditUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.events.y2020.bearfair20.BearFair20.WGUtils;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.HalloweenIsland.atticKey;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.HalloweenIsland.basketItem;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland.ancientPickaxe;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland.blessedHoneyBottle;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland.honeyStroopWafel;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland.rareFlower;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland.relic;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland.specialPrize;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MainIsland.stroofWafel;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.MinigameNightIsland.arcadePieces;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.PugmasIsland.presentItem;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.SummerDownUnderIsland.anzacBiscuit;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.SummerDownUnderIsland.goldenSyrup;
import static me.pugabyte.nexus.features.events.y2020.bearfair20.islands.SummerDownUnderIsland.peanuts;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@NoArgsConstructor
@Redirect(from = {"/bfp", "bfpoints", "/bearfairpoints"}, to = "/bearfair points")
public class BearFairCommand extends _WarpCommand implements Listener {
	private final BearFairService service = new BearFairService();

	public BearFairCommand(CommandEvent event) {
		super(event);
	}

	@Override
	public WarpType getWarpType() {
		return WarpType.BEAR_FAIR;
	}

	@Path
	void bearfair() {
		if (!BearFair20.allowWarp)
			error("Warp is disabled");

//		BearFairUser user = service.get(player());
//		if (user.isFirstVisit())
//			error("To unlock the warp, you must first travel to Bear Fair aboard the space yacht at spawn");

		teleport(new WarpService().get("bearfair", WarpType.BEAR_FAIR));
	}

	@Path("gallery")
	void warpToGallery() {
		teleport(new WarpService().get("gallery", WarpType.BEAR_FAIR));
	}

	@Path("store")
	void warpToStore() {
		teleport(new WarpService().get("store", WarpType.BEAR_FAIR));
	}

	@Path("quests giveAllQuestItem")
	void giveQuestItems() {
		// MAIN
		ItemUtils.giveItems(player(), Arrays.asList(honeyStroopWafel, stroofWafel, blessedHoneyBottle, relic, ancientPickaxe, rareFlower, specialPrize));
		// SDU
		ItemUtils.giveItems(player(), Arrays.asList(anzacBiscuit, goldenSyrup, peanuts));
		// PUGMAS
		ItemUtils.giveItem(player(), presentItem);
		// MGN
		arcadePieces.forEach(piece -> ItemUtils.giveItem(player(), piece.build()));
		// HALLOWEEN
		ItemUtils.giveItems(player(), Arrays.asList(atticKey, basketItem));
	}

	@Path("quests npc <text>")
	void questsSwitchQuest(String string) {
		if (!BearFair20.enableQuests) return;

		ProtectedRegion pugmasRegion = WGUtils.getProtectedRegion(IslandType.PUGMAS.get().getRegion());
		ProtectedRegion mainRegion = WGUtils.getProtectedRegion(IslandType.MAIN.get().getRegion());
		if (!WGUtils.getRegionsAt(player().getLocation()).contains(pugmasRegion) && !WGUtils.getRegionsAt(player().getLocation()).contains(mainRegion))
			return;
		switch (string) {
			// Main
			case "accept_witch":
				MainIsland.acceptWitchQuest(player());
				break;
			// Pugmas
			case "switch_mayor":
				PugmasIsland.switchQuest(player(), true);
				break;
			case "switch_grinch":
				PugmasIsland.switchQuest(player(), false);
				break;
			case "accept_mayor":
				PugmasIsland.acceptQuest(player(), true);
				break;
			case "accept_grinch":
				PugmasIsland.acceptQuest(player(), false);
				break;
		}
	}

	@Path("quests stats [player]")
	public void questStats(@Arg("self") Player player) {
		BearFairUser user = service.get(player);

		int treasureChests = user.getEasterEggsLocs().size();
		String questStatus = "&cIncomplete";
		if (user.isQuest_Main_Finish())
			questStatus = "&aComplete";

		String done = "&a✔";
		String notDone = "&c✖";

		send("");
		send("&3" + player.getName() + "'s Quest Stats:");
		send("&3Treasure Chests: &e" + treasureChests + "&3/&e15");
		send("&3Quest Progress: " + questStatus);
		if (questStatus.equalsIgnoreCase("&aComplete")) {
			send("");
			return;
		}

		String status_main = user.getQuest_Main_Step() == 3 ? done : notDone;
		String status_mgn = user.isQuest_MGN_Finish() ? done : notDone;
		String status_pugmas = user.isQuest_Pugmas_Finish() ? done : notDone;
		String status_halloween = user.isQuest_Halloween_Finish() ? done : notDone;
		String status_sdu = user.isQuest_SDU_Finish() ? done : notDone;

		send(status_main + " &7- &eHoney Stroopwafel");
		send(status_mgn + " &7- &eArcade Token");
		send(status_pugmas + " &7- &ePresent");
		send(status_halloween + " &7- &eHalloween Candy Basket");
		send(status_sdu + " &7- &eANZAC Biscuit");
		send("");
	}

	// Point Commands

	@Path("points [player]")
	public void points(@Arg("self") BearFairUser user) {
		if (player().equals(user.getOfflinePlayer()))
			send(PREFIX + "&3Total: &e" + user.getTotalPoints());
		else
			send(PREFIX + "&3" + user.getOfflinePlayer().getName() + "'s Total: &e" + user.getTotalPoints());
	}

	@Path("points daily [player]")
	public void pointsDaily(@Arg("self") BearFairUser user) {
		if (player().equals(user.getOfflinePlayer()))
			send(PREFIX + "&3Daily Points:");
		else
			send(PREFIX + "&3" + user.getOfflinePlayer().getName() + "'s Daily Points:");

		for (BFPointSource pointSource : BFPointSource.values()) {
			Map<LocalDate, Integer> dailyMap = user.getPointsReceivedToday().get(pointSource);
			int points = 0;
			if (dailyMap != null)
				points = dailyMap.getOrDefault(LocalDate.now(), 0);

			int dailyMax = BearFairUser.DAILY_SOURCE_MAX;
			String sourceColor = points == dailyMax ? "&a" : "&3";
			String sourceName = StringUtils.camelCase(pointSource.name());
			send(" " + sourceColor + sourceName + " &7- &e" + points + "&3/&e" + dailyMax);
		}
	}

	@Path("points pay <player> <points>")
	public void pointsPay(BearFairUser toUser, int points) {
		BearFairUser fromUser = service.get(player());
		if (toUser.getOfflinePlayer().equals(fromUser.getOfflinePlayer()))
			error("You cannot pay yourself");

		fromUser.takePoints(points);
		toUser.givePoints(points);

		fromUser.send(PREFIX + "&e" + points + " BFP &3has been sent to &e" + toUser.getOfflinePlayer().getName());
		if (toUser.getOfflinePlayer().isOnline())
			toUser.send(PREFIX + "&e" + points + " BFP &3has been received from &e" + fromUser.getOfflinePlayer().getName());

		service.save(fromUser);
		service.save(toUser);
	}

	@Path("points give <player> <points>")
	@Permission("group.admin")
	public void pointsGive(BearFairUser user, int points) {
		user.givePoints(points);
		service.save(user);
		send(PREFIX + "&e" + points + plural(" point", points) + " &3given to &e" + user.getOfflinePlayer().getName());
	}

	@Path("points take <player> <points>")
	@Permission("group.admin")
	public void pointsTake(BearFairUser user, int points) {
		user.takePoints(points);
		service.save(user);
		send(PREFIX + "&e" + points + plural(" point", points) + " &3taken from &e" + user.getOfflinePlayer().getName());
	}

	@Path("points set <player> <points>")
	@Permission("group.admin")
	public void pointsSet(BearFairUser user, int points) {
		user.setTotalPoints(points);
		service.save(user);
		send(PREFIX + "&3set &e" + user.getOfflinePlayer().getName() + "&3 to &e" + points + plural(" point", points));
	}

	@Path("points reset <player>")
	@Permission("group.admin")
	public void pointsReset(BearFairUser user) {
		user.setTotalPoints(0);
		user.getPointsReceivedToday().clear();
		service.save(user);
	}

	@Path("points top [page]")
	public void pointsTop(@Arg("1") int page) {
		List<BearFairUser> results = service.getTopPoints(page);
		if (results.size() == 0)
			error("&cNo results on page " + page);

		send("");
		send(PREFIX + (page > 1 ? "&3Page " + page : ""));
		int i = (page - 1) * 10 + 1;
		for (BearFairUser user : results)
			send("&3" + i++ + " &e" + user.getOfflinePlayer().getName() + " &7- " + user.getTotalPoints());
	}

	// Admin Commands

	@Path("quests info")
	@Permission("group.admin")
	public void topTreasureChests() {
		List<BearFairUser> all = service.getAll();
		int started = (int) all.stream().filter(BearFairUser::isQuest_Main_Start).count();
		int finished = (int) all.stream().filter(BearFairUser::isQuest_Main_Finish).count();

		send("");
		send(PREFIX + "&3Players Started/Finished: " + started + "\\" + finished);
		send(PREFIX + "&3Found EasterEggs:");
		all.stream()
				.filter(user -> user.getEasterEggsLocs().size() > 0)
				.sorted(Comparator.comparing((BearFairUser user) -> user.getEasterEggsLocs().size()).reversed())
				.forEach(user -> send("&3" + user.getOfflinePlayer().getName() + " &7- &e" + user.getEasterEggsLocs().size()));
		send("");
	}

	@Override
	@Path("warps list [filter]")
	@Permission("group.admin")
	public void list(@Arg(tabCompleter = Warp.class) String filter) {
		super.list(filter);
	}

	@Override
	@Path("warps set <name>")
	@Permission("group.admin")
	public void set(@Arg(tabCompleter = Warp.class) String name) {
		player();
		super.set(name);
	}

	@Path("warps (rm|remove|delete|del) <name>")
	@Permission("group.admin")
	public void delete(Warp warp) {
		super.delete(warp);
	}

	@Path("warps (teleport|tp) <name>")
	@Permission("group.admin")
	public void teleport(Warp warp) {
		player();
		super.teleport(warp);
	}

	@Path("warps <name>")
	@Permission("group.admin")
	public void tp(Warp warp) {
		player();
		super.tp(warp);
	}

	@TabCompleteIgnore(permission = "group.admin")
	@Path("warps tp nearest")
	public void teleportNearest() {
		player();
		getNearestWarp(player().getLocation()).ifPresent(warp -> warp.teleport(player()));
	}

	@Override
	@Path("warps nearest")
	@Permission("group.admin")
	public void nearest() {
		super.nearest();
	}

	@Path("recipes")
	@Permission("group.admin")
	void recipes() {
		runCommandAsConsole("minecraft:recipe take " + player().getName() + " nexus:custom_bearfair_anzac_biscuit");
		runCommandAsConsole("minecraft:recipe give " + player().getName() + " nexus:custom_bearfair_anzac_biscuit");
	}

	@Path("strengthTest")
	@Permission("group.admin")
	void strengthTest() {
		commandBlock();
		Interactables.strengthTest();
	}

	@Async
	@Confirm
	@Path("clearData [player]")
	@Permission("group.admin")
	void clearData(@Arg("self") Player player) {
		BearFairUser user = service.get(player);
		service.delete(user);
	}

	// Command Blocks
	@Path("smite")
	@Permission("group.admin")
	public void smite() {
		if (!BearFair20.enableQuests) return;
		commandBlock();
		BlockCommandSender sender = (BlockCommandSender) event.getSender();
		Location loc = sender.getBlock().getLocation();
		World world = loc.getWorld();
		if (world != null)
			world.strikeLightningEffect(loc);
		MainIsland.witchQuestCraft();
	}

	@Path("yachtHorn")
	@Permission("group.admin")
	public void yachtHorn() {
		commandBlock();
		BlockCommandSender sender = (BlockCommandSender) event.getSender();
		Location loc = sender.getBlock().getLocation();
		World world = loc.getWorld();
		if (world == null) return;
		world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 4F, 0.1F);
		Tasks.wait(Time.SECOND.x(2), () -> world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 4F, 0.1F));
	}

	@Path("moveCollector")
	@Permission("group.admin")
	public void moveCollector() {
		commandBlock();
		BFQuests.moveCollector();
	}

	// Custom Maps

	@EventHandler
	public void onClickSign(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Location loc = player.getLocation();

		if (!loc.getWorld().getName().toLowerCase().contains("bearfair")) return;
		if (BlockUtils.isNullOrAir(event.getClickedBlock())) return;
		if (!MaterialTag.SIGNS.isTagged(event.getClickedBlock().getType())) return;

		Sign sign = (Sign) event.getClickedBlock().getState();
		String prefix = stripColor(sign.getLine(0));
		if (!prefix.equalsIgnoreCase("[Buy Painting]")) return;

		String title = (sign.getLine(2).trim() + " " + sign.getLine(3).trim()).trim();
		if (Strings.isNullOrEmpty(title)) return;

		String price = stripColor(sign.getLine(1));
		int pricePoints = Integer.parseInt(price.replaceAll(" BFP", ""));

		// TODO - BearFairStore: Couldn't get DeliveryService to work
		boolean disableDelivery = true;
		if (disableDelivery) {
			if (player.hasPermission("group.admin")) {
				BearFairStoreMap bearFairStoreMap = convertToBearFairStoreMap(title);
				ItemUtils.giveItem(player, bearFairStoreMap.getSplatterMap());
			} else {
				send(player, PREFIX + "Couldn't get this feature to work ): include the title of this painting in your discord order");
			}
			return;
		}
		//

		BearFairUser user = service.get(player);
		AtomicInteger userPoints = new AtomicInteger(user.getTotalPoints());

		MenuUtils.ConfirmationMenu.builder()
				.onConfirm(e -> Tasks.async(() -> {
					if (userPoints.get() >= pricePoints) {
						userPoints.addAndGet(-pricePoints);
						user.setTotalPoints(userPoints.get());
						service.save(user);

						BearFairStoreMap bearFairStoreMap = convertToBearFairStoreMap(title);
						Delivery delivery = new DeliveryService().get(player);
						delivery.setupDelivery(bearFairStoreMap.getSplatterMap());
						send(player, PREFIX + "&3You bought &e" + title + " &3for &e" + price
								+ ", &3You now have &e" + userPoints.get() + " BFP");
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 2);
					} else {
						send(player, PREFIX + "&cYou do not have enough points for this");
						player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
					}
				}))
				.title("&4&lAre you sure?")
				.confirmText("&aBuy")
				.confirmLore("&3Painting: &e" + title
						+ "||&3Price: &e" + price
						+ "||&f ||"
						+ "&3You have &e" + userPoints.get() + " BFP")
				.cancelText("&cCancel")
				.open(player);
	}

	@Path("store maps reload")
	@Permission("group.admin")
	public void storeMapsReload() {
		reloadMaps();
		send(PREFIX + "Loaded " + maps.size() + " maps");
	}

	@Path("store maps get <map...>")
	@Permission("group.admin")
	void storeGetMap(BearFairStoreMap map) {
		ItemUtils.giveItem(player(), map.getSplatterMap());
	}

	@ConverterFor(BearFairStoreMap.class)
	BearFairStoreMap convertToBearFairStoreMap(String value) {
		return maps.get(value);
	}

	@TabCompleterFor(BearFairStoreMap.class)
	List<String> tabCompleteBearFairStoreMap(String filter) {
		return maps.keySet().stream()
				.filter(id -> id.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	@Data
	@AllArgsConstructor
	private static class BearFairStoreMap {
		private String id;
		private ItemStack splatterMap;
	}

	private static final Map<String, BearFairStoreMap> maps = new HashMap<>();

	static {
		reloadMaps();
	}

	private static void reloadMaps() {
		try {
			maps.clear();
			WorldEditUtils WEUtils = new WorldEditUtils(Bukkit.getWorld("bearfair"));

			for (Block block : WEUtils.getBlocks(WEUtils.getWorldGuardUtils().getRegion("maps"))) {
				try {
					if (!MaterialTag.SIGNS.isTagged(block.getType())) continue;

					Sign sign = (Sign) block.getState();
					String id = (sign.getLine(0).trim() + " " + sign.getLine(1).trim()).trim();
					if (Strings.isNullOrEmpty(id)) continue;

					Block chest = block.getRelative(BlockFace.DOWN);
					if (!(chest.getState() instanceof Chest)) continue;

					Chest inv = (Chest) chest.getState();
					ItemStack map = inv.getBlockInventory().getContents()[0].clone();

					if (map.getItemMeta().hasLore()) {
						String[] split = map.getItemMeta().getDisplayName().split("-");
						String name = String.join("-", Arrays.copyOfRange(split, 1, split.length));
						ItemBuilder.setName(map, "&6" + id + " &8-" + name);
						ItemBuilder.removeLoreLine(map, 1);
					}
					maps.put(id, new BearFairStoreMap(id, map));
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

}
