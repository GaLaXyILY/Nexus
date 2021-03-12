package me.pugabyte.nexus.utils;

import com.google.common.base.Strings;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import lombok.Getter;
import lombok.SneakyThrows;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.delivery.DeliveryCommand;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotFoundException;
import me.pugabyte.nexus.models.delivery.DeliveryService;
import me.pugabyte.nexus.models.delivery.DeliveryUser;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.nerd.NerdService;
import me.pugabyte.nexus.utils.Utils.MinMaxResult;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.StringUtils.colorize;
import static me.pugabyte.nexus.utils.Utils.getMin;

public class PlayerUtils {

	public static void puga(String message) {
		send(puga(), message);
	}

	public static void wakka(String message) {
		send(wakka(), message);
	}

	public static void blast(String message) {
		send(blast(), message);
	}

	public static void zani(String message) {
		send(zani(), message);
	}

	public static void vroom(String message){
		send(vroom(), message);
	}

	public static void lexi(String message){
		send(lexi(), message);
	}

	public static Player puga() {
		return Bukkit.getPlayer("Pugabyte");
	}

	public static Player wakka() {
		return Bukkit.getPlayer("WakkaFlocka");
	}

	public static Player blast() {
		return Bukkit.getPlayer("Blast");
	}

	public static Player zani() {
		return Bukkit.getPlayer("Zanitaeni");
	}

	public static Player vroom() {
		return Bukkit.getPlayer("Camaros");
	}

	public static Player lexi() {
		return Bukkit.getPlayer("lexikiq");
	}

	public static boolean isPuga(Player player) {
		return isPlayer(puga(), player);
	}

	public static boolean isWakka(Player player) {
		return isPlayer(wakka(), player);
	}

	public static boolean isBlast(Player player) {
		return isPlayer(blast(), player);
	}

	private static boolean isPlayer(Player player1, Player player2) {
		if (player1 == null || player2 == null)
			return false;
		return player1.equals(player2);
	}

	public static boolean isVanished(Player player) {
		for (MetadataValue meta : player.getMetadata("vanished"))
			return (meta.asBoolean());
		return false;
	}

	public static boolean isStaffGroup(Player player) {
		return player.hasPermission("group.staff");
	}

	public static boolean isBuilderGroup(Player player) {
		return player.hasPermission("group.builder");
	}

	public static boolean isModeratorGroup(Player player) {
		return player.hasPermission("group.moderator");
	}

	public static boolean isSeniorStaffGroup(Player player) {
		return player.hasPermission("group.seniorstaff");
	}

	public static boolean isAdminGroup(Player player) {
		return player.hasPermission("group.admin");
	}

	public static boolean isSelf(OfflinePlayer player1, OfflinePlayer player2) {
		return player1.getUniqueId().equals(player2.getUniqueId());
	}

	public static boolean canSee(OfflinePlayer viewer, OfflinePlayer target) {
		if (!viewer.isOnline() || !target.isOnline()) return false;
		return (canSee(viewer.getPlayer(), target.getPlayer()));
	}

	public static boolean canSee(Player viewer, Player target) {
		return !isVanished(target) || viewer.hasPermission("pv.see");
	}

	public static List<String> getOnlineUuids() {
		return Bukkit.getOnlinePlayers().stream()
				.map(p -> p.getUniqueId().toString())
				.collect(Collectors.toList());
	}

	public static OfflinePlayer getPlayer(UUID uuid) {
		return Bukkit.getOfflinePlayer(uuid);
	}

	public static OfflinePlayer getPlayer(String partialName) {
		if (partialName == null || partialName.length() == 0)
			throw new InvalidInputException("No player name given");

		String original = partialName;
		partialName = partialName.toLowerCase().trim();

		if (partialName.length() == 36)
			return getPlayer(UUID.fromString(partialName));

		for (Player player : Bukkit.getOnlinePlayers())
			if (player.getName().toLowerCase().startsWith(partialName))
				return player;
		for (Player player : Bukkit.getOnlinePlayers())
			if (player.getName().toLowerCase().contains((partialName)))
				return player;

		NerdService nerdService = new NerdService();

		Nerd fromNickname = nerdService.getFromAlias(partialName);
		if (fromNickname != null)
			return fromNickname.getOfflinePlayer();

		List<Nerd> matches = nerdService.find(partialName);
		if (matches.size() > 0) {
			Nerd nerd = matches.get(0);
			if (nerd != null && nerd.getUuid() != null)
				return nerd.getOfflinePlayer();
		}

		throw new PlayerNotFoundException(original);
	}

	public static MinMaxResult<Player> getNearestPlayer(Player original) {
		return getMin((Collection<Player>) Bukkit.getOnlinePlayers(), player -> {
			if (!player.getWorld().equals(original.getWorld()) || isSelf(original, player)) return null;
			return player.getLocation().distance(original.getLocation());
		});
	}

	@SneakyThrows
	public static int getPing(Player player) {
		Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
		return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
	}

