package gg.projecteden.nexus.features.survival.decorationstore.models;

import com.mojang.datafixers.util.Pair;
import gg.projecteden.api.common.utils.TimeUtils.TickTime;
import gg.projecteden.nexus.Nexus;
import gg.projecteden.nexus.features.resourcepack.decoration.common.DecorationConfig;
import gg.projecteden.nexus.utils.ActionBarUtils;
import gg.projecteden.nexus.utils.ItemBuilder;
import gg.projecteden.nexus.utils.StringUtils;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BuyableData {
	ItemStack baseItem;

	@Getter
	DecorationConfig decorationConfig;

	public BuyableData(ItemStack itemStack) {
		this.baseItem = itemStack;

		decorationConfig = DecorationConfig.of(baseItem);
	}

	public ItemStack getItem() {
		ItemBuilder baseItemBuilder = new ItemBuilder(baseItem);

		if (isDecoration()) {
			ItemBuilder configItemBuilder = new ItemBuilder(decorationConfig.getItem());
			Color dyeColor = baseItemBuilder.dyeColor();
			if (dyeColor != null)
				configItemBuilder.dyeColor(dyeColor);

			return configItemBuilder.build();
		}

		return baseItemBuilder.name("&f" + getName()).build();
	}


	public @Nullable Pair<String, Integer> getNameAndPrice() {
		Integer price = getPrice(baseItem);
		if (price == null) return null;

		String name = getName();
		if (name == null) return null;

		return new Pair<>(name, price);
	}

	public boolean isHDB() {
		return baseItem.getType().equals(Material.PLAYER_HEAD);
	}

	public boolean isDecoration() {
		return decorationConfig != null;
	}

	public @Nullable String getName() {
		if (isHDB()) {
			String id = Nexus.getHeadAPI().getItemID(baseItem);
			if (id == null)
				return "Player Head";

			ItemStack item = Nexus.getHeadAPI().getItemHead(id);
			return StringUtils.stripColor(item.getItemMeta().getDisplayName());
		} else if (isDecoration())
			return decorationConfig.getName();
		else
			return null;
	}

	public void showPrice(Player player) {
		Pair<String, Integer> namePrice = getNameAndPrice();
		if (namePrice == null)
			return;

		ActionBarUtils.sendActionBar(
			player,
			"&3Buy &e" + namePrice.getFirst() + " &3for &a$" + namePrice.getSecond(),
			TickTime.TICK.x(2),
			false
		);
	}

	//

	public static boolean hasPrice(ItemStack itemStack) {
		return getPrice(itemStack) != null;
	}

	public static @Nullable Integer getPrice(ItemStack itemStack) {
		// HDB Skull
		if (itemStack.getType().equals(Material.PLAYER_HEAD)) {
			return 3; // TODO: HDB SKULL PRICE
		}

		// Decoration
		DecorationConfig config = DecorationConfig.of(itemStack);
		if (config != null) {
			return 5; // TODO: GET DECORATION PRICE
		}

		// Unknown
		return null;
	}
}
