package me.pugabyte.nexus.features.shops;

import me.pugabyte.nexus.models.shop.Shop;
import me.pugabyte.nexus.models.shop.ShopService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

import static me.pugabyte.nexus.utils.ItemUtils.getRawShulkerContents;
import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.StringUtils.pretty;

public class ShopUtils {

	public static void giveItems(OfflinePlayer player, ItemStack item) {
		giveItems(player, Collections.singletonList(item));
	}

	public static void giveItems(OfflinePlayer player, List<ItemStack> items) {
		Shop shop = new ShopService().get(player);
		if (player.isOnline()) {
			List<ItemStack> excess = PlayerUtils.giveItemsGetExcess(player.getPlayer(), items);
			shop.addHolding(excess);
			if (!excess.isEmpty())
				if (player.isOnline() && player.getPlayer() != null)
					PlayerUtils.send(player.getPlayer(), new JsonBuilder(Shops.PREFIX + "Excess items added to item collection menu, click to view").command("/shops collect"));
		} else
			shop.addHolding(items);
	}

	public static String prettyMoney(Number number) {
		return prettyMoney(number, true);
	}

	public static String prettyMoney(Number number, boolean free) {
		if (free && number.doubleValue() == 0)
			return "free";
		return "$" + pretty(number);
	}

	public static boolean isSimilar(ItemStack item1, ItemStack item2) {
		if (isNullOrAir(item1) || isNullOrAir(item2))
			return false;

		if (item1.getType() != item2.getType())
			return false;

		if (!MaterialTag.SHULKER_BOXES.isTagged(item1.getType()))
			return item1.isSimilar(item2);

		List<ItemStack> contents1 = getRawShulkerContents(item1);
		List<ItemStack> contents2 = getRawShulkerContents(item2);
		if (contents1.isEmpty() && contents2.isEmpty())
			return true;

		for (int i = 0; i < contents1.size(); i++) {
			if (contents1.get(i) == null && contents2.get(i) == null)
				continue;
			if (contents1.get(i) == null || !contents1.get(i).isSimilar(contents2.get(i)))
				return false;
		}

		return true;
	}

}