	public static void deposit(Player player, double amount) {
		Nexus.getEcon().depositPlayer(player, amount);
	}

	public static void withdraw(Player player, double amount) {
		Nexus.getEcon().withdrawPlayer(player, amount);
	}

	public static void runCommand(CommandSender sender, String commandNoSlash) {
//		if (sender instanceof Player)
//			Utils.callEvent(new PlayerCommandPreprocessEvent((Player) sender, "/" + command));
		Bukkit.dispatchCommand(sender, commandNoSlash);
	}

	public static void runCommandAsOp(CommandSender sender, String commandNoSlash) {
		boolean deop = !sender.isOp();
		sender.setOp(true);
		runCommand(sender, commandNoSlash);
		if (deop)
			sender.setOp(false);
	}

	public static void runCommandAsConsole(String commandNoSlash) {
		runCommand(Bukkit.getConsoleSender(), commandNoSlash);
	}

	public static void send(String uuid, String message) {
		send(getPlayer(uuid), message);
	}

	public static void send(UUID uuid, String message) {
		OfflinePlayer offlinePlayer = getPlayer(uuid);
		send(offlinePlayer, message);
	}

	public static void send(OfflinePlayer offlinePlayer, String message) {
		if (offlinePlayer.getPlayer() != null)
			send(offlinePlayer.getPlayer(), message);
	}

	public static void send(Player player, String message) {
		if (player != null && player.isOnline())
			player.sendMessage(colorize(message));
	}

