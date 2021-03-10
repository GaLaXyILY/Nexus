package me.pugabyte.nexus.features.shops.providers;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.shops.Shops;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.models.shop.Shop;
import me.pugabyte.nexus.models.shop.ShopService;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.pugabyte.nexus.utils.StringUtils.colorize;

public class YourShopProvider extends _ShopProvider {

	public YourShopProvider(_ShopProvider previousMenu) {
		this.previousMenu = previousMenu;
	}

	@Override
	public void open(Player viewer, int page) {
		open(viewer, page, this, "&0Your shop");
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		super.init(player, contents);

		Shop shop = new ShopService().get(player);

		contents.set(0, 1, ClickableItem.from(nameItem(Material.ENDER_EYE, "&6Preview your shop"), e -> new PlayerShopProvider(this, shop).open(player)));
		contents.set(0, 2, ClickableItem.from(new ItemBuilder(Material.OAK_SIGN).name("&6Set shop description").lore("").lore(shop.getDescription()).build(), e ->
				Nexus.getSignMenuFactory().lines(shop.getDescriptionArray()).prefix(Shops.PREFIX).colorize(false).response(lines -> {
					shop.setDescription(Arrays.asList(lines));
					service.save(shop);
					open(player);
				}).open(player)));

		contents.set(0, 4, ClickableItem.from(nameItem(Material.LIME_CONCRETE_POWDER, "&6Add item"), e -> new ExchangeConfigProvider(this).open(player)));

		// TODO
		contents.set(0, 6, ClickableItem.from(nameItem(Material.WRITABLE_BOOK, "&6Shop history"), e -> {}));
		contents.set(0, 7, ClickableItem.from(nameItem(Material.CYAN_SHULKER_BOX, "&6Collect items"), e -> new CollectItemsProvider(this).open(player)));

		if (shop.getProducts() == null || shop.getProducts().size() == 0) return;
		List<ClickableItem> items = new ArrayList<>();

		shop.getProducts(shopGroup).forEach(product -> {
			ItemStack item = product.getItemWithOwnLore();
			items.add(ClickableItem.from(item, e -> new EditProductProvider(this, product).open(player)));
		});

		addPagination(player, contents, items);
	}

	public static class CollectItemsProvider extends _ShopProvider implements Listener {
		private final static String TITLE = colorize("&0Collect Items");
		private Player player;
		private final _ShopProvider previousMenu;

		public CollectItemsProvider(_ShopProvider previousMenu) {
			this.previousMenu = previousMenu;
		}

		public void open(Player player, int page) {
			this.player = player;

			final int size = 54;
			Inventory inv = Bukkit.createInventory(null, size, TITLE);


			ShopService service = new ShopService();
			Shop shop = service.get(player);

			if (shop.getHolding().isEmpty())
				throw new InvalidInputException("No items available for collection");

			List<ItemStack> items = new ArrayList<>(shop.getHolding().subList(0, Math.min(size, shop.getHolding().size())));
			shop.getHolding().removeAll(items);
			service.save(shop);

			inv.setContents(items.toArray(new ItemStack[0]));
			Nexus.registerTempListener(this);
			player.openInventory(inv);
		}

		@EventHandler
		public void onChestClose(InventoryCloseEvent event) {
			if (event.getInventory().getHolder() != null) return;
			if (!Utils.equalsInvViewTitle(event.getView(), TITLE)) return;
			if (!event.getPlayer().equals(player)) return;

			ShopService service = new ShopService();
			Shop shop = service.get(player);

			for (ItemStack content : event.getInventory().getContents())
				if (!ItemUtils.isNullOrAir(content))
					shop.addHolding(content);

			service.save(shop);

			Nexus.unregisterTempListener(this);
			event.getPlayer().closeInventory();
			Tasks.wait(1, () -> previousMenu.open(player));
		}
	}

}