	public static void send(CommandSender sender, String message) {
		if (sender instanceof Player)
			send((Player) sender, message);
		else if (sender instanceof OfflinePlayer) {
			OfflinePlayer offlinePlayer = (OfflinePlayer) sender;
			if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null)
				send(offlinePlayer.getPlayer(), message);
		} else
			sender.sendMessage(colorize(message));
	}

	public static void send(Player player, JsonBuilder builder) {
		if (player.isOnline())
			player.sendMessage(builder.build());
	}

	public static void send(CommandSender sender, JsonBuilder builder) {
		sender.sendMessage(builder.build());
	}

	public static void send(Player player, BaseComponent... baseComponents) {
		if (player.isOnline())
			player.sendMessage(baseComponents);
	}

	public static void send(CommandSender sender, BaseComponent... baseComponents) {
		sender.sendMessage(baseComponents);
	}

	public static boolean hasRoomFor(Player player, ItemStack... items) {
		List<ItemStack> itemList = new ArrayList<>();
		for (ItemStack item : new ArrayList<>(Arrays.asList(items))) {
			if (!isNullOrAir(item))
				itemList.add(item);
		}

		return hasRoomFor(player, itemList.size());
	}

	public static boolean hasRoomFor(Player player, int slots) {
		ItemStack[] contents = player.getInventory().getContents();
		int slotsUsed = 0;
		for (ItemStack content : contents) {
			if (!isNullOrAir(content))
				slotsUsed++;
		}

		return (slotsUsed <= (36 - slots));
	}

	public static boolean playerHas(Player player, ItemStack itemStack) {
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(itemStack))
			return true;
		if (Arrays.asList(inventory.getStorageContents()).contains(itemStack))
			return true;
		if (Arrays.asList(inventory.getArmorContents()).contains(itemStack))
			return true;
		return Arrays.asList(inventory.getExtraContents()).contains(itemStack);
	}

	@Deprecated
	@ReplaceWith("Chat.broadcast(message, StaticChannel.STAFF)")
	public static void sendStaff(String message) {
		throw new UnsupportedOperationException();
	}

	public static long setPlayerTime(Player player, String time) {
		long ticks;
		try {
			ticks = DescParseTickFormat.parse(time);
		} catch (Exception ex) {
			throw new InvalidInputException("Unable to process time " + time);
		}
		boolean move = !time.startsWith("@");
		long dayTime = player.getPlayerTime();
		dayTime -= dayTime % 24000;
		dayTime += 24000 + ticks;
		if (move)
			dayTime -= player.getWorld().getTime();
		player.setPlayerTime(dayTime, move);
		return ticks;
	}

	public static HidePlayer hidePlayer(Player player) {
		return new HidePlayer(player);
	}

	public static HidePlayer hidePlayer(Minigamer minigamer) {
		return new HidePlayer(minigamer.getPlayer());
	}

	public static ShowPlayer showPlayer(Player player) {
		return new ShowPlayer(player);
	}

	public static ShowPlayer showPlayer(Minigamer minigamer) {
		return new ShowPlayer(minigamer.getPlayer());
	}

	public static class HidePlayer {
		private Player player;

		public HidePlayer(Player player) {
			this.player = player;
		}

		public void from(Minigamer minigamer) {
			from(minigamer.getPlayer());
		}

		public void from(Player player) {
			player.hidePlayer(Nexus.getInstance(), this.player);
		}
	}

	public static class ShowPlayer {
		private Player player;

		public ShowPlayer(Player player) {
			this.player = player;
		}

		public void to(Minigamer minigamer) {
			to(minigamer.getPlayer());
		}

		public void to(Player player) {
			player.showPlayer(Nexus.getInstance(), this.player);
		}
	}

	@Getter
	private static Map<String, Advancement> advancements = new LinkedHashMap<>();

	static {
		Map<String, Advancement> advancements = new LinkedHashMap<>();
		Iterator<Advancement> it = Bukkit.getServer().advancementIterator();
		while (it.hasNext()) {
			Advancement advancement = it.next();
			advancements.put(advancement.getKey().getKey().toLowerCase(), advancement);
		}

		PlayerUtils.advancements = Utils.sortByKey(advancements);
	}

	public static Advancement getAdvancement(String name) {
		name = name.toLowerCase();
		if (advancements.containsKey(name))
			return advancements.get(name);
		throw new InvalidInputException("Advancement &e" + name + " &cnot found");
	}

	public static void giveItem(Player player, Material material) {
		giveItem(player, material, 1);
	}

	public static void giveItem(Player player, Material material, String nbt) {
		giveItem(player, material, 1, nbt);
	}

	public static void giveItem(Player player, Material material, int amount) {
		giveItem(player, material, amount, null);
	}

	public static void giveItem(Player player, Material material, int amount, String nbt) {
		if (material == Material.AIR)
			throw new InvalidInputException("Cannot spawn air");

		if (amount > 64) {
			for (int i = 0; i < (amount / 64); i++)
				giveItem(player, new ItemStack(material, 64), nbt);
			giveItem(player, new ItemStack(material, amount % 64), nbt);
		} else {
			giveItem(player, new ItemStack(material, amount), nbt);
		}
	}

	public static void giveItem(Player player, ItemStack item) {
		giveItems(player, Collections.singletonList(item));
	}

	public static void giveItem(Player player, ItemStack item, String nbt) {
		giveItems(player, Collections.singletonList(item), nbt);
	}

	public static void giveItems(Player player, Collection<ItemStack> items) {
		giveItems(player, items, null);
	}

	public static void giveItems(Player player, Collection<ItemStack> items, String nbt) {
		List<ItemStack> finalItems = new ArrayList<>(items);
		finalItems.removeIf(ItemUtils::isNullOrAir);
		if (!Strings.isNullOrEmpty(nbt)) {
			finalItems.clear();
			NBTContainer nbtContainer = new NBTContainer(nbt);
			for (ItemStack item : new ArrayList<>(items)) {
				NBTItem nbtItem = new NBTItem(item);
				nbtItem.mergeCompound(nbtContainer);
				finalItems.add(nbtItem.getItem());
			}
		}

		dropExcessItems(player, giveItemsGetExcess(player, finalItems));
	}

	public static List<ItemStack> giveItemsGetExcess(Player player, ItemStack items) {
		return giveItemsGetExcess(player, Collections.singletonList(items));
	}

	public static List<ItemStack> giveItemsGetExcess(Player player, List<ItemStack> items) {
		List<ItemStack> excess = new ArrayList<>();
		for (ItemStack item : items)
			if (!isNullOrAir(item))
				excess.addAll(player.getInventory().addItem(item).values());

		return excess;
	}

	public static void giveItemAndDeliverExcess(OfflinePlayer player, ItemStack items, WorldGroup worldGroup) {
		giveItemsAndDeliverExcess(player, Collections.singleton(items), null, worldGroup);
	}

	public static void giveItemAndDeliverExcess(OfflinePlayer player, ItemStack items, String message, WorldGroup worldGroup) {
		giveItemsAndDeliverExcess(player, Collections.singleton(items), message, worldGroup);
	}

	public static void giveItemsAndDeliverExcess(OfflinePlayer player, Collection<ItemStack> items, String message, WorldGroup worldGroup) {
		List<ItemStack> finalItems = new ArrayList<>(items);
		finalItems.removeIf(ItemUtils::isNullOrAir);
		List<ItemStack> excess;
		if (player.isOnline() && player.getPlayer() != null && WorldGroup.get(player.getPlayer()) == worldGroup)
			excess = giveItemsGetExcess(player.getPlayer(), finalItems);
		else
			excess = new ArrayList<>(items);
		if (Utils.isNullOrEmpty(excess)) return;
		DeliveryService service = new DeliveryService();
		DeliveryUser user = service.get(player);
		DeliveryUser.Delivery delivery = DeliveryUser.Delivery.serverDelivery(excess);
		if (!Strings.isNullOrEmpty(message))
			delivery.setMessage(message);
		user.add(worldGroup, delivery);
		service.save(user);
		user.send(user.json(DeliveryCommand.PREFIX + "Your inventory was full. Excess items were given to you as a &c/delivery").command("/delivery").hover("&eClick to view deliveries"));
	}

	public static void dropExcessItems(Player player, List<ItemStack> excess) {
		if (!excess.isEmpty())
			for (ItemStack itemStack : excess)
				if (!isNullOrAir(itemStack))
					player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
	}

}
